package org.byrde.slick.migrations

import java.util.UUID
import org.byrde.slick.{SlickDb, SlickHasPrivilege, SlickProfile}
import org.byrde.slick.SlickRole.Master
import org.byrde.slick.conf.SlickMigrationEngineConfig

import slick.jdbc.meta.MTable

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class SlickMigrationEngine(
  migrations: Seq[SlickNamedMigration]
)(
  implicit val ec: ExecutionContext,
  migrationEngineConfig: SlickMigrationEngineConfig = new SlickMigrationEngineConfig(1.second, 1.second)
) {
  self: SlickDb[Master] with SlickProfile[Master] =>

  import profile.api._

  private case class MigrationRow(migrationName: String, applicationId: UUID, status: String, dateApplied: Long)

  private class MigrationTable(tag: Tag) extends Table[MigrationRow](tag, "migrations") {
    def migrationName = column[String]("migration_name", O.PrimaryKey)
    def applicationId = column[UUID]("id", O.SqlType("UUID"))
    def status = column[String]("status")
    def dateApplied = column[Long]("date_applied")

    def * = (
      migrationName,
      applicationId,
      status,
      dateApplied
    ) <> ((MigrationRow.apply _).tupled, MigrationRow.unapply)
  }

  private val MigrationTable = TableQuery[MigrationTable]

  def migrate(implicit ev: Master SlickHasPrivilege profile.api.Effect.All): Future[Unit] =
    migrations
      .foldLeft(createTable(0, 10)) {
        (prev, next) =>
          prev.flatMap(_ => runMigration(next))
      }
      .andThen {
        case _ =>
          shutdown
      }
      .recover {
        case ex =>
          shutdown
          throw ex
      }

  private def createTable(
    retry: Int,
    limit: Int
  )(implicit ev: Master SlickHasPrivilege profile.api.Effect.All): Future[Unit] =
    run {
      MTable
        .getTables
        .flatMap { tables =>
          tables.find(_.name.name == MigrationTable.baseTableRow.tableName) match {
            case Some(_) =>
              DBIO.successful(())

            case None =>
              MigrationTable.schema.create
          }
        }
    }
    .recoverWith {
      case _ if retry < limit =>
        delay[Unit](migrationEngineConfig.createMigrationTableRetryDelay.toMillis, createTable(retry + 1, limit))

      case ex =>
        throw ex
    }

  private def delay[T](millis: Long, next: => Future[T]): Future[T] =
    Future(Thread.sleep(millis)).flatMap(_ => next)

  private def runMigration(
    migration: SlickNamedMigration
  )(implicit ev: Master SlickHasPrivilege profile.api.Effect.All): Future[Unit] =
    runMigration(UUID.randomUUID(), migration)

  private def runMigration(
    id: UUID,
    migration: SlickNamedMigration
  )(implicit ev: Master SlickHasPrivilege profile.api.Effect.All): Future[Unit] =
    for {
      _ <- claimMigration(migration, id).recover { case _ => () }
      check <- checkClaimedOrCompleted(id, migration)
      _ <- check match {
        case Claimed =>
          run(migration.migration())

        case AlreadyCompleted =>
          Future.unit
      }
      _ <- check match {
        case Claimed =>
          markAsCompleted(migration, id)

        case AlreadyCompleted =>
          Future.unit
      }
    } yield ()

  private def claimMigration(
    migration: SlickNamedMigration,
    id: UUID
  )(implicit ev: Master SlickHasPrivilege profile.api.Effect.All): Future[Unit] =
    run(MigrationTable += MigrationRow(migration.name, id, "Requested", System.currentTimeMillis())).map(_ => ())

  private def checkClaimedOrCompleted(
    id: UUID,
    migration: SlickNamedMigration
  )(implicit ev: Master SlickHasPrivilege profile.api.Effect.All): Future[ClaimStatus] =
    checkClaimedOrCompleted(id, migration.name, migration)

  private def checkClaimedOrCompleted(
    id: UUID,
    name: String,
    migration: SlickNamedMigration
  )(implicit ev: Master SlickHasPrivilege profile.api.Effect.All): Future[ClaimStatus] =
    run(MigrationTable.filter(_.migrationName === name).result.headOption).flatMap {
      case Some(MigrationRow(innerName, `id`, "Requested", _)) if innerName == name =>
        Future.successful(Claimed)

      case Some(MigrationRow(innerName, _, "Completed", _)) if innerName == name =>
        Future.successful(AlreadyCompleted)

      case Some(MigrationRow(innerName, _, "Requested", _)) if innerName == name =>
        delay(
          migrationEngineConfig.checkClusteredMigrationCompleteDelay.toMillis,
          checkClaimedOrCompleted(id, migration)
        )

      case x =>
        Future.failed(new IllegalStateException(s"By now someone should have claimed the migration: $x"))
    }

  private def markAsCompleted(
    migration: SlickNamedMigration,
    id: UUID
  )(implicit ev: Master SlickHasPrivilege profile.api.Effect.All): Future[Unit] =
    run {
      MigrationTable
        .filter(r => r.migrationName === migration.name && r.applicationId === id)
        .update(MigrationRow(migration.name, id, "Completed", System.currentTimeMillis()))
    }.map(_ => ())

  sealed trait ClaimStatus
  case object Claimed extends ClaimStatus
  case object AlreadyCompleted extends ClaimStatus
  
}

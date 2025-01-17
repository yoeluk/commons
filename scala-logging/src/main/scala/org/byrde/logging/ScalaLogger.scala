package org.byrde.logging

import net.logstash.logback.marker.Markers._
import scala.jdk.CollectionConverters._

class ScalaLogger(name: String) extends Logger {
  private val logger = com.typesafe.scalalogging.Logger(name)

  override def logDebug(msg: String): Unit =
    logger.debug(msg)
  
  override def logDebug(msg: String, extras: Log): Unit =
    logger.debug(msg, appendEntries(extras.asMap.asJava))

  override def logInfo(msg: String): Unit =
    logger.info(msg)
  
  override def logInfo(msg: String, extras: Log): Unit =
    logger.info(msg, appendEntries(extras.asMap.asJava))

  override def logWarning(msg: String): Unit =
    logger.warn(msg)
  
  override def logWarning(msg: String, extras: Log): Unit =
    logger.warn(msg, appendEntries(extras.asMap.asJava))
  
  override def logWarning(msg: String, cause: Throwable): Unit =
    logger.warn(msg, cause)

  override def logError(msg: String): Unit =
    logger.error(msg)
  
  override def logError(msg: String, extras: Log): Unit =
    logger.error(msg, appendEntries(extras.asMap.asJava))

  override def logError(msg: String, cause: Throwable): Unit =
    logger.error(msg, cause)
}
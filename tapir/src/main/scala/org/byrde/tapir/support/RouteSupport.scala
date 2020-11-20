package org.byrde.tapir.support

import akka.http.scaladsl.server.Route

import org.byrde.tapir._

import io.circe.{Decoder, Encoder}

import sttp.capabilities.WebSockets
import sttp.capabilities.akka.AkkaStreams
import sttp.tapir.{Endpoint, Schema, Validator}

import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions

trait RouteSupport extends RequestSupport with ResponseSupport with RequestIdSupport {
  def SuccessCode: Int
  
  def ErrorCode: Int
  
  implicit def tapirRoute2TapirRoutes(route: TapirRoute): TapirRoutes =
    TapirRoutes(route)
  
  implicit class ChainTapirRoute(route: TapirRoute) {
    def ~ (_route: TapirRoute): TapirRoutes =
      TapirRoutes(Seq(route, _route))
  }
  
  implicit class ChainTapirRoutes(routes: TapirRoutes) {
    def ~ (route: TapirRoute): TapirRoutes =
      TapirRoutes(routes.value :+ route)
  }
  
  implicit class RichEndpoint[I, T <: TapirResponse](endpoint: Endpoint[I, TapirErrorResponse, T, AkkaStreams with WebSockets]) {
    def toRoute(logic: I => Future[Either[TapirErrorResponse, T]]): Route =
      sttp.tapir.server.akkahttp.RichAkkaHttpEndpoint(endpoint).toRoute(logic)
    
    def toTapirRoute(logic: I => Future[Either[TapirErrorResponse, T]]): TapirRoute =
      TapirRoute(endpoint, endpoint.toRoute(logic))
  }
  
  implicit class RichResponse[T, TT](future: Future[Either[TT, T]]) {
    def toOut[A <: TapirResponse](
      success: (T, Int) => A,
      error: (TT, Int) => TapirErrorResponse =
        (_, code) => TapirResponse.Default(code)
    )(
      implicit encoder: Encoder[T],
      decoder: Decoder[T],
      schema: Schema[T],
      validator: Validator[T],
      ec: ExecutionContext
    ): Future[Either[TapirErrorResponse, A]] =
      future.map {
        case Right(succ) =>
          Right(success(succ, SuccessCode))

        case Left(err) =>
          Left(error(err, ErrorCode))
      }
  }
}

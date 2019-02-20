package org.byrde.clients.ahc.impl

import org.byrde.service.response.DefaultServiceResponse.Message
import org.byrde.service.response.ServiceResponse.TransientServiceResponse
import org.byrde.service.response.ServiceResponseType
import org.byrde.service.response.utils.ServiceResponseUtils._
import org.byrde.uri.Path

import com.github.ghik.silencer.silent
import play.api.libs.ws.StandaloneWSRequest

import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder, Json}

import scala.concurrent.Future

abstract class ServiceResponseAhcExecutor extends JsonAhcExecutor {
  self =>
  import ServiceResponseAhcExecutor._

  @silent def get[T](path: Path, requestHook: StandaloneWSRequest => StandaloneWSRequest = identity, curlRequestHook: CurlRequest => Unit = _ => ())(implicit decoder: Decoder[T]): Future[TransientServiceResponse[T]] =
    super.getJson(path, requestHook, curlRequestHook).map(processResponse[T](_)(deriveDecoder[TransientServiceResponse[T]]))(ec)

  @silent def post[T, TT](body: T)(path: Path, requestHook: StandaloneWSRequest => StandaloneWSRequest = identity, curlRequestHook: CurlRequest => Unit = _ => ())(implicit encoder: Encoder[T], decoder: Decoder[TT]): Future[TransientServiceResponse[TT]] =
    super.postJson(body)(path, requestHook, curlRequestHook).map(processResponse[TT](_)(deriveDecoder[TransientServiceResponse[TT]]))(ec)

  @silent def put[T, TT](body: T)(path: Path, requestHook: StandaloneWSRequest => StandaloneWSRequest = identity, curlRequestHook: CurlRequest => Unit = _ => ())(implicit encoder: Encoder[T], decoder: Decoder[TT]): Future[TransientServiceResponse[TT]] =
    super.putJson(body)(path, requestHook, curlRequestHook).map(processResponse[TT](_)(deriveDecoder[TransientServiceResponse[TT]]))(ec)

  @silent def delete[T](path: Path, requestHook: StandaloneWSRequest => StandaloneWSRequest = identity, curlRequestHook: CurlRequest => Unit = _ => ())(implicit decoder: Decoder[T]): Future[TransientServiceResponse[T]] =
    super.deleteJson(path, requestHook, curlRequestHook).map(processResponse[T](_)(deriveDecoder[TransientServiceResponse[T]]))(ec)

  @silent def patch[T, TT](body: T)(path: Path, requestHook: StandaloneWSRequest => StandaloneWSRequest = identity, curlRequestHook: CurlRequest => Unit = _ => ())(implicit encoder: Encoder[T], decoder: Decoder[TT]): Future[TransientServiceResponse[TT]] =
    super.patchJson(body)(path, requestHook, curlRequestHook).map(processResponse[TT](_)(deriveDecoder[TransientServiceResponse[TT]]))(ec)

  private def processResponse[T](json: Json)(implicit decoder: Decoder[TransientServiceResponse[T]]): TransientServiceResponse[T] =
    json
      .errorHook(deriveDecoder[TransientServiceResponse[Message]])
      .as[TransientServiceResponse[T]] match {
        case Right(validated: TransientServiceResponse[T]) =>
          validated

        case Left(error) =>
          throw error
      }
}

object ServiceResponseAhcExecutor {
  implicit class JsValue2ServiceResponseError(value: Json) {
    @inline def errorHook(implicit decoder: Decoder[TransientServiceResponse[Message]]): Json =
      value
        .as[TransientServiceResponse[Message]] match {
          case Right(validated) if validated.`type` == ServiceResponseType.Error =>
            throw validated.toException

          case _ =>
            value
        }
  }
}

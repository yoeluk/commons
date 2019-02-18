package org.byrde.service.response

import org.byrde.service.response.ServiceResponse.TransientServiceResponseInOut

import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.{Encoder, Json}

trait ServiceResponse[T] {
  implicit def encoder: Encoder[T]

  def `type`: ServiceResponseType

  def msg: String

  def status: Int

  def code: Int

  def response: T

  def toJson: Json =
    TransientServiceResponseInOut(`type`, msg, status, code, response).asJson
}

object ServiceResponse {
  val `type`: String =
    "type"

  val message: String =
    "message"

  val status: String =
    "status"

  val code: String =
    "code"

  val response: String =
    "response"


  case class TransientServiceResponseInOut[T](`type`: ServiceResponseType,
                                              msg: String,
                                              status: Int,
                                              code: Int,
                                              response: T)


  case class TransientServiceResponse[T](override val `type`: ServiceResponseType,
                                         override val msg: String,
                                         override val status: Int,
                                         override val code: Int,
                                         override val response: T)(implicit val encoder: Encoder[T]) extends ServiceResponse[T]

  def apply[T](response: T)(implicit encoder: Encoder[T]): TransientServiceResponse[T] =
    apply("Success", response)(encoder)

  def apply[T](msg: String, response: T)(implicit encoder: Encoder[T]): TransientServiceResponse[T] =
    apply(msg, 200, response)

  def apply[T](msg: String, code: Int, response: T)(implicit encoder: Encoder[T]): TransientServiceResponse[T] =
    apply(ServiceResponseType.Success, msg, code, response)(encoder)

  def apply[T](`type`: ServiceResponseType, msg: String, code: Int, response: T)(implicit encoder: Encoder[T]): TransientServiceResponse[T] =
    apply(`type`, msg, 200, code, response)(encoder)

  def apply[T](`type`: ServiceResponseType, msg: String, status: Int, code: Int, response: T)(implicit encoder: Encoder[T]): TransientServiceResponse[T] =
    TransientServiceResponse[T](`type`, msg, status, code, response)(encoder)
}

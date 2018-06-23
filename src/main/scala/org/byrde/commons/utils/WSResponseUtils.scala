package org.byrde.commons.utils

import play.api.http.HttpEntity
import play.api.libs.ws.WSResponse
import play.api.mvc.{ResponseHeader, Result}

object WSResponseUtils {
  implicit class WSResponse2Result(value: WSResponse) {
    @inline def toResult: Result = {
      val headers =
        value
          .headers
          .map { header =>
            header._1 -> header._2.head
          }

      Result(
        ResponseHeader(value.status, headers),
        HttpEntity.Strict(value.bodyAsBytes, Some(value.contentType))
      )
    }
  }
}

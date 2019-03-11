package org.byrde.play.utils

import play.api.http.HttpEntity
import play.api.libs.ws.StandaloneWSResponse
import play.api.mvc.{ResponseHeader, Result}

object WSResponseUtils {
  implicit class WSResponse2Result(value: StandaloneWSResponse) {
    @inline def toResult(dropHeaders: Boolean = false): Result = {
      val headers =
        if (dropHeaders)
          Map.empty[String, String]
        else
          value
            .headers
            .map { header =>
              header._1 -> header._2.head
            }

      Result(
        ResponseHeader(value.status, headers),
        HttpEntity.Strict(value.bodyAsBytes, Some(value.contentType)))
    }
  }
}
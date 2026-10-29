package org.matrix.chromext.utils

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.core.graphics.drawable.toDrawable
import androidx.core.net.toFile
import androidx.core.net.toUri
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import org.matrix.chromext.Chrome
import java.nio.charset.Charset
import kotlin.io.encoding.Base64

sealed class LoadExternalResourceResult {
  class HttpResponseResult(val httpResponse: HttpResponse): LoadExternalResourceResult()
  class ByteArrayResult(val byteArray: ByteArray, val charset: Charset? = null) :
    LoadExternalResourceResult()

  class StringResult(val string: String) : LoadExternalResourceResult()
}

suspend fun loadExternalResource(url: String): LoadExternalResourceResult {
  val uri = url.toUri()
  return when (uri.scheme) {
    "http", "https" -> {
      LoadExternalResourceResult.HttpResponseResult(
        Chrome.httpClient.get(uri.toString())
      )
    }

    "content" -> {
      Chrome.getContext().contentResolver.openInputStream(uri)
        ?.use { LoadExternalResourceResult.ByteArrayResult(it.readBytes()) }
        ?: throw RuntimeException("contentResolver open $uri return null")
    }

    "file" -> {
      LoadExternalResourceResult.ByteArrayResult(uri.toFile().readBytes())
    }
    /**
     * RFC 2397: The "data" URL scheme
     *
     * dataurl    := "data:" [ mediatype ] [ ";base64" ] "," data
     * mediatype  := [ type "/" subtype ] *( ";" parameter )
     * data       := *urlchar
     * parameter  := attribute "=" value
     *
     */
    "data" -> {
      val line = url.removePrefix("data:")
      val comma = line.indexOf(",")
      if (comma == -1) throw RuntimeException("loadExternalResource unsupported data url for $uri")
      val p1 = line.substring(0, comma)
      val p2 = line.substring(comma + 1)
      if (p1.split(";").last().equals("base64", ignoreCase = true)) {
        LoadExternalResourceResult.ByteArrayResult(
          Base64.withPadding(Base64.PaddingOption.PRESENT_OPTIONAL)
            .decode(Uri.decode(p2))
        )
      } else {
        LoadExternalResourceResult.StringResult(Uri.decode(p2))
      }
    }

    else -> {
      throw RuntimeException("loadExternalResource unsupported scheme ${uri.scheme} for $uri")
    }
  }
}

suspend fun LoadExternalResourceResult.asBytes() = when (this) {
  is LoadExternalResourceResult.HttpResponseResult -> httpResponse.bodyAsBytes()
  is LoadExternalResourceResult.ByteArrayResult -> byteArray
  is LoadExternalResourceResult.StringResult -> string.encodeToByteArray()
}

suspend fun LoadExternalResourceResult.asText(fallbackCharset: Charset = Charsets.UTF_8) = when (this) {
  is LoadExternalResourceResult.HttpResponseResult -> httpResponse.bodyAsText(fallbackCharset)
  is LoadExternalResourceResult.ByteArrayResult -> String(byteArray, charset ?: fallbackCharset)
  is LoadExternalResourceResult.StringResult -> string
}

suspend fun LoadExternalResourceResult.asBitmap(): Bitmap {
  val data = this.asBytes()
  val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
    ?: throw RuntimeException("bitmap decode failed")
  return bitmap
}

suspend inline fun LoadExternalResourceResult.asDrawable(resources: Resources): Drawable =
  this.asBitmap().toDrawable(resources)

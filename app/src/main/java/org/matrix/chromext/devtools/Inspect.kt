package org.matrix.chromext.devtools

import android.net.LocalSocket
import org.json.JSONArray
import org.matrix.chromext.utils.Log

fun getInspectPages(): JSONArray? {
  var response = ""
  runCatching {
        hitDevTools().inputStream.bufferedReader().use {
          while (true) {
            val line = it.readLine()
            if (line.length == 0) {
              val bufferSize =
                  response
                      .split("\n")
                      .find { it.startsWith("Content-Length") }!!
                      .substring(15)
                      .toInt()
              val buffer = CharArray(bufferSize)
              it.read(buffer)
              response = buffer.joinToString("")
              it.close()
              break
            }
            response += line + "\n"
          }
        }
      }
      .onFailure {
        Log.ex(it)
        return null
      }
  return JSONArray(response)
}

fun hitDevTools(): LocalSocket {
  val client = LocalSocket()
  connectDevTools(client)
  client.outputStream.write("GET /json HTTP/1.1\r\n\r\n".toByteArray())
  return client
}

object DevSessions {
  private val clients = mutableSetOf<DevToolClient>()

  fun get(condition: (DevToolClient) -> Boolean): DevToolClient? {
    var cached = clients.find { condition(it) }
    if (cached?.isClosed() == true) {
      clients.remove(cached)
      cached = null
    }
    return cached
  }

  fun new(
      pageId: String,
      tag: String?,
      condition: (DevToolClient) -> Boolean = { true }
  ): DevToolClient {
    var client =
        get { it.pageId == pageId && it.tag == tag && condition(it) } ?: DevToolClient(pageId, tag)
    if (client.isClosed()) {
      hitDevTools().close()
      client = DevToolClient(pageId)
    }
    return client
  }

  fun add(client: DevToolClient?) {
    if (client == null) return
    val cached = clients.find { it.pageId == client.pageId }
    if (cached != null) clients.remove(cached)
    clients.add(client)
  }
}

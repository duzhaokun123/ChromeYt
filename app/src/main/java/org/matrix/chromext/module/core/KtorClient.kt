package org.matrix.chromext.module.core

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.cache.storage.FileStorage
import kotlinx.io.files.Path
import org.matrix.chromext.MainHook
import org.matrix.chromext.module.base.BaseModule

object KtorClient : BaseModule("http_client") {
  lateinit var client: HttpClient
    private set

  override fun load() {
    client = HttpClient {
      expectSuccess = true
      install(HttpTimeout) {
        requestTimeoutMillis = 10_000
      }
      install(HttpCache) {
        publicStorage(FileStorage(Path(MainHook.application.cacheDir.resolve("ChromeXt").path)))
      }
    }
  }
}
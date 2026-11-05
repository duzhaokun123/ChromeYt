package org.matrix.chromext.module.core

import org.luckypray.dexkit.DexKitCacheBridge
import org.luckypray.dexkit.annotations.DexKitExperimentalApi
import org.matrix.chromext.BuildConfig
import org.matrix.chromext.Chrome.packageName
import org.matrix.chromext.Chrome.version
import org.matrix.chromext.JsonCache
import org.matrix.chromext.MainHook
import org.matrix.chromext.module.base.BaseModule

@OptIn(DexKitExperimentalApi::class)
object DexKit : BaseModule("dexkit") {
  val dexKitBridge: DexKitCacheBridge.RecyclableBridge
    get() = DexKitCacheBridge.create(
      "$packageName:$version:${BuildConfig.BUILD_TIME}", MainHook.application.classLoader
    )

  override fun load() {
    System.loadLibrary("dexkit")
    DexKitCacheBridge.apply {
      idleTimeoutMillis = 5_000
      cachePolicy = DexKitCacheBridge.CachePolicy(
        cacheSuccess = true,
        failurePolicy = DexKitCacheBridge.CacheFailurePolicy.ALL
      )
    }
    DexKitCacheBridge.addListener(object : DexKitCacheBridge.CacheBridgeListener() {
      override fun onBridgeCreated(appTag: String) {
        logger.d("created bridge: $appTag")
      }

      override fun onBridgeReleased(appTag: String) {
        logger.d("released bridge $appTag")
      }

      override fun onBridgeDestroyed(appTag: String) {
        logger.d("destroyed bridge $appTag")
      }
    })
    DexKitCacheBridge.init(JsonCache(MainHook.application.cacheDir.resolve("dexkit.json")))
  }
}
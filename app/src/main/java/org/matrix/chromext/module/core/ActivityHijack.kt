package org.matrix.chromext.module.core

import android.app.Activity
import android.app.Application
import android.content.ComponentName
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import io.github.qauxv.lifecycle.Parasitics
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.matrix.chromext.Chrome
import org.matrix.chromext.Resource
import org.matrix.chromext.module.base.BaseModule
import org.matrix.chromext.ui.InspectActivity
import org.matrix.chromext.ui.ScriptConfigActivity
import org.matrix.chromext.ui.ScriptInstallActivity
import org.matrix.chromext.ui.ScriptManagerActivity
import org.matrix.chromext.ui.SettingsActivity
import org.matrix.chromext.utils.Toast
import org.matrix.chromext.utils.reflect.getStaticFieldValueAs
import org.matrix.chromext.MainHook as AppMainHook
import org.matrix.chromext.R as AppR

object ActivityHijack : BaseModule("activity_hijack") {
  private val moduleActivity = listOf(
    ScriptManagerActivity::class.java,
    ScriptConfigActivity::class.java,
    ScriptInstallActivity::class.java,
    SettingsActivity::class.java,
    InspectActivity::class.java,
  ).map { it.name }

  override fun load() {
    AppMainHook.application.registerActivityLifecycleCallbacks(object :
      Application.ActivityLifecycleCallbacks {
      override fun onActivityCreated(
        activity: Activity, savedInstanceState: Bundle?
      ) {
//        Parasitics.injectModuleResources(activity.resources)
        Resource.enrich(activity)
      }

      override fun onActivityDestroyed(activity: Activity) {}

      override fun onActivityPaused(activity: Activity) {}

      override fun onActivityResumed(activity: Activity) {}

      override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

      override fun onActivityStarted(activity: Activity) {}

      override fun onActivityStopped(activity: Activity) {}
    })
    Parasitics.initForStubActivity(AppMainHook.application)
    val success = Parasitics::class.java.getStaticFieldValueAs<Boolean>("__stub_hooked")
    logger.d(success)
    if (!success) {
      Toast.show("Parasitics not load!")
      throw RuntimeException("Parasitics init failed")
    }
  }

  object ParasiticsStub {
    object R {
      object string {
        @JvmField
        val res_inject_success = AppR.string.res_inject_success
      }
    }

    object StartupInfo {
      @JvmStatic
      fun getModulePath(): String? = AppMainHook.modulePath
    }

    object MainHook

    object SyncUtils {
      @OptIn(DelicateCoroutinesApi::class)
      @JvmStatic
      fun runOnUiThread(r: Runnable) {
        GlobalScope.launch(Dispatchers.Main) {
          r.run()
        }
      }
    }

    object Log {
      @JvmStatic
      fun e(message: Any) {
        logger.e(message)
      }

      @JvmStatic
      fun i(message: Any) {
        logger.i(message)
      }
    }

    object Initiator {
      @JvmStatic
      fun getPluginClassLoader(): ClassLoader? {
        return this::class.java.classLoader
      }

      @JvmStatic
      fun getHostClassLoader(): ClassLoader {
        return AppMainHook.hostClassloader!!
      }
    }

    object HostInfo {
      @JvmStatic
      fun getApplication(): Application {
        return AppMainHook.application as Application
      }

      @JvmStatic
      fun getPackageName(): String {
        return AppMainHook.application.packageName
      }
    }

    object ActProxyMgr {
      const val STUB_DEFAULT_ACTIVITY = "org.chromium.chrome.browser.settings.SettingsActivity"
      const val STUB_TRANSLUCENT_ACTIVITY = STUB_DEFAULT_ACTIVITY
      const val STUB_TOOL_ACTIVITY = STUB_DEFAULT_ACTIVITY

      const val ACTIVITY_PROXY_INTENT = "chromext.ACTIVITY_PROXY_INTENT"

      @JvmStatic
      fun isModuleProxyActivity(name: String): Boolean {
        return name in moduleActivity
      }

      @JvmStatic
      fun isModuleBundleClassLoaderRequired(name: String): Boolean {
        return isModuleProxyActivity(name)
      }
    }

    interface WindowIsTranslucent

    object CounterfeitActivityInfoFactory {
      @JvmStatic
      fun makeProxyActivityInfo(className: String, flags: Long): ActivityInfo? {
        val activityInfo = Chrome.getContext().packageManager.getActivityInfo(
          ComponentName(Chrome.getContext(), ActProxyMgr.STUB_DEFAULT_ACTIVITY), flags.toInt()
        )
        activityInfo.apply {
          targetActivity = null
          taskAffinity = null
          descriptionRes = 0
          name = className
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            splitName = null
          }
        }
        return activityInfo
      }
    }
  }
}

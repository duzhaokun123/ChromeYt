package org.matrix.chromext

import android.app.Application
import android.app.Instrumentation
import android.content.Context
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.callbacks.XC_LoadPackage
import org.lsposed.hiddenapibypass.HiddenApiBypass
import org.matrix.chromext.hook.BaseHook
import org.matrix.chromext.hook.ContextMenuHook
import org.matrix.chromext.hook.PageMenuHook
import org.matrix.chromext.hook.PreferenceHook
import org.matrix.chromext.hook.UserScriptHook
import org.matrix.chromext.logger.AndroidLogger
import org.matrix.chromext.module.ScriptNotification
import org.matrix.chromext.module.core.ActivityHijack
import org.matrix.chromext.module.core.DexKit
import org.matrix.chromext.module.core.KtorClient
import org.matrix.chromext.utils.Log
import org.matrix.chromext.utils.hookAfter
import org.matrix.chromext.utils.hookBefore

val supportedPackages =
    arrayOf(
        "com.android.chrome",
      )

class MainHook : IXposedHookLoadPackage, IXposedHookZygoteInit {
  companion object {
    var modulePath: String? = null
    var hostClassloader: ClassLoader? = null
    lateinit var application: Application
      private set
  }

  private val modules = listOf(
    KtorClient,
    DexKit,
    ActivityHijack,
    ScriptNotification,
  )

  override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
    Log.d(lpparam.processName + " started")
    if (lpparam.packageName == BuildConfig.APPLICATION_ID) return
    if (supportedPackages.contains(lpparam.packageName)) {
      val packageName = lpparam.packageName
      val processName = lpparam.processName.removePrefix(packageName)
      AndroidLogger.d("load $packageName precess $processName")
      Instrumentation::class.java.getDeclaredMethod("callApplicationOnCreate", Application::class.java)
        .hookBefore {
          application = it.args[0] as Application
          HiddenApiBypass.setHiddenApiExemptions("")
          modules.filter { module ->
            processName in module.process
          }.forEach { module ->
            runCatching {
              module.load()
            }.onFailure { e ->
              AndroidLogger.e("load ${module.id} failed", e)
            }.onSuccess {
              AndroidLogger.v("load ${module.id} success")
            }
          }
        }

      hostClassloader = lpparam.classLoader
      lpparam.classLoader
          .loadClass("org.chromium.ui.base.WindowAndroid")
          .declaredConstructors[1]
          .hookAfter {
            Chrome.init(it.args[0] as Context, lpparam.packageName)
            initHooks(UserScriptHook)
            if (ContextMenuHook.isInit) return@hookAfter
            runCatching {
                  initHooks(PreferenceHook)
                  initHooks(PageMenuHook)
                }
                .onFailure {
                  initHooks(ContextMenuHook)
                  if (BuildConfig.DEBUG) Log.ex(it)
                }
          }
    } else {
      Log.e("Unsupported package: ${lpparam.packageName}, not Google Chrome stable")
    }
  }

  override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
    modulePath = startupParam.modulePath
    Resource.init(startupParam.modulePath)
  }

  private fun initHooks(vararg hook: BaseHook) {
    hook.forEach {
      if (it.isInit) return@forEach
      it.init()
      if (it.isInit) Log.d("${it.javaClass.simpleName} hooked")
    }
  }
}

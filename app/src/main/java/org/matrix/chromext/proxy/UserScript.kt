package org.matrix.chromext.proxy

import android.net.Uri
import android.os.BaseBundle
import org.luckypray.dexkit.annotations.DexKitExperimentalApi
import org.matrix.chromext.Chrome
import org.matrix.chromext.utils.Log
import org.matrix.chromext.utils.findField
import org.matrix.chromext.utils.findMethod
import org.matrix.chromext.utils.findMethodOrNull
import org.matrix.chromext.utils.reflect.invokeMethod
import org.matrix.chromext.utils.reflect.invokeMethodAs
import org.matrix.chromext.utils.reflect.toClass
import org.matrix.chromext.utils.reflect.toMethod

@OptIn(DexKitExperimentalApi::class)
object UserScriptProxy {
  // It is possible to do a HTTP POST with LoadUrlParams Class
  // grep org/chromium/content_public/common/ResourceRequestBody to get setPostData in
  // org/chromium/content_public/browser/LoadUrlParams

  val gURL = Chrome.load("org.chromium.url.GURL")
  val loadUrlParams =
      Chrome.load("org.chromium.content_public.browser.LoadUrlParams")
  // val tabModelJniBridge = Chrome.load("org.chromium.chrome.browser.tabmodel.TabModelJniBridge")
  val tabWebContentsDelegateAndroidImpl =
      Chrome.load("org.chromium.chrome.browser.tab.TabWebContentsDelegateAndroidImpl")
//  val navigationControllerImpl =
//      Chrome.load("org.chromium.content.browser.framehost.NavigationControllerImpl")
  val chromeTabbedActivity =
      Chrome.load("org.chromium.chrome.browser.ChromeTabbedActivity")
  val tabImpl =
      Chrome.load("org.chromium.chrome.browser.tab.TabImpl")
  val mTab = findField(tabWebContentsDelegateAndroidImpl) { type == tabImpl }

  /**
   * mIsLoading is used in method stopLoading, before calling
   * Lorg/chromium/content_public/browser/WebContents;->stop()V
   */
  val mIsLoading = Chrome.dexKitBridge.getFieldDirect("TabImpl.mIsLoading") { dexKitBridge ->
    val stopLoading = dexKitBridge.findMethod {
      matcher {
        declaredClass(tabImpl)
        addInvoke("Lorg/chromium/content_public/browser/WebContents;->stop()V")
      }
    }.single()
    dexKitBridge.findField {
      matcher {
        declaredClass(tabImpl)
        type(Boolean::class.javaPrimitiveType!!)
        addReadMethod(stopLoading.descriptor)
      }
    }.single()
  }.getFieldInstance(Chrome.getContext().classLoader)
    .also { it.isAccessible = true }

  val getUrl = findMethodOrNull(tabImpl) { returnType == gURL }
  val loadUrl =
      findMethod(tabImpl) {
        parameterTypes contentDeepEquals arrayOf(loadUrlParams) && (returnType != Void.TYPE)
      }

  val kMaxURLChars = 2097152


  val class_TabWindowManager = Chrome.dexKitBridge.getClass {
    matcher {
      usingStrings("Android.MultiWindowMode.AssertIndicesMatch")
    }
  }.toClass()

  val method_TabWindowManagerSingleton_getInstance = Chrome.dexKitBridge.getMethod {
    matcher {
      paramCount(0)
      returnType(class_TabWindowManager)
      addCaller { // Lorg/chromium/chrome/browser/ChromeTabbedActivity;->saveToBaseBundle(Landroid/os/BaseBundle;)V
        declaredClass(chromeTabbedActivity)
        paramTypes(BaseBundle::class.java)
        returnType(Void.TYPE)
      }
    }
  }.toMethod()

  val method_TabWindowManager_getTabById = Chrome.dexKitBridge.getMethod {
    matcher {
      declaredClass(class_TabWindowManager)
      paramTypes(Int::class.javaPrimitiveType)
      returnType("org.chromium.chrome.browser.tab.Tab")
    }
  }.toMethod()

  val method_TabImpl_getId =
    tabImpl.getDeclaredMethod("getId")

  private fun loadUrl(url: String, tab: Any? = Chrome.getTab()) {
    if (!Chrome.checkTab(tab)) return
    loadUrl.invoke(tab, newLoadUrlParams(url))
  }

  fun getTabId(tab: Any): Int {
    return tab.invokeMethodAs(method_TabImpl_getId)
  }

  fun getTabById(id: Int): Any? {
    return method_TabWindowManagerSingleton_getInstance.invoke(null)!!.invokeMethod(
      method_TabWindowManager_getTabById, id
    )
  }

  fun newLoadUrlParams(url: String): Any {
    val constructor =
        loadUrlParams.declaredConstructors.find { it.parameterTypes.contains(String::class.java) }!!
    val types = constructor.parameterTypes
    if (types contentDeepEquals arrayOf(Int::class.java, String::class.java)) {
      return constructor.newInstance(0, url)
    } else if (types contentDeepEquals arrayOf(String::class.java, Int::class.java)) {
      return constructor.newInstance(url, 0)
    } else {
      return constructor.newInstance(url)
    }
  }

  fun evaluateJavascript(script: String, tab: Any? = Chrome.getTab()): Boolean {
    if (script == "") return true
    if (script.length > kMaxURLChars - 20000) return false
    val code = Uri.encode(script)
    if (code.length < kMaxURLChars - 200) {
      loadUrl("javascript:${code}", tab ?: Chrome.getTab())
      return true
    } else {
      return false
    }
  }

  fun getTab(delegate: Any): Any? {
    return mTab.get(delegate)
  }

  fun parseUrl(packed: Any?): String? {
    if (packed == null) {
      return null
    } else if (packed::class.java == String::class.java) {
      return packed as String
    } else if (packed::class.java == loadUrlParams) {
      val mUrl = loadUrlParams.getDeclaredField("a")
      return mUrl.get(packed) as String
    } else if (packed::class.java == gURL) {
      val mSpec = gURL.getDeclaredField("a")
      return mSpec.get(packed) as String
    }
    Log.e("parseUrl: ${packed::class.java} is not ${loadUrlParams.name} nor ${gURL.name}")
    return null
  }
}

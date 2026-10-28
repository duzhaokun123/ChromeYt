package org.matrix.chromext.hook

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Insets
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import kotlin.math.roundToInt
import org.matrix.chromext.Chrome
import org.matrix.chromext.R
import org.matrix.chromext.proxy.PreferenceProxy
import org.matrix.chromext.ui.ScriptManagerActivity
import org.matrix.chromext.ui.SettingsActivity
import org.matrix.chromext.utils.*
import org.matrix.chromext.utils.reflect.findMethod

object PreferenceHook : BaseHook() {
  override fun init() {
    val proxy = PreferenceProxy

    proxy.class_MainSettings
      .findMethod(findSuper = false) { it.parameterTypes contentDeepEquals arrayOf(Bundle::class.java) && it.returnType == Void.TYPE }
      .hookAfter { // onCreate
        val thiz = it.thisObject
        proxy.method_PreferenceFragmentCompat_addPreferencesFromResource.invoke(thiz, R.xml.chromeyt_settings)
        val scriptManagerPreference =
          proxy.method_PreferenceFragmentCompat_findPreference.invoke(thiz, "ChromeYt_ScriptManager")
        proxy.field_Preference_mClickListener.set(scriptManagerPreference, object : View.OnClickListener {
          override fun onClick(v: View) {
            val context = v.context
            context.startActivity(Intent(context, ScriptManagerActivity::class.java))
          }
        })
        val settingsPreference =
          proxy.method_PreferenceFragmentCompat_findPreference.invoke(thiz, "ChromeYt_Settings")
        proxy.field_Preference_mClickListener.set(settingsPreference, object : View.OnClickListener {
          override fun onClick(v: View) {
            val context = v.context
            context.startActivity(Intent(context, SettingsActivity::class.java))
          }
        })
      }

    findMethod(proxy.chromeTabbedActivity) { name == "onNewIntent" || name == "onMAMNewIntent" }
        .hookBefore {
          val intent = it.args[0] as Intent
          if (intent.hasExtra("ChromeXt")) {
            intent.setAction(Intent.ACTION_VIEW)
            var url = intent.getStringExtra("ChromeXt") as String
            intent.setData(Uri.parse(url))
          }
        }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      findMethodOrNull(WindowInsets::class.java) { name == "getSystemGestureInsets" }
          ?.hookBefore {
            val ctx = Chrome.getContext()
            val sharedPref = ctx.getSharedPreferences("ChromeXt", Context.MODE_PRIVATE)
            if (sharedPref.getBoolean("gesture_mod", false)) {
              it.result = Insets.of(0, 0, 0, 0)
              toggleGestureConflict(true)
            } else {
              toggleGestureConflict(false)
            }
          }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      findMethod(WindowInsets::class.java) {
            name == "getInsets" &&
                parameterTypes.size == 1 &&
                parameterTypes.first() == Int::class.java
          }
          .hookBefore {
            val typeMask = it.args[0] as Int
            if (typeMask == WindowInsets.Type.systemGestures()) {
              val ctx = Chrome.getContext()
              val sharedPref = ctx.getSharedPreferences("ChromeXt", Context.MODE_PRIVATE)
              if (sharedPref.getBoolean("gesture_mod", false)) {
                it.result = Insets.of(0, 0, 0, 0)
                toggleGestureConflict(true)
              } else {
                toggleGestureConflict(false)
              }
            }
          }
    }

    isInit = true
  }

  private fun toggleGestureConflict(excludeSystemGesture: Boolean) {
    val activity = Chrome.getContext()
    if (activity is Activity && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      val decoView = activity.window.decorView
      if (excludeSystemGesture) {
        val width = decoView.width
        val height = decoView.height
        val excludeHeight: Int = (activity.resources.displayMetrics.density * 100).roundToInt()
        decoView.setSystemGestureExclusionRects(
            // public Rect (int left, int top, int right, int bottom)
            listOf(Rect(width / 2, height / 2 - excludeHeight, width, height / 2 + excludeHeight)))
      } else {
        decoView.setSystemGestureExclusionRects(listOf(Rect(0, 0, 0, 0)))
      }
    }
  }
}

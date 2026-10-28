package org.matrix.chromext.proxy

import android.content.Context
import android.os.Build
import android.os.Environment
import android.preference.Preference
import android.preference.SwitchPreference
import android.view.View
import java.io.File
import java.io.FileReader
import org.json.JSONObject
import org.luckypray.dexkit.annotations.DexKitExperimentalApi
import org.matrix.chromext.Chrome
import org.matrix.chromext.Listener
import org.matrix.chromext.script.Local
import org.matrix.chromext.script.ScriptFileStorage
import org.matrix.chromext.utils.Log
import org.matrix.chromext.utils.reflect.findField
import org.matrix.chromext.utils.reflect.loadClass
import org.matrix.chromext.utils.reflect.toMethod

@OptIn(DexKitExperimentalApi::class)
object PreferenceProxy {
  val chromeTabbedActivity = UserScriptProxy.chromeTabbedActivity

  val class_MainSettings =
    loadClass("Lorg/chromium/chrome/browser/settings/MainSettings;")

  val class_ChromeBaseSettingsFragment =
    loadClass("Lorg/chromium/chrome/browser/settings/ChromeBaseSettingsFragment;")

  val method_PreferenceFragmentCompat_addPreferencesFromResource = Chrome.dexKitBridge
    .getMethodDirect("PreferenceFragmentCompat.addPreferencesFromResource") { dexKitBridge ->
      dexKitBridge.findMethod {
        matcher {
          @Suppress("TYPE_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
          declaredClass(class_ChromeBaseSettingsFragment.superclass)
          paramTypes(Int::class.javaPrimitiveType)
          returnType(Void.TYPE)
        }
      }.single()
    }.toMethod()

  val method_PreferenceFragmentCompat_findPreference = Chrome.dexKitBridge
    .getMethodDirect("PreferenceFragmentCompat.findPreference") { dexKitBridge ->
      dexKitBridge.findMethod {
        matcher {
          @Suppress("TYPE_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
          declaredClass(class_ChromeBaseSettingsFragment.superclass)
          paramTypes(CharSequence::class.java)
          returnType("androidx.preference.Preference")
        }
      }.single()
    }.toMethod()

  val field_Preference_mClickListener =
    loadClass("androidx.preference.Preference")
      .findField { it.type == View.OnClickListener::class.java }

  fun setClickListener(preferences: Map<String, Preference>) {
    val ctx = Chrome.getContext()
    val sharedPref = ctx.getSharedPreferences("ChromeXt", Context.MODE_PRIVATE)

    var summary = "Click to install eruda, size around 0.5 MiB"
    if (Local.eruda_version != null) {
      summary = "Current version: v" + Local.eruda_version
    }
    preferences["eruda"]!!.summary = summary

    (preferences["gesture_mod"] as SwitchPreference).isChecked =
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
          sharedPref.getBoolean("gesture_mod", false)

    var reset_confirming = 1
    val listeners =
        mapOf(
            "bookmark" to
                fun(_: Any) {
                  val bookmark = File(ctx.filesDir, "../app_chrome/Default/Bookmarks")
                  if (bookmark.exists()) {
                    var html =
                        "<!DOCTYPE NETSCAPE-Bookmark-file-1>\n<!-- This is an automatically generated file. It will be read and overwritten. DO NOT EDIT! -->\n<META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=UTF-8\">\n<TITLE>Bookmarks</TITLE>\n<H1>Bookmarks</H1>\n<DL><p>\n"
                    runCatching {
                          val data =
                              JSONObject(FileReader(bookmark).use { it.readText() })
                                  .getJSONObject("roots")

                          val DIV = 10000000
                          fun loopChildren(folder: JSONObject, indent: String = "\t") {
                            if (folder.optString("type") == "folder" && folder.has("children")) {
                              val children = folder.getJSONArray("children")
                              html +=
                                  indent +
                                      "<DT><H3 ADD_DATE=\"${folder.getLong("date_added") / DIV }\" LAST_MODIFIED=\"${folder.getLong("date_modified") / DIV}\" PERSONAL_TOOLBAR_FOLDER=\"true\">${folder.getString("name")}</H3>\n"
                              html += indent + "<DL><p>\n"
                              for (i in 0 until children.length()) {
                                val item = children.getJSONObject(i)
                                if (item.getString("type") == "url") {
                                  html +=
                                      indent +
                                          "\t<DT><A HREF=\"${item.getString("url")}\" ADD_DATE=\"${item.getLong("date_added") / DIV }\">${item.getString("name")}</A>\n"
                                } else if (item.getString("type") == "folder") {
                                  loopChildren(item, indent + "\t")
                                }
                              }
                              html += indent + "</DL><p>\n"
                            }
                          }

                          val bookmarks = data.names()!!
                          for (i in 0 until bookmarks.length()) {
                            loopChildren(data.getJSONObject(bookmarks.getString(i)))
                          }

                          html += "</DL><p>"
                          File(
                                  Environment.getExternalStoragePublicDirectory(
                                      Environment.DIRECTORY_DOWNLOADS),
                                  "Bookmarks.html")
                              .writeText(html)
                          Log.toast(
                              ctx,
                              "Bookmarks exported to " +
                                  Environment.DIRECTORY_DOWNLOADS +
                                  "/Bookmarks.html")
                        }
                        .onFailure { Log.ex(it) }
                  } else {
                    Log.toast(ctx, "Bookmarks data not found")
                  }
                },
            "eruda" to
                fun(_: Any) {
                  Listener.on("updateEruda")
                },
            "exit" to
                fun(_: Any) {
                  with(
                      ctx.getSharedPreferences(
                              Chrome.getContext().packageName + "_preferences",
                              Context.MODE_PRIVATE)
                          .edit()) {
                        putBoolean("developer", false)
                        apply()
                      }
                  Log.toast(ctx, "Please restart Chrome to apply the changes")
                },
            "gesture_mod" to
                fun(obj: Any) {
                  obj as SwitchPreference
                  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val enabled = sharedPref.getBoolean("gesture_mod", false)
                    obj.isChecked = !enabled
                    with(sharedPref.edit()) {
                      putBoolean("gesture_mod", !enabled)
                      apply()
                    }
                  } else {
                    obj.isChecked = false
                    Log.toast(ctx, "Feature unavailable")
                  }
                },
            "reset" to
                fun(_: Any) {
                  if (reset_confirming < 3) {
                    Log.toast(
                        ctx,
                        "Clik ${3 - reset_confirming} more times if you confirm to reset ChromeXt")
                    reset_confirming += 1
                    return
                  }
                  arrayOf("ChromeXt", "CosmeticFilter", "CSPRule").forEach {
                    with(ctx.getSharedPreferences(it, Context.MODE_PRIVATE).edit()) {
                      clear()
                      apply()
                    }
                  }
                  val file = File(ctx.filesDir, "Eruda.js")
                  if (file.exists()) {
                    file.delete()
                    Local.eruda_version = null
                  }
                  ctx.deleteDatabase("ChromeXt")
                  ScriptFileStorage.deleteAll()
                  Log.toast(ctx, "ChromeXt data are reset")
                },
        )

    preferences.forEach { (name, pref) ->
      pref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
        listeners[name]?.invoke(pref)
        true
      }
    }
  }
}

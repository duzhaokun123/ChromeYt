package org.matrix.chromext.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import org.json.JSONArray
import org.json.JSONObject
import org.matrix.chromext.Chrome
import java.security.MessageDigest

fun String.md5(): String {
  val md5 = MessageDigest.getInstance("MD5")
  val digest = md5.digest(this.toByteArray())
  return digest.toHexString()
}

fun JSONArray.toList(): List<JSONObject> {
  return buildList {
    for (i in 0 until this@toList.length()) {
      add(this@toList.getJSONObject(i))
    }
  }
}

fun dp2px(dp: Number, resources: Resources? = null): Int {
  val resources = resources ?: Chrome.getContext().resources
  return (dp.toDouble() * resources.displayMetrics.density).toInt()
}

fun Context.startActivitySafe(intent: Intent): Throwable? {
  return runCatching {
    if (this !is Activity) {
      Log.w("start from outside of an Activity ")
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    this.startActivity(intent)
  }.exceptionOrNull()
}

fun Collection<*>.toJSONArray() = JSONArray(this)
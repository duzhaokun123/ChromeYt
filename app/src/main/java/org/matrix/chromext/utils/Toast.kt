package org.matrix.chromext.utils

import android.widget.Toast
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.matrix.chromext.MainHook

object Toast {
  fun show(vararg message: String) {
    @OptIn(DelicateCoroutinesApi::class)
    GlobalScope.launch(Dispatchers.Main) {
      Toast.makeText(MainHook.application, message.normalizeToSting(), Toast.LENGTH_LONG).show()
    }
  }

  fun Array<*>.normalizeToSting(): String {
    return this.joinToString(separator = " ") {
      when (it) {
        is Throwable -> it.stackTraceToString()
        is Array<*> -> it.contentDeepToString()
        else -> it.toString()
      }
    }
  }
}
package org.matrix.chromext

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import androidx.core.net.toUri
import org.json.JSONObject
import org.matrix.chromext.script.Local

object ScriptNotification : BroadcastReceiver() {
  const val ACTION_USERSCRIPT_NOTIFICATION = "org.matrix.chromext.ACTION_USERSCRIPT_NOTIFICATION"
  const val EXTRA_NOTIFICATION_ID = "org.matrix.chromext.EXTRA_NOTIFICATION_ID"
  const val EXTRA_SCRIPT_ID = "org.matrix.chromext.EXTRA_SCRIPT_ID"
  const val EXTRA_TAB_ID = "org.matrix.chromext.EXTRA_TAB_ID"
  const val EXTRA_FRAME_ID = "org.matrix.chromext.EXTRA_FRAME_ID"

  var isInit: Boolean = false
    private set


  fun init() {
    if (isInit) return
    val context = Chrome.getContext().applicationContext
    val intentFilter = IntentFilter().apply {
      addAction(ACTION_USERSCRIPT_NOTIFICATION)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      context.registerReceiver(this, intentFilter, Context.RECEIVER_NOT_EXPORTED)
    } else {
      @SuppressLint("UnspecifiedRegisterReceiverFlag")
      context.registerReceiver(this, intentFilter)
    }
    isInit = true
  }

  fun newPendingIntent(scriptId: String, notificationId: Int, tab: Any, frameId: String?): PendingIntent {
    val tabId = Chrome.getTabId(tab)
    val context = Chrome.getContext()
    val intent = Intent().apply {
      action = ACTION_USERSCRIPT_NOTIFICATION
      `package` = context.packageName
      // PendingIntent need unique data
      data = "/notification/$tabId/${Uri.encode(frameId)}/${Uri.encode(scriptId)}/$notificationId".toUri()
      putExtra(EXTRA_SCRIPT_ID, scriptId)
      putExtra(EXTRA_NOTIFICATION_ID, notificationId)
      putExtra(EXTRA_TAB_ID, tabId)
      putExtra(EXTRA_FRAME_ID, frameId)
    }
    return PendingIntent.getBroadcast(context, notificationId, intent, PendingIntent.FLAG_IMMUTABLE)
  }

  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action != ACTION_USERSCRIPT_NOTIFICATION) return
    val scriptId = intent.getStringExtra(EXTRA_SCRIPT_ID) ?: return
    if (!intent.hasExtra(EXTRA_NOTIFICATION_ID)) return
    val notificationId =
      intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
    if (!intent.hasExtra(EXTRA_TAB_ID)) return
    val tabId =
      intent.getIntExtra(EXTRA_TAB_ID, -1)
    val tab = Chrome.getTabById(tabId) ?: return
    val frameId = intent.getStringExtra(EXTRA_FRAME_ID)
    val detail = JSONObject().apply {
      put("id", scriptId)
      put("notificationId", notificationId)
    }
    Chrome.evaluateJavascript(
      ["Symbol.${Local.name}.unlock(${Local.key}).post('notification', ${detail});"], tab, frameId
    )
  }
}
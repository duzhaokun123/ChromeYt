package org.matrix.chromext.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toolbar
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.matrix.chromext.Chrome
import org.matrix.chromext.R
import org.matrix.chromext.Resource
import org.matrix.chromext.devtools.getInspectPages
import org.matrix.chromext.utils.toList
import kotlin.time.Duration.Companion.seconds

data class InspectPage(
  val title: String,
  val description: String,
  val type: String,
  val id: String,
  val url: String,
  val devtoolsFrontendUrl: String,
)

class InspectActivity : Activity(), OnApplyWindowInsetsListener {
  val toolbar by lazy { findViewById<Toolbar>(R.id.toolbar) }
  val lv_inspect by lazy { findViewById<ListView>(R.id.lv_inspect) }

  var inspectPages = emptyList<InspectPage>()

  private val scope = MainScope()

  override fun onCreate(savedInstanceState: Bundle?) {
    Resource.enrich(this)

    setTheme(android.R.style.Theme_DeviceDefault_NoActionBar)
    super.onCreate(savedInstanceState)
    setContentView(R.layout.layout_inspect)
    setActionBar(toolbar)
    setTitle("inspect")

    lv_inspect.adapter =
      object : BaseAdapter() {
        override fun getCount() = inspectPages.size
        override fun getItem(position: Int) = inspectPages[position]
        override fun getItemId(position: Int) = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
          val view = convertView ?: LayoutInflater.from(this@InspectActivity)
            .inflate(R.layout.item_inspect, parent, false)
          val inspectPage = inspectPages[position]

          view.apply {
            findViewById<TextView>(R.id.tv_info).text = """
              |${inspectPage.title}
              |${inspectPage.url}
              |type: ${inspectPage.type} id: ${inspectPage.id}
              |${inspectPage.description}
            """.trimMargin()
            findViewById<Button>(R.id.btn_inspect).setOnClickListener {
              val intent = CustomTabsIntent.Builder()
                .setUrlBarHidingEnabled(true)
                .setShowTitle(true)
                .build().intent.apply {
                  setData(inspectPage.devtoolsFrontendUrl.toUri())
                  setPackage(Chrome.packageName)
                  addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
              startActivity(intent)
            }
          }

          return view
        }
      }

    scope.launch {
      while (isActive) {
        if (window.isActive) {
          updateInspectPages()
        }
        delay(10.seconds)
      }
    }

    ViewCompat.setOnApplyWindowInsetsListener(window.decorView, this)
  }

  override fun onApplyWindowInsets(
    v: View,
    insets: WindowInsetsCompat
  ): WindowInsetsCompat {
    insets.getInsets(WindowInsetsCompat.Type.systemBars()).let { systemBars ->
      toolbar.updatePadding(top = systemBars.top)
      lv_inspect.updatePadding(bottom = systemBars.bottom)
    }
    return WindowInsetsCompat.CONSUMED
  }

  override fun onDestroy() {
    scope.cancel()
    super.onDestroy()
  }

  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    menuInflater.inflate(R.menu.menu_inspect, menu)
    return super.onCreateOptionsMenu(menu)
  }

  override fun onMenuItemSelected(featureId: Int, item: MenuItem): Boolean {
    when(item.itemId) {
      R.id.reflash -> {
        updateInspectPages()
        return true
      }
    }
    return super.onMenuItemSelected(featureId, item)
  }

  fun updateInspectPages() {
    scope.launch(Dispatchers.IO) {
      val pages = getInspectPages() ?: return@launch
      inspectPages = pages.toList().map {
        InspectPage(
          title = it.getString("title"),
          description = it.getString("description"),
          type = it.getString("type"),
          id = it.getString("id"),
          url = it.getString("url"),
          devtoolsFrontendUrl = it.getString("devtoolsFrontendUrl")
        )
      }
      withContext(Dispatchers.Main) {
        (lv_inspect.adapter as BaseAdapter).notifyDataSetChanged()
      }
    }
  }
}
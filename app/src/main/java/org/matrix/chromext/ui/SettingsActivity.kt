package org.matrix.chromext.ui

import android.app.Activity
import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceFragment
import android.view.View
import android.widget.FrameLayout
import android.widget.Toolbar
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import org.matrix.chromext.R
import org.matrix.chromext.Resource
import org.matrix.chromext.proxy.PreferenceProxy

class SettingsPreferenceFragment : PreferenceFragment() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    preferenceManager.sharedPreferencesName = "ChromeXt"
    addPreferencesFromResource(R.xml.settings_preferences)

    val preferences = mutableMapOf<String, Preference>()
    arrayOf("eruda", "gesture_mod", "bookmark", "reset", "exit").forEach {
      preferences[it] = findPreference(it)
    }
    PreferenceProxy.setClickListener(preferences)
  }
}

class SettingsActivity : Activity(), OnApplyWindowInsetsListener {
  val toolbar by lazy { findViewById<Toolbar>(R.id.toolbar) }
  val fl by lazy { findViewById<FrameLayout>(R.id.fl) }

  override fun onCreate(savedInstanceState: Bundle?) {
    Resource.enrich(this)

    setTheme(android.R.style.Theme_DeviceDefault_NoActionBar)
    super.onCreate(savedInstanceState)
    setContentView(R.layout.layout_settings)
    setActionBar(toolbar)
    setTitle("Settings")

    if (savedInstanceState == null) {
      fragmentManager.beginTransaction()
        .replace(R.id.fl, SettingsPreferenceFragment())
        .commit()
    }

    ViewCompat.setOnApplyWindowInsetsListener(window.decorView, this)
  }


  override fun onApplyWindowInsets(
    v: View,
    insets: WindowInsetsCompat
  ): WindowInsetsCompat {
    insets.getInsets(WindowInsetsCompat.Type.systemBars()).let { systemBars ->
      toolbar.updatePadding(top = systemBars.top)
      fl.updatePadding(bottom = systemBars.bottom)
    }
    return WindowInsetsCompat.CONSUMED
  }
}
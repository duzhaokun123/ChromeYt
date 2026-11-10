package org.matrix.chromext.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toolbar
import androidx.core.net.toUri
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.matrix.chromext.R
import org.matrix.chromext.Resource
import org.matrix.chromext.db.entity.Script
import org.matrix.chromext.logger.AndroidLogger
import org.matrix.chromext.script.ScriptDbManager
import org.matrix.chromext.utils.Log
import org.matrix.chromext.utils.ScriptMetadata
import org.matrix.chromext.utils.ScriptParser
import org.matrix.chromext.utils.Toast
import org.matrix.chromext.utils.asText
import org.matrix.chromext.utils.loadExternalResource

class ScriptConfigActivity : Activity(), OnApplyWindowInsetsListener {
  val toolbar by lazy { findViewById<Toolbar>(R.id.toolbar) }
  val sv by lazy { findViewById<ScrollView>(R.id.sv) }
  val sp_userRunAt by lazy { findViewById<Spinner>(R.id.sp_userRunAt) }
  val et_userMatch by lazy { findViewById<EditText>(R.id.et_userMatch) }
  val et_userInclude by lazy { findViewById<EditText>(R.id.et_userInclude) }
  val et_userExclude by lazy { findViewById<EditText>(R.id.et_userExclude) }
  val et_userConnect by lazy { findViewById<EditText>(R.id.et_userConnect) }
  val tv_raw by lazy { findViewById<TextView>(R.id.tv_raw) }
  val cb_enabled by lazy { findViewById<CheckBox>(R.id.cb_enabled) }

  var scriptId: String? = null
  var script: Script? = null
  lateinit var metadata: ScriptMetadata

  private val scope = MainScope()

  override fun onCreate(savedInstanceState: Bundle?) {
    Resource.enrich(this)

    setTheme(android.R.style.Theme_DeviceDefault_NoActionBar)
    super.onCreate(savedInstanceState)
    setContentView(R.layout.layout_script_config)
    setActionBar(toolbar)

    scriptId = intent.getStringExtra("id")
    if (scriptId == null) {
      setTitle("not select script")
      return
    }
    runBlocking {
      script = ScriptDbManager.getScript(scriptId!!)
    }
    if (script == null) {
      setTitle("no script ($scriptId)")
      return
    }

    metadata = ScriptParser.parseMetadata(script?.meta!!)
    setTitle(metadata.localeName ?: scriptId)

    cb_enabled.isChecked = script!!.enabled
    cb_enabled.setOnCheckedChangeListener { _, checked ->
      script!!.enabled = checked
    }
    sp_userRunAt.adapter = ArrayAdapter(
      this,
      android.R.layout.simple_spinner_item,
      Script.RunAt.entries
    ).apply {
      setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    }
    sp_userRunAt.setSelection(Script.RunAt.entries.indexOf(script!!.userRunAt))
    sp_userRunAt.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
      override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        script!!.userRunAt = Script.RunAt.entries[position]
      }

      override fun onNothingSelected(parent: AdapterView<*>) {}
    }
    et_userMatch.setText(script!!.userMatch)
    et_userMatch.doAfterTextChanged { editable ->
      script!!.userMatch = editable.toString()
    }
    et_userInclude.setText(script!!.userInclude)
    et_userInclude.doAfterTextChanged { editable ->
      script!!.userInclude = editable.toString()
    }
    et_userExclude.setText(script!!.userExclude)
    et_userExclude.doAfterTextChanged { editable ->
      script!!.userExclude = editable.toString()
    }
    et_userConnect.setText(script!!.userConnect)
    et_userConnect.doAfterTextChanged { editable ->
      script!!.userConnect = editable.toString()
    }
    tv_raw.text = script!!.meta

    ViewCompat.setOnApplyWindowInsetsListener(window.decorView, this)
  }

  override fun onDestroy() {
    scope.cancel()
    super.onDestroy()
  }

  override fun onApplyWindowInsets(
    v: View,
    insets: WindowInsetsCompat
  ): WindowInsetsCompat {
    insets.getInsets(WindowInsetsCompat.Type.systemBars()).let { systemBars ->
      toolbar.updatePadding(top = systemBars.top)
      sv.updatePadding(bottom = systemBars.bottom)
    }
    return WindowInsetsCompat.CONSUMED
  }


  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    menuInflater.inflate(R.menu.menu_script_config, menu)
    return super.onCreateOptionsMenu(menu)
  }

  override fun onMenuItemSelected(featureId: Int, item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.save -> {
        if (script != null) {
          runBlocking {
            ScriptDbManager.upsert(script!!)
          }
        } else {
          Log.toast(this, "no script")
        }
        return true
      }

      R.id.update -> {
        if (script == null) {
          Toast.show("no script")
          return true
        }
        val updateUrl = metadata.getMetadata("updateURL") ?: metadata.getMetadata("downloadURL")
        if (updateUrl == null) {
          AlertDialog.Builder(this)
            .setTitle("no way to check update")
            .setMessage(
              "script has no updateURL nor downloadURL\n" +
                  "get update from where you download it"
            )
            .show()
        } else {
          val checkingDialog =
            AlertDialog.Builder(this)
              .setTitle("checking")
              .setCancelable(false)
              .show()
          scope.launch(Dispatchers.IO) {
            runCatching {
              ScriptParser.parseScriptMetadata(loadExternalResource(updateUrl).asText())
                ?: throw RuntimeException("invalid metadata from $updateUrl")
            }.onFailure { e ->
              AndroidLogger.e(e)
              withContext(Dispatchers.Main) {
                checkingDialog.cancel()
                AlertDialog.Builder(this@ScriptConfigActivity)
                  .setTitle("failed")
                  .setMessage(e.localizedMessage ?: e.message ?: e.toString())
                  .show()
              }
            }.onSuccess { newMetadata ->
              withContext(Dispatchers.Main) {
                checkingDialog.cancel()
                if (newMetadata.version == metadata.version) {
                  Toast.show("no update")
                } else {
                  AlertDialog.Builder(this@ScriptConfigActivity)
                    .setTitle("update")
                    .setMessage("${metadata.version} -> ${newMetadata.version}")
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                      val downloadUrl = newMetadata.getMetadata("downloadURL") ?: newMetadata.getMetadata("updateUrl")
                      if (downloadUrl == null) {
                        Toast.show("no downloadURL")
                      } else {
                        startActivity(
                          Intent(
                            this@ScriptConfigActivity, ScriptInstallActivity::class.java
                          ).apply {
                            data = downloadUrl.toUri()
                          })
                        finish()
                      }
                    }.show()
                }
              }
            }
          }
        }
        return true
      }
    }
    return super.onMenuItemSelected(featureId, item)
  }
}
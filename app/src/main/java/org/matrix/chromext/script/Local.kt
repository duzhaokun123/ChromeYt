package org.matrix.chromext.script

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileReader
import kotlin.random.Random
import org.json.JSONArray
import org.json.JSONObject
import org.matrix.chromext.BuildConfig
import org.matrix.chromext.Chrome
import org.matrix.chromext.Resource
import org.matrix.chromext.db.entity.Script
import org.matrix.chromext.utils.Log
import org.matrix.chromext.utils.randomString
import org.matrix.chromext.utils.toJSONArray
import kotlin.io.encoding.Base64

object GM {
  private val localScript: Map<String, String>

  init {
    val ctx = Chrome.getContext()
    Resource.enrich(ctx)
    localScript =
        ctx.assets
            .open("GM.js")
            .bufferedReader()
            .use { it.readText() }
            .split("// Kotlin separator\n\n")
            .associateBy(
                {
                  val decalre = it.lines()[0]
                  val sep = if (decalre.startsWith("function")) "(" else " ="
                  decalre.split(sep)[0].split(" ").last()
                },
                { it })
  }

  fun bootstrap(
    script: LoadedScript,
    codes: MutableList<String> = mutableListOf<String>(),
  ): MutableList<String> {
    var code = script.code
    var grants = ""

    script.options.grant.forEach {
      when (it) {
        "none" -> return@forEach
        "frames" -> return@forEach
        "GM_info" -> return@forEach
        "GM.ChromeXt" -> return@forEach
        "window.close" -> return@forEach
        else ->
            if (localScript.containsKey(it)) {
              grants += localScript.get(it)
            } else if (it.startsWith("GM_")) {
              grants +=
                  "function ${it}(){ console.error('${it} is not implemented in ChromeXt yet, called with', arguments) }\n"
            } else if (it.startsWith("GM.")) {
              val func = it.substring(3)
              val name =
                  "GM_" +
                      if (func == "xmlHttpRequest") {
                        "xmlhttpRequest"
                      } else if (func == "getResourceUrl") {
                        "getResourceURL"
                      } else {
                        func
                      }
              if (localScript.containsKey(name) && !script.options.grant.contains(name))
                  grants += localScript.get(name)
              grants += "${it}={sync: ${name}};\n"
            }
      }
    }

    grants += localScript.get("GM.bootstrap")!!

    val GM_info = JSONObject().apply {
      put("scriptHandler", BuildConfig.SCRIPT_HANDLER_NAME)
      put("version", BuildConfig.VERSION_NAME)
      put("scriptMetaStr", script.script.meta)
      put("scriptWillUpdate", false)
      put("script", JSONObject().apply {
        put("name", script.metadata.name)
        put("namespace", script.metadata.namespace)
        put("description", script.metadata.description)
        put("version", script.metadata.version)
        put("author", script.metadata.author)
        put("includes", script.metadata.include.toJSONArray())
        put("matches", script.metadata.match.toJSONArray())
        put("excludes", script.metadata.exclude.toJSONArray())
        put("grant", script.metadata.grant.toJSONArray())
//        put("resources", script.metadata.getAllMetadata("resource").toJSONArray())
        put("icon", script.metadata.icon)
        put("homepage", script.metadata.getMetadata("homepage"))
        put("homepageURL", script.metadata.getMetadata("homepageURL"))
        put("updateURL", script.metadata.getMetadata("updateURL"))
        put("downloadURL", script.metadata.getMetadata("downloadURL"))
        put("unwrap", script.metadata.hasMetadata("unwrap"))
        put("require", script.metadata.require.toJSONArray())
        put("noframes", script.metadata.noframes)
        put("run-at", script.metadata.`run-at`.takeIf { it != Script.RunAt.UNSET }?.value ?: "")
        put("runAt", script.metadata.`run-at`.takeIf { it != Script.RunAt.UNSET }?.value ?: "")
        put("connects", script.metadata.connect.toJSONArray())
        put("connect", script.metadata.connect.toJSONArray())

        put("id", script.id)
      })
      put("uuid", script.uuid)
      put("injectInto", script.metadata.getMetadata("inject-into"))
      put("storage", script.storage)
    }
    val loadConfig = JSONObject().apply {
      put("resource", script.metadata.getAllMetadata("resource").toJSONArray())
      put("run-at", script.options.runAt.value)
      put("inject-into", script.metadata.getMetadata("inject-into"))
      put("sandbox", script.metadata.getMetadata("sandbox"))
      put("grant", script.options.grant.toJSONArray())
      put("require", script.options.require.toJSONArray())
      put("connect", script.options.connect.toJSONArray())
    }
    code = "\ndelete window.__loading__;\n${code};"
    code = localScript.get("globalThis")!! + script.lib.joinToString("\n") + code
    codes.add("""
(() => {
  const GM = {key:${Local.key}, name: '${Local.name}'};
  const GM_info = ${GM_info};
  const loadConfig = ${loadConfig};
  loadConfig.code = (key=null) => {${code}};
  ${grants};
  GM.bootstrap();
})();

//# sourceURL=local://ChromeXt/${Uri.encode(script.id)}
    """.trimIndent())
    return codes
  }
}

object Local {

  val promptInstallUserScript: String
  val customizeDevTool: String
  val eruda: String
  val initChromeXt: String
  val openEruda: String
  val cspRule: String
  val cosmeticFilter: String
  val updateMenuCommands: String

  val key = Random.nextDouble()
  val name = randomString(25)

  var eruda_version: String?

  // lineNumber of the anchor in GM.js, used to verify ChromeXt.dispatch
  val anchorInChromeXt: Int
  init {
    val ctx = Chrome.getContext()
    Resource.enrich(ctx)

    customizeDevTool = ctx.assets.open("devtools.js").bufferedReader().use { it.readText() }
    val css =
        JSONArray(ctx.assets.open("eruda.css").bufferedReader().use { it.readText() }.split("\n\n"))
    eruda =
        "eruda._styles = ${css};\n" +
            ctx.assets
                .open("eruda.js")
                .bufferedReader()
                .use { it.readText() }
                .replaceFirst("Symbol.ChromeXt", "Symbol." + name)
                .replaceFirst("ChromeXtUnlockKeyForEruda", key.toString())
    eruda_version = getErudaVersion()

    val seed = Random.nextDouble()
    // Use empty lines to randomize anchorInChromeXt
    val parts = loadScript(ctx, "scripts/initChromeXt.js")
            .split("\n")
            .filter { it.isNotEmpty() || Random.nextDouble() > seed }
    anchorInChromeXt = parts.indexOfFirst { it.endsWith("// Kotlin anchor") } + 2
    initChromeXt = parts.joinToString("\n")

    updateMenuCommands = loadScript(ctx, "scripts/updateMenuCommands.js")
    cspRule = loadScript(ctx, "scripts/cspRule.js")
    cosmeticFilter = loadScript(ctx, "scripts/cosmeticFilter.js")
    openEruda = loadScript(ctx, "scripts/openEruda.js")
    val promptInstallUserScriptCss =
      Base64.encode(loadText(ctx, "scripts/promptInstallUserScript.css").toByteArray())
    promptInstallUserScript =
      loadScript(ctx, "scripts/promptInstallUserScript.js")
        .replace("PlaceHolder_cssString", "atob('$promptInstallUserScriptCss')")
  }

  fun getErudaVersion(ctx: Context = Chrome.getContext(), versionText: String? = null): String? {
    val eruda = File(ctx.filesDir, "Eruda.js")
    if (eruda.exists() || versionText != null) {
      val verisonReg = Regex(" eruda v(?<version>[\\d\\.]+) https://")
      val firstLine = (versionText ?: FileReader(eruda).use { it.readText() }).lines()[0]
      val vMatchGroup = verisonReg.find(firstLine)?.groups
      if (vMatchGroup != null) {
        return vMatchGroup[1]?.value as String
      } else if (eruda.exists()) {
        eruda.delete()
        Log.toast(ctx, "Eruda.js is corrupted")
      }
    }
    return null
  }

  private fun loadScript(context: Context, path: String): String {
    return context.assets
      .open(path)
      .reader()
      .use { it.readText() }
      .replace("PlaceHolder_name", name)
      .replace("PlaceHolder_key", key.toString())
  }

  private fun loadText(context: Context, path: String): String {
    return context.assets
      .open(path)
      .reader()
      .use { it.readText() }
  }
}

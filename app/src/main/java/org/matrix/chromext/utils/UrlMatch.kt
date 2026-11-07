package org.matrix.chromext.utils

import com.hrakaroo.glob.GlobPattern
import org.matrix.chromext.script.LoadedScript
import java.net.URL

object UrlMatch {
  // use ^([a-zA-Z0-9*]+)://([a-zA-Z0-9.*-]*|\[[a-fA-F0-9:.]+(?:%.+)?\])(:[0-9*]+)?(/.*)$
  // to match `[::ffff:127.0.0.1]` `[fe80::1%25eth0]` ?
  val matchRegex = Regex("""^([a-zA-Z0-9*]+)://([a-zA-Z0-9.*-]*|\[[a-fA-F0-9:]+])(:[0-9*]+)?(/.*)$""")

  /**
   * 使用 chrome match pattern 测试 [url]
   */
  fun testMatch(url: String, exp: String): Boolean {
    val aUrl = runCatching { URL(url) }.getOrNull() ?: return false
    val aProtocol = aUrl.protocol.lowercase()
    val aHost = aUrl.host.lowercase()
    val aPort = ":" +
        if (aUrl.port != -1) {
          aUrl.port
        } else {
          when (aProtocol) {
            "http" -> 80
            "https" -> 443
            else -> -1
          }
        }
    val aPath = aUrl.path

    if (exp == "<all_urls>") {
      if (aProtocol in ["http", "https", "file"]) return true
    }

    val bMatches = matchRegex.find(exp) ?: return false
    val bGroupValues = bMatches.groupValues
    val bProtocol = bGroupValues[1].lowercase()
    val bHost = bGroupValues[2].lowercase()
    val bPort = bGroupValues[3]
    val bPath = bGroupValues[4]

    if (bProtocol !in ["http", "https", "*", "file", "http*"]) return false
    if (bProtocol in ["*", "http*"]) {
      if (aProtocol !in ["http", "https"]) return false
    } else {
      if (aProtocol != bProtocol) return false
    }

    if (bPort !in ["", ":*"]) {
      if (aPort != bPort) return false
    }

    if (bHost.startsWith("*.")) {
      val hostSuffix = bHost.drop(2)
      if (aHost != hostSuffix && !aHost.endsWith(".$hostSuffix")) return false
    } else if (bHost != "*") {
      if (aHost != bHost) return false
    }

    if (bPath != "/*") {
      if ("*" in bPath) {
        val glob = GlobPattern.compile(bPath, '*', GlobPattern.NULL_CHARACTER, 0)
        if (!glob.matches(aPath)) return false
      } else {
        if (aPath != bPath) return false
      }
    }

    return true
  }

  /**
   * 使用 glob 风格测试 [url]
   */
  fun testGlob(url: String, exp: String): Boolean {
    val glob = GlobPattern.compile(exp, '*', GlobPattern.NULL_CHARACTER, GlobPattern.CASE_INSENSITIVE)
    return glob.matches(url)
  }

  /**
   * 使用正则测试 [url]
   */
  fun testRegex(url: String, exp: String): Boolean {
    runCatching {
      val reg = Regex(exp, RegexOption.IGNORE_CASE)
      return reg.containsMatchIn(url)
    }
    return false
  }

  /**
   * 测试用户脚本是匹配 [url]
   */
  fun testScript(url: String, scriptOptions: LoadedScript.Options): Boolean {
    scriptOptions.exclude.forEach { exclude ->
      if (exclude.startsWith('/') && exclude.endsWith('/')) {
        if (testRegex(url, exclude.removeSurrounding("/", "/"))) {
          return false
        }
      } else {
        if (testGlob(url, exclude)) {
          return false
        }
      }
    }
    scriptOptions.match.forEach { match ->
      if (testMatch(url, match)) {
        return true
      }
    }
    scriptOptions.include.forEach { include ->
      if (include.startsWith('/') && include.endsWith('/')) {
        if (testRegex(url, include.removeSurrounding("/", "/"))) {
          return true
        }
      } else {
        if (testGlob(url, include)) {
          return true
        }
      }
    }
    return false
  }
}
package org.matrix.chromext.utils

import org.matrix.chromext.script.LoadedScript

object UrlMatch {
  /**
   * 使用 chrome match pattern 测试 [url]
   */
  fun testMatch(url: String, exp: String): Boolean {
    TODO()
  }

  /**
   * 使用 glob 风格测试 [url]
   */
  fun testGlob(url: String, exp: String): Boolean {
    TODO()
  }

  /**
   * 使用正则测试 [url]
   */
  fun testRegex(url: String, exp: String): Boolean {
    TODO()
  }

  /**
   * 测试用户脚本是匹配 [url]
   */
  fun testScript(url: String, scriptOptions: LoadedScript.Options): Boolean {
    scriptOptions.exclude.forEach { exclude ->
      if (exclude.startsWith('/') && exclude.endsWith('/')) {
        if (testRegex(url, exclude.substring(1, exclude.length - 1))) {
          return false
        }
      } else {
        if (testGlob(url, exclude)) {
          return false
        }
      }
    }
    if (scriptOptions.match.isNotEmpty()) {
      scriptOptions.match.forEach { match ->
        if (testMatch(url, match)) {
          return true
        }
      }
    } else {
      scriptOptions.include.forEach { include ->
        if (include.startsWith('/') && include.endsWith('/')) {
          if (testRegex(url, include.substring(1, include.length - 1))) {
            return true
          }
        } else {
          if (testGlob(url, include)) {
            return true
          }
        }
      }
    }
    return false
  }
}
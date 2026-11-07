package org.matrix.chromext.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.matrix.chromext.db.entity.Script
import org.matrix.chromext.script.LoadedScript

/**
 * UrlMatch 的行为以主流用户脚本管理器为准：
 * - [UrlMatch.testMatch] 使用 Chrome match patterns 语义（Tampermonkey 与 Violentmonkey 的 @match）
 * - [UrlMatch.testGlob] 使用 Greasemonkey/Violentmonkey 的 include/exclude glob 语义：只有 `*` 是通配符
 * - [UrlMatch.testRegex] 使用用户脚本的 /.../ 正则语义（默认大小写不敏感）
 * - [UrlMatch.testScript] 中 @match 与 @include 是并集，@exclude 优先
 */
class UrlMatchTest {

  private fun options(
    match: List<String> = emptyList(),
    include: List<String> = emptyList(),
    exclude: List<String> = emptyList(),
  ) =
    LoadedScript.Options(
      match = match,
      include = include,
      exclude = exclude,
      grant = emptyList(),
      require = emptyList(),
      connect = emptyList(),
      runAt = Script.RunAt.UNSET,
      noFrames = false,
      hasStorage = false,
    )

  private fun assertMatch(url: String, exp: String) {
    assertTrue("expected <$exp> to match <$url>", UrlMatch.testMatch(url, exp))
  }

  private fun assertNoMatch(url: String, exp: String) {
    assertFalse("expected <$exp> not to match <$url>", UrlMatch.testMatch(url, exp))
  }

  private fun assertGlob(url: String, exp: String) {
    assertTrue("expected glob <$exp> to match <$url>", UrlMatch.testGlob(url, exp))
  }

  private fun assertNoGlob(url: String, exp: String) {
    assertFalse("expected glob <$exp> not to match <$url>", UrlMatch.testGlob(url, exp))
  }

  private fun assertRegex(url: String, exp: String) {
    assertTrue("expected regex <$exp> to match <$url>", UrlMatch.testRegex(url, exp))
  }

  private fun assertNoRegex(url: String, exp: String) {
    assertFalse("expected regex <$exp> not to match <$url>", UrlMatch.testRegex(url, exp))
  }

  // ---------------------------------------------------------------- testMatch

  @Test
  fun `match wildcard scheme matches only http and https`() {
    assertMatch("http://example.com/", "*://example.com/*")
    assertMatch("https://example.com/", "*://example.com/*")
    assertNoMatch("ftp://example.com/", "*://example.com/*")
    assertNoMatch("file:///tmp/a.html", "*://example.com/*")
    assertNoMatch("chrome://settings/", "*://example.com/*")
  }

  @Test
  fun `match respects a concrete scheme`() {
    assertMatch("http://example.com/", "http://example.com/*")
    assertNoMatch("https://example.com/", "http://example.com/*")
    assertMatch("https://example.com/", "https://example.com/*")
    assertNoMatch("http://example.com/", "https://example.com/*")
  }

  @Test
  fun `match accepts the http star scheme extension`() {
    assertMatch("http://example.com/", "http*://example.com/*")
    assertMatch("https://example.com/", "http*://example.com/*")
    assertNoMatch("ftp://example.com/", "http*://example.com/*")
  }

  @Test
  fun `match supports the file scheme`() {
    assertMatch("file:///foo", "file:///foo*")
    assertMatch("file:///foo/bar.html", "file:///foo*")
    assertNoMatch("file:///bar.html", "file:///foo*")
  }

  @Test
  fun `match host must be exact unless wildcarded`() {
    assertMatch("https://example.com/", "https://example.com/*")
    assertNoMatch("https://sub.example.com/", "https://example.com/*")
    assertNoMatch("https://evil-example.com/", "https://example.com/*")
  }

  @Test
  fun `match star dot host also matches the bare domain`() {
    assertMatch("https://example.com/", "https://*.example.com/*")
    assertMatch("https://www.example.com/", "https://*.example.com/*")
    assertMatch("https://a.b.example.com/", "https://*.example.com/*")
    assertNoMatch("https://evil-example.com/", "https://*.example.com/*")
    assertNoMatch("https://example.org/", "https://*.example.com/*")
  }

  @Test
  fun `match wildcard host matches any host`() {
    assertMatch("https://example.com/", "https://*/*")
    assertMatch("https://a.b.example.org/", "https://*/*")
    assertMatch("http://127.0.0.1:8080/", "*://*/*")
  }

  @Test
  fun `match host comparison is case insensitive`() {
    assertMatch("https://EXAMPLE.com/", "https://example.COM/*")
  }

  @Test
  fun `match without an explicit port accepts any port`() {
    assertMatch("http://example.com/a", "http://example.com/*")
    assertMatch("http://example.com:8080/a", "http://example.com/*")
    assertMatch("http://example.com:8080/a", "http://example.com:*/*")
  }

  @Test
  fun `match with an explicit port only accepts that port`() {
    assertMatch("http://example.com:8080/a", "http://example.com:8080/*")
    assertNoMatch("http://example.com/a", "http://example.com:8080/*")
    assertNoMatch("http://example.com:9090/a", "http://example.com:8080/*")
  }

  @Test
  fun `match path star matches any path`() {
    assertMatch("https://example.com/", "https://example.com/*")
    assertMatch("https://example.com/a", "https://example.com/*")
    assertMatch("https://example.com/a/b/c", "https://example.com/*")
  }

  @Test
  fun `match path wildcards can appear at any position`() {
    assertMatch("https://example.com/foo", "https://example.com/foo*")
    assertMatch("https://example.com/foo/bar", "https://example.com/foo*")
    assertMatch("https://example.com/fooXbar", "https://example.com/foo*bar")
    assertMatch("https://example.com/foo/baz/bar", "https://example.com/foo*bar")
    assertNoMatch("https://example.com/foo", "https://example.com/foo*bar")
  }

  @Test
  fun `match path comparison is case sensitive`() {
    assertMatch("https://example.com/foo", "https://example.com/foo")
    assertNoMatch("https://example.com/FOO", "https://example.com/foo*")
  }

  @Test
  fun `match ignores the query string and hash`() {
    assertMatch("https://example.com/foo?x=1", "https://example.com/foo")
    assertMatch("https://example.com/foo#h", "https://example.com/foo")
    assertMatch("https://example.com/foo?x=1", "https://example.com/foo*")
    assertMatch("https://example.com/?q=1", "https://example.com/")
    assertMatch("https://example.com/#h", "https://example.com/")
  }

  @Test
  fun `match without a wildcard is an exact path match`() {
    assertMatch("https://example.com/foo", "https://example.com/foo")
    assertNoMatch("https://example.com/foo/bar", "https://example.com/foo")
    assertNoMatch("https://example.com/", "https://example.com/foo")
  }

  @Test
  fun `match all urls covers http https and file`() {
    assertMatch("http://example.com/x", "<all_urls>")
    assertMatch("https://example.com/x", "<all_urls>")
    assertMatch("file:///tmp/a.html", "<all_urls>")
    assertNoMatch("chrome://settings/", "<all_urls>")
  }

  @Test
  fun `match returns false for malformed patterns`() {
    assertNoMatch("https://example.com/", "example.com/*")
    assertNoMatch("https://example.com/", "://*/*")
    assertNoMatch("https://example.com/", "*//*/*")
    assertNoMatch("https://example.com/", "https://")
  }

  // ---------------------------------------------------------------- testGlob

  @Test
  fun `glob without a wildcard requires the whole url to be equal`() {
    assertGlob("https://a.com/", "https://a.com/")
    assertNoGlob("https://a.com/x", "https://a.com/")
    assertNoGlob("https://a.com/?q=1", "https://a.com/")
    assertNoGlob("https://a.com/path", "a.com")
  }

  @Test
  fun `glob comparison is case insensitive`() {
    assertGlob("https://A.COM/Path", "https://a.com/path")
    assertGlob("HTTPS://a.com/", "https://a.com/*")
  }

  @Test
  fun `glob star matches any sequence including slashes and colons`() {
    assertGlob("https://a.com/", "https://a.com/*")
    assertGlob("https://a.com/a/b/c", "https://a.com/*")
    assertGlob("https://a.com/a/b?x=1#h", "https://a.com/*")
    assertGlob("ftp://example.com/x", "*://*/*")
    assertGlob("anything at all", "*")
  }

  @Test
  fun `glob star can match a prefix or a suffix`() {
    assertGlob("https://a.com/pre", "https://a.com/pre*")
    assertGlob("https://a.com/xfix", "*fix")
    assertNoGlob("https://a.com/fixx", "*fix")
  }

  @Test
  fun `glob supports multiple wildcards`() {
    assertGlob("https://a.example.com/foo", "https://*.example.com/*")
    assertNoGlob("https://example.org/foo", "https://*.example.com/*")
  }

  @Test
  fun `glob treats question mark as a literal character`() {
    assertGlob("https://a.com/?", "https://a.com/?")
    assertGlob("https://a.com/?q", "https://a.com/?*")
    assertNoGlob("https://a.com/", "https://a.com/?")
    assertNoGlob("https://a.com/x", "https://a.com/?")
    assertNoGlob("https://a.com/xy", "https://a.com/?")
  }

  @Test
  fun `glob treats other regex metacharacters as literals`() {
    assertGlob("https://a.com/a.b", "https://a.com/a.b")
    assertNoGlob("https://a.com/axb", "https://a.com/a.b")
    assertGlob("https://a.com/(x)", "https://a.com/(x)")
    assertNoGlob("https://a.com/x", "https://a.com/(x)")
    assertGlob("https://a.com/[x]", "https://a.com/[x]")
    assertNoGlob("https://a.com/x", "https://a.com/[x]")
  }

  @Test
  fun `glob does not treat slash delimited text as a regular expression`() {
    assertNoGlob("https://a.com/x", "/a\\.com/")
  }

  // ---------------------------------------------------------------- testRegex

  @Test
  fun `regex uses contains semantics without anchors`() {
    assertRegex("https://example.com/a", "example")
    assertRegex("https://www.google.com/any/subview", "\\.google\\.com[\\.\\/]")
    assertRegex("https://www.google.com/", "\\.google\\.com[\\.\\/]")
    assertRegex("http://www.google.com/", "\\.google\\.com[\\.\\/]")
    assertRegex("https://www.google.com.hk/", "\\.google\\.com[\\.\\/]")
  }

  @Test
  fun `regex is case insensitive`() {
    assertRegex("https://example.com/a", "EXAMPLE")
    assertRegex("https://EXAMPLE.com/", "example")
  }

  @Test
  fun `regex honors explicit anchors`() {
    assertRegex("https://example.com/", """^https://example\.com/$""")
    assertNoRegex("https://example.com/a", """^https://example\.com/$""")
    assertNoRegex("https://example.com/a", """^https://example\.org/""")
  }

  @Test
  fun `regex accepts javascript style escaped slashes`() {
    assertRegex("https://a.com/x", """^https?:\/\/a\.com\/""")
  }

  @Test
  fun `regex returns false for an invalid pattern`() {
    assertNoRegex("https://a.com/", "(")
    assertNoRegex("https://a.com/", "[a-")
  }

  // ---------------------------------------------------------------- testScript

  @Test
  fun `script exclude glob overrides match`() {
    val options =
      options(match = listOf("https://a.com/*"), exclude = listOf("https://a.com/private/*"))

    assertFalse(UrlMatch.testScript("https://a.com/private/x", options))
    assertTrue(UrlMatch.testScript("https://a.com/public/x", options))
  }

  @Test
  fun `script exclude regex overrides include`() {
    val options =
      options(
        include = listOf("https://a.com/*"),
        exclude = listOf("""/^https:\/\/a\.com\/private/"""),
      )

    assertFalse(UrlMatch.testScript("https://a.com/private/x", options))
    assertTrue(UrlMatch.testScript("https://a.com/public/x", options))
  }

  @Test
  fun `script accepts when any match rule matches`() {
    val options = options(match = listOf("https://b.com/*", "https://a.com/*"))

    assertTrue(UrlMatch.testScript("https://a.com/x", options))
    assertFalse(UrlMatch.testScript("https://c.com/x", options))
  }

  @Test
  fun `script accepts when match and include are combined`() {
    val options =
      options(match = listOf("https://a.com/*"), include = listOf("https://b.com/*"))

    assertTrue(UrlMatch.testScript("https://a.com/x", options))
    assertTrue(UrlMatch.testScript("https://b.com/x", options))
    assertFalse(UrlMatch.testScript("https://c.com/x", options))
  }

  @Test
  fun `script accepts when any include rule matches`() {
    val options = options(include = listOf("https://*.example.com/*"))

    assertTrue(UrlMatch.testScript("https://www.example.com/x", options))
    assertFalse(UrlMatch.testScript("https://example.org/x", options))
  }

  @Test
  fun `script supports regex include`() {
    val options = options(include = listOf("""/^https:\/\/a\.com\//"""))

    assertTrue(UrlMatch.testScript("https://a.com/x", options))
    assertFalse(UrlMatch.testScript("https://b.com/x", options))
  }

  @Test
  fun `script without match or include does not run`() {
    assertFalse(UrlMatch.testScript("https://a.com/x", options()))
  }

  @Test
  fun `script exclude without a wildcard only excludes the exact url`() {
    val options =
      options(include = listOf("https://a.com/*"), exclude = listOf("https://a.com/private"))

    assertFalse(UrlMatch.testScript("https://a.com/private", options))
    assertTrue(UrlMatch.testScript("https://a.com/private/x", options))
  }
}

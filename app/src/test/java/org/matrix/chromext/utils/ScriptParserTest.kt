package org.matrix.chromext.utils

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.matrix.chromext.db.entity.Script

class ScriptParserTest {

  /** 生成元数据块，结尾为 `// ==/UserScript==`，不带尾换行。 */
  private fun block(vararg directives: String): String = buildString {
    append("// ==UserScript==\n")
    directives.forEach {
      append("// ")
      append(it)
      append("\n")
    }
    append("// ==/UserScript==")
  }

  /** 生成元数据块，使用 CRLF 换行。 */
  private fun blockCrlf(vararg directives: String): String = buildString {
    append("// ==UserScript==\r\n")
    directives.forEach {
      append("// ")
      append(it)
      append("\r\n")
    }
    append("// ==/UserScript==")
  }

  // ---------------------------------------------------------------- parseMetadata

  @Test
  fun `parseMetadata collects multi value directives in source order`() {
    val metadata =
      ScriptParser.parseMetadata(
        block(
          "@name Demo",
          "@namespace https://example.com/",
          "@match https://a.example/*",
          "@include https://b.example/*",
          "@exclude https://a.example/private/*",
          "@grant GM_getValue",
          "@require https://cdn.example/lib.js",
          "@connect api.example.com",
          "@match https://c.example/*",
          "@grant GM_setValue",
          "@exclude https://a.example/secret/*",
          "@include https://d.example/*",
          "@require https://cdn.example/lib2.js",
          "@connect cdn.example.com",
        ))

    assertEquals(listOf("https://a.example/*", "https://c.example/*"), metadata.match)
    assertEquals(listOf("https://b.example/*", "https://d.example/*"), metadata.include)
    assertEquals(
      listOf("https://a.example/private/*", "https://a.example/secret/*"),
      metadata.exclude,
    )
    assertEquals(listOf("GM_getValue", "GM_setValue"), metadata.grant)
    assertEquals(
      listOf("https://cdn.example/lib.js", "https://cdn.example/lib2.js"),
      metadata.require,
    )
    assertEquals(listOf("api.example.com", "cdn.example.com"), metadata.connect)
  }

  @Test
  fun `parseMetadata trims the edges of a value but keeps inner spaces`() {
    val metadata =
      ScriptParser.parseMetadata(
        "// ==UserScript==\n// @description   保持 中间   的空格   \n// ==/UserScript==\n")

    assertEquals("保持 中间   的空格", metadata.description)
  }

  @Test
  fun `parseMetadata accepts a directive without a space after the slashes`() {
    val metadata = ScriptParser.parseMetadata("// ==UserScript==\n//@match https://a.example/*\n// ==/UserScript==\n")

    assertEquals(listOf("https://a.example/*"), metadata.match)
  }

  @Test
  fun `parseMetadata accepts tabs as separators`() {
    val metadata = ScriptParser.parseMetadata("// ==UserScript==\n//\t@match\thttps://a.example/*\n// ==/UserScript==\n")

    assertEquals(listOf("https://a.example/*"), metadata.match)
  }

  @Test
  fun `parseMetadata ignores markers, plain comments and blank lines`() {
    val metadata =
      ScriptParser.parseMetadata(
        "// ==UserScript==\n// 这里只是普通注释\n\n// @name Demo\n//\n// ==/UserScript==\n")

    assertEquals("Demo", metadata.name)
    assertEquals(listOf("name" to "Demo"), metadata.raw)
  }

  /**
   * 没有 UserScript 块时整体不解析，避免块外注释被当成元数据
   */
  @Test
  fun `parseMetadata ignores lines that are not line comments`() {
    val withoutBlock = ScriptParser.parseMetadata("// @name Demo\n@match https://a.example/*\n")

    assertNull(withoutBlock.name)
    assertTrue(withoutBlock.match.isEmpty())
  }

  @Test
  fun `parseMetadata keeps unknown directives readable`() {
    val metadata = ScriptParser.parseMetadata(block("@name Demo", "@tag tools"))

    assertEquals("tools", metadata.getMetadata("tag"))
    assertTrue(metadata.hasMetadata("tag"))
  }

  @Test
  fun `parseMetadata treats a directive without a value as present and valueless`() {
    val metadata = ScriptParser.parseMetadata(block("@name Demo", "@noframes"))

    assertTrue(metadata.hasMetadata("noframes"))
    assertTrue(metadata.noframes)
    assertTrue(metadata.raw.contains("noframes" to null))
    assertTrue(metadata.match.isEmpty())
  }

  @Test
  fun `parseMetadata returns null for absent metadata`() {
    val metadata = ScriptParser.parseMetadata(block("@name Demo"))

    assertNull(metadata.namespace)
    assertNull(metadata.getMetadata("grant"))
    assertFalse(metadata.hasMetadata("noframes"))
  }

  @Test
  fun `getAllMetadata returns every value and skips valueless entries`() {
    val metadata = ScriptParser.parseMetadata(block("@grant GM_getValue", "@grant", "@grant GM_setValue"))

    assertEquals(listOf("GM_getValue", "GM_setValue"), metadata.getAllMetadata("grant"))
    assertEquals(listOf("GM_getValue", "GM_setValue"), metadata.grant)
    assertEquals(emptyList<String>(), metadata.getAllMetadata("missing"))
  }

  @Test
  fun `parseMetadata maps every supported run-at value`() {
    assertEquals(
      Script.RunAt.DOCUMENT_START,
      ScriptParser.parseMetadata(block("@run-at document-start")).`run-at`,
    )
    assertEquals(
      Script.RunAt.DOCUMENT_END,
      ScriptParser.parseMetadata(block("@run-at document-end")).`run-at`,
    )
    assertEquals(
      Script.RunAt.DOCUMENT_IDLE,
      ScriptParser.parseMetadata(block("@run-at document-idle")).`run-at`,
    )
  }

  @Test
  fun `parseMetadata treats unknown or absent run-at as unset`() {
    assertEquals(
      Script.RunAt.UNSET,
      ScriptParser.parseMetadata(block("@run-at bogus")).`run-at`,
    )
    assertEquals(Script.RunAt.UNSET, ScriptParser.parseMetadata(block("@name Demo")).`run-at`)
  }

  @Test
  fun `id is namespace and name joined by a colon`() {
    val metadata =
      ScriptParser.parseMetadata(block("@name Demo", "@namespace https://example.com/"))

    assertEquals("https://example.com/:Demo", metadata.id)
  }

  @Test
  fun `getLocaleMetadata prefers the exact language and country`() {
    val metadata =
      ScriptParser.parseMetadata(
        block("@name Base", "@name:zh 中文", "@name:zh-CN 简体中文"))

    assertEquals("简体中文", metadata.getLocaleMetadata("name", Locale.forLanguageTag("zh-CN")))
  }

  @Test
  fun `getLocaleMetadata falls back to the language alone`() {
    val metadata =
      ScriptParser.parseMetadata(block("@name Base", "@name:zh 中文", "@name:de Deutsch"))

    assertEquals("中文", metadata.getLocaleMetadata("name", Locale.forLanguageTag("zh-TW")))
  }

  @Test
  fun `getLocaleMetadata falls back to the plain key`() {
    val metadata = ScriptParser.parseMetadata(block("@name Base", "@name:de Deutsch"))

    assertEquals("Base", metadata.getLocaleMetadata("name", Locale.forLanguageTag("zh-CN")))
    assertNull(metadata.getLocaleMetadata("description", Locale.forLanguageTag("zh-CN")))
  }

  @Test
  fun `parseMetadata handles a CRLF metadata block`() {
    val metadata = ScriptParser.parseMetadata(blockCrlf("@name Demo", "@match https://a.example/*"))

    assertEquals("Demo", metadata.name)
    assertEquals(listOf("https://a.example/*"), metadata.match)
  }

  @Test
  fun `parseMetadata returns empty metadata for empty input`() {
    for (input in listOf("", "// ==UserScript==\n// ==/UserScript==\n")) {
      val metadata = ScriptParser.parseMetadata(input)
      assertEquals(emptyList<String>(), metadata.match)
      assertEquals(emptyList<Pair<String, String?>>(), metadata.raw)
      assertNull(metadata.name)
    }
  }

  // ---------------------------------------------------------------- splitScript

  @Test
  fun `splitScript returns null when there is no metadata block`() {
    assertNull(ScriptParser.splitScript("console.log('no metadata');\n"))
    assertNull(ScriptParser.splitScript("// ==UserScript==\n// @name Demo\n"))
    assertNull(ScriptParser.splitScript(""))
  }

  @Test
  fun `splitScript separates the metadata block from the code`() {
    val code = "console.log('hi');\n"
    val input = block("@name Demo", "@match https://a.example/*") + "\n" + code

    val (meta, parsedCode) = ScriptParser.splitScript(input)!!

    assertTrue(meta!!.startsWith("// ==UserScript=="))
    assertTrue(meta.endsWith("// ==/UserScript=="))
    assertFalse(meta.contains("console.log"))
    assertEquals(code, parsedCode)
  }

  @Test
  fun `splitScript accepts a metadata only script`() {
    val input = block("@name Demo") + "\n"

    val (meta, parsedCode) = ScriptParser.splitScript(input)!!

    assertTrue(meta!!.startsWith("// ==UserScript=="))
    assertEquals("", parsedCode)
  }

  @Test
  fun `splitScript accepts a script that ends at the closing marker`() {
    val input = block("@name Demo", "@match https://a.example/*")

    val split = ScriptParser.splitScript(input)
    assertNotNull(split)
    val (meta, parsedCode) = split!!

    assertEquals("Demo", ScriptParser.parseMetadata(meta!!).name)
    assertEquals("", parsedCode)
  }

  @Test
  fun `splitScript accepts an empty metadata block`() {
    val input = "// ==UserScript==\n// ==/UserScript==\nconsole.log('hi');\n"

    val split = ScriptParser.splitScript(input)
    assertNotNull(split)
    val (meta, parsedCode) = split!!

    assertTrue(ScriptParser.parseMetadata(meta!!).match.isEmpty())
    assertEquals("console.log('hi');\n", parsedCode)
  }

  @Test
  fun `splitScript preserves leading blank lines and indentation of the code`() {
    val code = "\n\n  var a = 1;\n  var b = 2;\n"
    val input = block("@name Demo") + "\n" + code

    val (meta, parsedCode) = ScriptParser.splitScript(input)!!

    assertTrue(meta!!.endsWith("// ==/UserScript=="))
    assertEquals(code, parsedCode)
  }

  @Test
  fun `splitScript preserves CRLF code verbatim`() {
    val code = "\r\n\r\n  var a = 1;\r\n  var b = 2;\r\n"
    val input = blockCrlf("@name Demo", "@match https://a.example/*") + code

    val (meta, parsedCode) = ScriptParser.splitScript(input)!!

    assertEquals("Demo", ScriptParser.parseMetadata(meta!!).name)
    assertEquals(code, parsedCode)
  }

  @Test
  fun `splitScript accepts a BOM and blank lines before the block`() {
    val input = "\uFEFF\n" + block("@name Demo") + "\nconsole.log('hi');\n"

    val metadata = ScriptParser.parseScriptMetadata(input)

    assertNotNull(metadata)
    assertEquals("Demo", metadata!!.name)
  }

  @Test
  fun `splitScript accepts a BOM glued to the opening marker`() {
    val input = "\uFEFF" + block("@name Demo", "@match https://a.example/*") + "\nconsole.log('hi');\n"

    val metadata = ScriptParser.parseScriptMetadata(input)

    assertNotNull(metadata)
    assertEquals("Demo", metadata!!.name)
    assertEquals(listOf("https://a.example/*"), metadata.match)
  }

  @Test
  fun `splitScript returns only the metadata block and drops the preamble`() {
    val preamble = "\uFEFF\n\n// 许可证头\n// 与元数据无关\n"
    val body = block("@name Demo", "@match https://a.example/*")
    val code = "console.log('hi');\n"
    val input = preamble + body + "\n" + code

    val (meta, parsedCode) = ScriptParser.splitScript(input)!!

    assertEquals(body, meta)
    assertEquals(code, parsedCode)
  }

  @Test
  fun `splitScript tolerates trailing spaces on the opening marker`() {
    val input = "// ==UserScript==  \n// @name Demo\n// ==/UserScript==\nconsole.log('hi');\n"

    val metadata = ScriptParser.parseScriptMetadata(input)

    assertNotNull(metadata)
    assertEquals("Demo", metadata!!.name)
  }

  // ---------------------------------------------------------------- parseScriptMetadata

  @Test
  fun `parseScriptMetadata parses a complete userscript`() {
    val input =
      block(
        "@name Example",
        "@namespace https://example.com/",
        "@version 1.2.3",
        "@description Demo",
        "@author Alice",
        "@match https://example.com/*",
        "@include https://example.org/*",
        "@exclude https://example.com/private/*",
        "@grant GM_getValue",
        "@grant GM_setValue",
        "@connect api.example.com",
        "@run-at document-end",
        "@noframes",
      ) + "\n\nconsole.log('hi');\n"

    val metadata = ScriptParser.parseScriptMetadata(input)!!

    assertEquals("Example", metadata.name)
    assertEquals("https://example.com/", metadata.namespace)
    assertEquals("1.2.3", metadata.version)
    assertEquals("Demo", metadata.description)
    assertEquals("Alice", metadata.author)
    assertEquals("https://example.com/:Example", metadata.id)
    assertEquals(listOf("https://example.com/*"), metadata.match)
    assertEquals(listOf("https://example.org/*"), metadata.include)
    assertEquals(listOf("https://example.com/private/*"), metadata.exclude)
    assertEquals(listOf("GM_getValue", "GM_setValue"), metadata.grant)
    assertEquals(listOf("api.example.com"), metadata.connect)
    assertEquals(Script.RunAt.DOCUMENT_END, metadata.`run-at`)
    assertTrue(metadata.noframes)
  }

  @Test
  fun `parseScriptMetadata returns null when there is no metadata block`() {
    assertNull(ScriptParser.parseScriptMetadata("console.log('no metadata');\n"))
  }

  @Test
  fun `parseScriptMetadata accepts a script that ends at the closing marker`() {
    val metadata = ScriptParser.parseScriptMetadata(block("@name Demo"))

    assertNotNull(metadata)
    assertEquals("Demo", metadata!!.name)
  }

  @Test
  fun `parseScriptMetadata ignores directives that only appear in the code`() {
    val input = block("@name Real") + "\n// @name Injected\nconsole.log('hi');\n"

    val metadata = ScriptParser.parseScriptMetadata(input)!!

    assertEquals("Real", metadata.name)
  }

  @Test
  fun `parseScriptMetadata ignores a decoy metadata block inside the code`() {
    val input =
      block("@name Real") +
        "\n" +
        block("@name Injected", "@grant GM_setValue") +
        "\nconsole.log('hi');\n"

    val metadata = ScriptParser.parseScriptMetadata(input)!!

    assertEquals("Real", metadata.name)
    assertTrue(metadata.grant.isEmpty())
  }

  @Test
  fun `parseScriptMetadata ignores directives outside the metadata block`() {
    val input =
      "// @name Wrong\n" +
        "// @namespace https://wrong.example/\n" +
        "// @match https://wrong.example/*\n" +
        "// @grant GM_setValue\n" +
        "// @run-at document-start\n" +
        "// @noframes\n" +
        block(
          "@name Right",
          "@namespace https://right.example/",
          "@match https://right.example/*",
        ) +
        "\nconsole.log('hi');\n"

    val metadata = ScriptParser.parseScriptMetadata(input)!!

    assertEquals("Right", metadata.name)
    assertEquals("https://right.example/:Right", metadata.id)
    assertEquals(listOf("https://right.example/*"), metadata.match)
    assertTrue(metadata.grant.isEmpty())
    assertEquals(Script.RunAt.UNSET, metadata.`run-at`)
    assertFalse(metadata.noframes)
  }

  @Test
  fun `parseMetadata of the split block sees only in-block directives`() {
    val input = "// @name Wrong\n" + block("@name Right", "@grant GM_getValue") + "\nconsole.log(1);\n"

    val (meta, _) = ScriptParser.splitScript(input)!!
    val metadata = ScriptParser.parseMetadata(meta!!)

    assertEquals("Right", metadata.name)
    assertEquals(listOf("GM_getValue"), metadata.grant)
  }
}

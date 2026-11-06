package dev.leosnczki.sonora

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.assertTrue
import org.junit.Test

/** Every user-visible English string must have a Portuguese (pt-BR) translation, in every module. */
class TranslationsTest {
    private val root = File(System.getProperty("sonora.rootDir") ?: "..")

    private fun keys(file: File): Set<String> {
        if (!file.exists()) return emptySet()
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val nodes = doc.getElementsByTagName("string")
        return (0 until nodes.length)
            .map { nodes.item(it).attributes }
            .filter { it.getNamedItem("translatable")?.nodeValue != "false" }
            .map { it.getNamedItem("name").nodeValue }
            .toSet()
    }

    @Test
    fun every_english_string_has_a_portuguese_translation() {
        val english = root.walkTopDown()
            .onEnter { it.name != "build" && !it.name.startsWith(".") }
            .filter { it.name == "strings.xml" && it.parentFile.name == "values" && "src" in it.path }
            .toList()
        assertTrue(english.isNotEmpty(), "no strings.xml found under $root")

        val missing = english.flatMap { file ->
            val pt = File(file.parentFile.parentFile, "values-pt-rBR/strings.xml")
            (keys(file) - keys(pt)).map { "${file.relativeTo(root)}: $it" }
        }
        assertTrue(missing.isEmpty(), "missing pt-BR translations:\n" + missing.joinToString("\n"))
    }
}

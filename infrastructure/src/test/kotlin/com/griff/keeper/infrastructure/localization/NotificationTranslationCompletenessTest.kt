package com.griff.keeper.infrastructure.localization

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.w3c.dom.Element

/**
 * Notification copy is translated in both languages, deliberately.
 *
 * The same guarantee the presentation module has, applied to the strings a reminder is built from.
 * These are the easiest strings in the app to forget: nothing on screen shows them, so an
 * untranslated one only surfaces in someone's notification drawer.
 */
class NotificationTranslationCompletenessTest {

    private val english = read("values")
    private val polish = read("values-pl")

    @Test
    fun `polish translates every notification string`() {
        val missing = english.keys - polish.keys
        assertTrue(missing.isEmpty(), "Not translated into Polish: ${missing.sorted()}")
    }

    @Test
    fun `polish adds no notification string english does not have`() {
        val extra = polish.keys - english.keys
        assertTrue(extra.isEmpty(), "Only in values-pl: ${extra.sorted()}")
    }

    @Test
    fun `english notification plurals cover the english quantities`() {
        pluralQuantities(english).forEach { (name, quantities) ->
            assertEquals(setOf("one", "other"), quantities, "values/$name")
        }
    }

    @Test
    fun `polish notification plurals cover the four polish quantities`() {
        // "Odnowienie za 1 dzień", "za 2 dni", "za 5 dni" - `one` and `other` are not enough.
        pluralQuantities(polish).forEach { (name, quantities) ->
            assertEquals(setOf("one", "few", "many", "other"), quantities, "values-pl/$name")
        }
    }

    @Test
    fun `a translation takes the same format arguments as the base string`() {
        english.forEach { (name, entry) ->
            assertEquals(
                entry.placeholders,
                polish.getValue(name).placeholders,
                "Format arguments differ for $name",
            )
        }
    }

    @Test
    fun `there is something to check`() {
        assertTrue(english.size >= 15, "Only ${english.size} resources found")
        assertTrue(pluralQuantities(english).size >= 3, "Suspiciously few plurals")
    }

    private fun pluralQuantities(entries: Map<String, Entry>): Map<String, Set<String>> =
        entries.filterValues { it.isPlural }.mapValues { it.value.quantities }

    private data class Entry(
        val isPlural: Boolean,
        val quantities: Set<String>,
        val placeholders: Set<String>,
    )

    /** Read from the module directory, which is the working directory of a Gradle `Test` task. */
    private fun read(resourceDir: String): Map<String, Entry> {
        val file = File("src/main/res/$resourceDir/strings.xml")
        require(file.isFile) { "Missing ${file.absolutePath}" }

        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val children = document.documentElement.childNodes
        val entries = mutableMapOf<String, Entry>()
        for (index in 0 until children.length) {
            val element = children.item(index) as? Element ?: continue
            if (element.tagName != "string" && element.tagName != "plurals") continue
            if (element.getAttribute("translatable") == "false") continue
            val items = (0 until element.childNodes.length)
                .mapNotNull { element.childNodes.item(it) as? Element }
            entries[element.getAttribute("name")] = Entry(
                isPlural = element.tagName == "plurals",
                quantities = items.map { it.getAttribute("quantity") }.filter { it.isNotEmpty() }.toSet(),
                placeholders = PLACEHOLDER.findAll(element.textContent).map { it.value }.toSet(),
            )
        }
        return entries
    }

    private companion object {
        val PLACEHOLDER = Regex("""%\d+\$[a-zA-Z]""")
    }
}

package com.griff.keeper.presentation.localization

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

/**
 * Reads a `strings.xml` the way aapt does, so a test can compare two locales without a device.
 *
 * Android's own fallback means a missing Polish string silently shows the English one, and nothing
 * fails. That is the right *runtime* behaviour and the wrong development one: the app is shipped in
 * two languages, and a string nobody translated should be a build failure rather than a surprise on
 * someone's phone. Parsing the files is enough to prove it, and needs no emulator.
 */
internal object StringResources {

    /** A resource as far as translation parity is concerned. */
    data class Entry(
        val name: String,
        val kind: Kind,
        /** Empty for anything that is not a `<plurals>`. */
        val quantities: Set<String>,
        val itemCount: Int,
        /** The format arguments the text uses, e.g. `%1${'$'}s`. */
        val placeholders: Set<String>,
    )

    enum class Kind { STRING, PLURALS, ARRAY }

    /**
     * Resource files are read from the module directory, which is the working directory Gradle runs
     * a `Test` task in.
     */
    fun read(resourceDir: String): Map<String, Entry> {
        val file = File("src/main/res/$resourceDir/strings.xml")
        require(file.isFile) { "Missing ${file.absolutePath}" }

        val document = DocumentBuilderFactory.newInstance()
            .also { it.isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(file)

        val entries = mutableMapOf<String, Entry>()
        val root = document.documentElement
        val children = root.childNodes
        for (index in 0 until children.length) {
            val element = children.item(index) as? Element ?: continue
            // Brand names, format patterns and other resources marked untranslatable live in the
            // base locale only, by design, so they are not part of the comparison.
            if (element.getAttribute("translatable") == "false") continue

            val kind = when (element.tagName) {
                "string" -> Kind.STRING
                "plurals" -> Kind.PLURALS
                "string-array" -> Kind.ARRAY
                else -> continue
            }
            val items = element.childElements("item")
            entries[element.getAttribute("name")] = Entry(
                name = element.getAttribute("name"),
                kind = kind,
                quantities = if (kind == Kind.PLURALS) {
                    items.map { it.getAttribute("quantity") }.toSet()
                } else {
                    emptySet()
                },
                itemCount = items.size,
                placeholders = placeholdersIn(element),
            )
        }
        return entries
    }

    /**
     * A translation has to take the same arguments as the base string: the code passes them
     * positionally, so a `%1${'$'}s` that became a `%2${'$'}s` is a crash rather than a typo.
     */
    private fun placeholdersIn(element: Element): Set<String> =
        PLACEHOLDER.findAll(element.textContent).map { it.value }.toSet()

    private val PLACEHOLDER = Regex("""%\d+\${'$'}[a-zA-Z]""")

    private fun Element.childElements(tag: String): List<Element> =
        (0 until childNodes.length)
            .mapNotNull { childNodes.item(it) as? Element }
            .filter { it.tagName == tag }
}

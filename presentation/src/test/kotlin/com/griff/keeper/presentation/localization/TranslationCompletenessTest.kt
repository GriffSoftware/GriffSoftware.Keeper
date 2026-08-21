package com.griff.keeper.presentation.localization

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Every user facing resource exists in both languages, deliberately.
 *
 * Lint's `MissingTranslation` already fails the build on an untranslated string; this covers what it
 * does not. A `<plurals>` with only `one` and `other` compiles and passes lint, and then reads
 * correctly in English and wrongly in Polish, which needs `few` and `many` as well - the kind of bug
 * that only appears once someone has five days left. And a translation whose format arguments drift
 * from the base string's is not a typo but a crash, because the code passes them positionally.
 */
class TranslationCompletenessTest {

    private val english = StringResources.read("values")
    private val polish = StringResources.read("values-pl")

    /** The quantities CLDR defines for each language; anything less is a wrong plural form. */
    private val englishQuantities = setOf("one", "other")
    private val polishQuantities = setOf("one", "few", "many", "other")

    @Test
    fun `polish translates every english resource`() {
        val missing = english.keys - polish.keys
        assertTrue(missing.isEmpty(), "Not translated into Polish: ${missing.sorted()}")
    }

    @Test
    fun `polish adds no resource english does not have`() {
        // An orphan translation is a resource the code cannot reach, or a base string someone
        // deleted and forgot to remove here.
        val extra = polish.keys - english.keys
        assertTrue(extra.isEmpty(), "Only in values-pl: ${extra.sorted()}")
    }

    @Test
    fun `a resource keeps its type in both languages`() {
        english.forEach { (name, entry) ->
            assertEquals(entry.kind, polish.getValue(name).kind, "Different resource type: $name")
        }
    }

    @Test
    fun `english plurals cover the english quantities`() {
        english.values
            .filter { it.kind == StringResources.Kind.PLURALS }
            .forEach { plural ->
                assertEquals(
                    englishQuantities,
                    plural.quantities,
                    "values/${plural.name} has the wrong quantities",
                )
            }
    }

    @Test
    fun `polish plurals cover the four polish quantities`() {
        // "1 dzień", "2 dni", "5 dni" - Polish needs `one`, `few` and `many`, and English rules are
        // nowhere near enough.
        polish.values
            .filter { it.kind == StringResources.Kind.PLURALS }
            .forEach { plural ->
                assertEquals(
                    polishQuantities,
                    plural.quantities,
                    "values-pl/${plural.name} has the wrong quantities",
                )
            }
    }

    @Test
    fun `string arrays have the same number of items`() {
        english.values
            .filter { it.kind == StringResources.Kind.ARRAY }
            .forEach { array ->
                assertEquals(
                    array.itemCount,
                    polish.getValue(array.name).itemCount,
                    "values-pl/${array.name} has a different number of items",
                )
            }
    }

    @Test
    fun `a translation takes the same format arguments as the base string`() {
        english.forEach { (name, entry) ->
            assertEquals(
                entry.placeholders,
                polish.getValue(name).placeholders,
                "Format arguments differ between values/ and values-pl/ for $name",
            )
        }
    }

    @Test
    fun `there is something to check`() {
        // Guards against the parser silently reading nothing and every assertion above passing.
        assertTrue(english.size > 300, "Only ${english.size} resources found")
        assertTrue(
            english.values.count { it.kind == StringResources.Kind.PLURALS } >= 10,
            "Suspiciously few plurals",
        )
    }
}

package com.github.youjiyun1123.youregexinterpreter.template

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.regex.PatternSyntaxException

/**
 * Regression tests for bundled template JSON files.
 *
 * Background: a single JSON typo (e.g. an unescaped regex `\s` written as `\s`
 * instead of `\\s` inside a JSON string) makes `kotlinx.serialization` decode the
 * whole file successfully, but the resulting pattern then explodes at
 * `Pattern.compile` time. Because `TemplateRegistry` was silently swallowing
 * that exception, the entire category disappeared from the UI with no signal.
 *
 * These tests fail loudly if a similar regression is ever reintroduced.
 */
class TemplateRegistryTest {

    @Test
    @DisplayName("Each bundled template category file must load at least one template")
    fun everyCategoryFileLoadsAtLeastOneTemplate() {
        val registry = TemplateRegistry()

        for (category in TemplateCategory.entries) {
            val templates = registry.getByCategory(category)
            assertThat(templates)
                .withFailMessage(
                    "Category %s has zero templates loaded. " +
                        "Check that src/main/resources/templates/%s.json exists and is valid JSON, " +
                        "and that every pattern compiles.",
                    category, category.name.lowercase()
                )
                .isNotEmpty
        }
    }

    @Test
    @DisplayName("Every loaded template pattern must be a valid Java regex")
    fun everyLoadedPatternIsValidJavaRegex() {
        val registry = TemplateRegistry()
        val all = registry.getAll()

        assertThat(all)
            .withFailMessage("TemplateRegistry returned no templates at all")
            .isNotEmpty

        for (tpl in all) {
            try {
                java.util.regex.Pattern.compile(tpl.pattern)
            } catch (e: PatternSyntaxException) {
                org.junit.jupiter.api.Assertions.fail<String>(
                    "Template id='${tpl.id}' category='${tpl.category}' has invalid pattern: " +
                        "${tpl.pattern} (cause: ${e.description} at index ${e.index})"
                )
            }
        }
    }

    @Test
    @DisplayName("Total loaded template count must match the number of JSON entries across all category files")
    fun totalCountMatchesAllJsonEntries() {
        val classLoader = TemplateRegistry::class.java.classLoader
        var expected = 0

        for (category in TemplateCategory.entries) {
            val fileName = "${category.name.lowercase()}.json"
            val stream: java.io.InputStream = classLoader.getResourceAsStream("templates/$fileName")
                ?: error("Missing bundled resource: templates/$fileName")
            stream.use { input ->
                val text = input.reader(Charsets.UTF_8).readText()
                val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                val entries = json.decodeFromString<List<RegexTemplateDto>>(text)
                expected += entries.size
            }
        }

        assertThat(TemplateRegistry().getTemplateCount())
            .withFailMessage("TemplateRegistry count mismatch: registry returned %d but JSON files contain %d",
                TemplateRegistry().getTemplateCount(), expected)
            .isEqualTo(expected)
    }
}

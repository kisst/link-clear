package app.linkclear.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * End-to-end cases against the REAL bundled ClearURLs ruleset (not a synthetic
 * one). Guards against regressions when the shipped ruleset is updated: if a
 * future ruleset stops stripping a common tracker — or starts eating a
 * meaningful param like `v`, `id`, or `q` — one of these breaks.
 */
class BundledRulesGoldenTest {
    private val engine = CleaningEngine(BundledRules.load())

    private data class Case(val dirty: String, val clean: String)

    private val cases =
        listOf(
            // Amazon: ref/keywords/qid/sr affiliate + search-context params stripped.
            Case(
                "https://www.amazon.com/dp/B08N5WRWNW/ref=sr_1_1?keywords=x&qid=123&sr=8-1",
                "https://www.amazon.com/dp/B08N5WRWNW",
            ),
            // YouTube: feature + utm_source stripped, the video id `v` preserved.
            Case(
                "https://www.youtube.com/watch?v=dQw4w9WgXcQ&feature=share&utm_source=nl",
                "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            ),
            // Generic UTM campaign params stripped, real `id` preserved.
            Case(
                "https://example.com/article?utm_source=newsletter&utm_medium=email&id=42",
                "https://example.com/article?id=42",
            ),
            // Google search: utm stripped, the query `q` preserved.
            Case(
                "https://www.google.com/search?q=test&utm_source=x",
                "https://www.google.com/search?q=test",
            ),
            // Click-id trackers (fbclid, gclid) fully stripped.
            Case(
                "https://shop.example.com/p?fbclid=abc123&gclid=xyz",
                "https://shop.example.com/p",
            ),
        )

    @Test fun `bundled ruleset cleans real-world urls`() {
        for (c in cases) {
            assertEquals(c.clean, engine.cleanUrl(c.dirty).cleaned, "dirty: ${c.dirty}")
        }
    }
}

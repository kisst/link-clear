package app.linkclear.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BundledRulesTest {
    private val engine = CleaningEngine(BundledRules.load())

    @Test fun `bundled ruleset is non-empty`() {
        assertTrue(BundledRules.load().providers.size > 50)
    }

    @Test fun `strips common trackers using real rules`() {
        assertEquals(
            "https://example.com/product",
            engine.cleanUrl("https://example.com/product?utm_source=news&utm_medium=email").cleaned,
        )
        assertEquals(
            "https://example.com/x",
            engine.cleanUrl("https://example.com/x?fbclid=IwAR0abc").cleaned,
        )
    }
}

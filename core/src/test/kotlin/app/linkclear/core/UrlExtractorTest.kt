package app.linkclear.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UrlExtractorTest {
    @Test fun `bare url`() {
        assertEquals(listOf("https://a.com/x"), UrlExtractor.extract("https://a.com/x"))
    }

    @Test fun `url embedded in shared message`() {
        val text = "Check this out https://shop.com/p?utm_source=x cool right"
        assertEquals(listOf("https://shop.com/p?utm_source=x"), UrlExtractor.extract(text))
    }

    @Test fun `trailing sentence punctuation trimmed`() {
        assertEquals(listOf("https://a.com/x"), UrlExtractor.extract("see https://a.com/x."))
    }

    @Test fun `multiple urls in order`() {
        val text = "one http://a.com two https://b.com/y done"
        assertEquals(listOf("http://a.com", "https://b.com/y"), UrlExtractor.extract(text))
    }

    @Test fun `no url yields empty`() {
        assertTrue(UrlExtractor.extract("just some text").isEmpty())
    }

    @Test fun `balanced trailing paren kept`() {
        assertEquals(
            listOf("https://en.wikipedia.org/wiki/Foo_(bar)"),
            UrlExtractor.extract("https://en.wikipedia.org/wiki/Foo_(bar)"),
        )
    }

    @Test fun `unbalanced trailing paren trimmed`() {
        assertEquals(listOf("https://a.com/x"), UrlExtractor.extract("(see https://a.com/x)"))
    }

    @Test fun `plain sentence punctuation still trimmed`() {
        assertEquals(listOf("https://a.com/x"), UrlExtractor.extract("go to https://a.com/x."))
    }

    @Test fun `balanced trailing bracket kept`() {
        assertEquals(listOf("https://a.com/a[0]"), UrlExtractor.extract("https://a.com/a[0]"))
    }
}

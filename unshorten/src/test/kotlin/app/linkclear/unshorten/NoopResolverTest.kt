package app.linkclear.unshorten

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NoopResolverTest {
    @Test fun `returns input unchanged`() {
        assertEquals("https://short.ly/abc", NoopResolver.resolve("https://short.ly/abc"))
    }
}

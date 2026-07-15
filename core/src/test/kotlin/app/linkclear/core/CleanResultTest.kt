package app.linkclear.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class CleanResultTest {
    @Test fun `removedCount reflects removed list`() {
        val r =
            CleanResult(
                original = "http://x/?a=1&b=2",
                cleaned = "http://x/",
                removed = listOf(RemovedParam("a", "1"), RemovedParam("b", "2")),
                wasChanged = true,
            )
        assertEquals(2, r.removedCount)
    }

    @Test fun `unchanged result defaults`() {
        val r = CleanResult("http://x/", "http://x/", emptyList(), false)
        assertFalse(r.wasChanged)
        assertFalse(r.unshortened)
        assertEquals(0, r.removedCount)
    }
}

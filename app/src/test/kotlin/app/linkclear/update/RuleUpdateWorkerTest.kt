package app.linkclear.update

import app.linkclear.core.Provider
import app.linkclear.core.RuleSet
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException
import java.net.HttpURLConnection

class RuleUpdateWorkerTest {
    private val server = MockWebServer()

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    private fun connectionTo(server: MockWebServer): HttpURLConnection =
        server.url("/").toUrl().openConnection() as HttpURLConnection

    private fun provider(name: String) =
        Provider(
            name = name,
            urlPattern = ".*",
            completeProvider = false,
            rules = emptyList(),
            rawRules = emptyList(),
            referralMarketing = emptyList(),
            exceptions = emptyList(),
            redirections = emptyList(),
        )

    private fun ruleSetOf(
        count: Int,
        includeRequired: Boolean = true,
    ): RuleSet {
        val required = if (includeRequired) RuleUpdateWorker.REQUIRED_PROVIDERS.toList() else emptyList()
        val filler = (1..(count - required.size).coerceAtLeast(0)).map { provider("filler$it") }
        return RuleSet(required.map { provider(it) } + filler)
    }

    @Test
    fun `validateRuleset accepts a realistic ruleset`() {
        assertTrue(validateRuleset(ruleSetOf(206)))
    }

    @Test
    fun `validateRuleset rejects too few providers`() {
        assertFalse(validateRuleset(ruleSetOf(RuleUpdateWorker.MIN_PROVIDERS - 1)))
    }

    @Test
    fun `validateRuleset rejects an inflated payload`() {
        assertFalse(validateRuleset(ruleSetOf(RuleUpdateWorker.MAX_PROVIDERS + 1)))
    }

    @Test
    fun `validateRuleset rejects a well-sized ruleset missing known providers`() {
        assertFalse(validateRuleset(ruleSetOf(206, includeRequired = false)))
    }

    @Test
    fun `rejects non-https urls`() {
        assertThrows(IllegalArgumentException::class.java) {
            fetchRuleset("http://example.com/x")
        }
    }

    @Test
    fun `rejects responses over the size cap`() {
        val body = "a".repeat(2048)
        server.enqueue(MockResponse().setBody(body))
        server.start()

        assertThrows(IOException::class.java) {
            readBoundedResponse(connectionTo(server), maxBytes = 1024)
        }
    }

    @Test
    fun `returns body for a small valid response under the cap`() {
        val body = "{\"providers\":{}}"
        server.enqueue(MockResponse().setBody(body))
        server.start()

        val result = readBoundedResponse(connectionTo(server), maxBytes = 4096)
        assertEquals(body, result)
    }

    @Test
    fun `throws on non-2xx response`() {
        server.enqueue(MockResponse().setResponseCode(500))
        server.start()

        assertThrows(IOException::class.java) {
            readBoundedResponse(connectionTo(server))
        }
    }
}

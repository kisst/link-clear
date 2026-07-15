package app.linkclear.unshorten

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * MockWebServer serves plain http, so these tests use the `enforceHttps = false`
 * internal test hook to exercise request/parsing logic against it, plus a
 * dedicated test for the production https-only guard.
 */
class RemoteResolverTest {
    private lateinit var server: MockWebServer

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test fun `returns final_url from JSON response`() {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""{"final_url":"https://real.com/x"}"""),
        )
        val base = server.url("/resolve?url=").toString()
        val resolver = RemoteResolver(base, enforceHttps = false)
        assertEquals("https://real.com/x", resolver.resolve("https://short.ly/abc"))
    }

    @Test fun `returns unshortened_url from JSON response`() {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""{"unshortened_url":"https://real.com/y"}"""),
        )
        val base = server.url("/api/v2/unshorten?url=").toString()
        val resolver = RemoteResolver(base, enforceHttps = false)
        assertEquals("https://real.com/y", resolver.resolve("https://short.ly/abc"))
    }

    @Test fun `returns resolvedURL from JSON response`() {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""{"resolvedURL":"https://real.com/z"}"""),
        )
        val base = server.url("/?shortURL=").toString()
        val resolver = RemoteResolver(base, enforceHttps = false)
        assertEquals("https://real.com/z", resolver.resolve("https://short.ly/abc"))
    }

    @Test fun `body with none of the known fields returns input unchanged`() {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""{"chain":[],"status":"ok"}"""),
        )
        val base = server.url("/resolve?url=").toString()
        val resolver = RemoteResolver(base, enforceHttps = false)
        assertEquals("https://short.ly/abc", resolver.resolve("https://short.ly/abc"))
    }

    @Test fun `matching field with non-URL string returns input unchanged`() {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""{"final_url":"not-a-url"}"""),
        )
        val base = server.url("/resolve?url=").toString()
        val resolver = RemoteResolver(base, enforceHttps = false)
        assertEquals("https://short.ly/abc", resolver.resolve("https://short.ly/abc"))
    }

    @Test fun `non-https base returns input unchanged`() {
        val base = server.url("/resolve?url=").toString() // http, not https
        val resolver = RemoteResolver(base)
        val input = "https://short.ly/abc"
        assertEquals(input, resolver.resolve(input))
    }

    @Test fun `non-2xx response returns input unchanged`() {
        server.enqueue(MockResponse().setResponseCode(500))
        val base = server.url("/resolve?url=").toString()
        val resolver = RemoteResolver(base, enforceHttps = false)
        assertEquals("https://short.ly/abc", resolver.resolve("https://short.ly/abc"))
    }

    @Test fun `malformed JSON returns input unchanged`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("not json"))
        val base = server.url("/resolve?url=").toString()
        val resolver = RemoteResolver(base, enforceHttps = false)
        assertEquals("https://short.ly/abc", resolver.resolve("https://short.ly/abc"))
    }

    @Test fun `missing final_url returns input unchanged`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"chain":[]}"""))
        val base = server.url("/resolve?url=").toString()
        val resolver = RemoteResolver(base, enforceHttps = false)
        assertEquals("https://short.ly/abc", resolver.resolve("https://short.ly/abc"))
    }

    @Test fun `request url is prefix concatenated with encoded target, no extra separator`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"final_url":"https://real.com/x"}"""))
        val base = server.url("/resolve?url=").toString()
        val resolver = RemoteResolver(base, enforceHttps = false)
        resolver.resolve("https://short.ly/abc?x=1")
        val recorded = server.takeRequest()
        val expectedPath = "/resolve?url=" + java.net.URLEncoder.encode("https://short.ly/abc?x=1", "UTF-8")
        assertEquals(expectedPath, recorded.path)
    }
}

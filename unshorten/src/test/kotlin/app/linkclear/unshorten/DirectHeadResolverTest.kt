package app.linkclear.unshorten

import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DirectHeadResolverTest {
    @Test fun `resolves known shortener via Location header`() {
        val server = MockWebServer()
        server.enqueue(
            MockResponse().setResponseCode(301)
                .setHeader("Location", "https://real.com/page?utm_source=x"),
        )
        server.start()
        val host = server.hostName + ":" + server.port
        val resolver =
            DirectHeadResolver(
                client = OkHttpClient(),
                shortenerHosts = setOf(host),
            )
        val out = resolver.resolve(server.url("/abc").toString())
        assertEquals("https://real.com/page?utm_source=x", out)
        server.shutdown()
    }

    @Test fun `non-shortener returned unchanged`() {
        val resolver = DirectHeadResolver(shortenerHosts = setOf("bit.ly"))
        assertEquals("https://normal.com/x", resolver.resolve("https://normal.com/x"))
    }

    private fun resolverAgainst(
        server: MockWebServer,
        location: String,
    ): String {
        server.enqueue(MockResponse().setResponseCode(301).setHeader("Location", location))
        server.start()
        val host = server.hostName + ":" + server.port
        val resolver = DirectHeadResolver(client = OkHttpClient(), shortenerHosts = setOf(host))
        val input = server.url("/abc").toString()
        return resolver.resolve(input).also { server.shutdown() }
    }

    @Test fun `javascript scheme Location is rejected`() {
        // A hostile shortener returning a non-http(s) Location must not escape.
        val out = resolverAgainst(MockWebServer(), "javascript:alert(1)")
        // Falls back to the original request URL, never the javascript: payload.
        assertEquals(false, out.startsWith("javascript:"))
    }

    @Test fun `file scheme Location is rejected`() {
        val server = MockWebServer()
        val out = resolverAgainst(server, "file:///etc/passwd")
        assertEquals(false, out.startsWith("file:"))
    }
}

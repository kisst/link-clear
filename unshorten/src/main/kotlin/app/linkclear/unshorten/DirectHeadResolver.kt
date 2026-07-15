package app.linkclear.unshorten

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

val DEFAULT_SHORTENERS: Set<String> =
    setOf(
        "bit.ly",
        "t.co",
        "tinyurl.com",
        "goo.gl",
        "ow.ly",
        "buff.ly",
        "is.gd",
    )

/**
 * On-device direct resolver: the phone itself issues a HEAD request to the
 * (suspected) shortener host and reads the Location header. NOT off-device —
 * the shortener/tracker sees the user's IP. Kept as the no-backend fallback.
 */
class DirectHeadResolver(
    client: OkHttpClient = OkHttpClient(),
    private val shortenerHosts: Set<String> = DEFAULT_SHORTENERS,
) : Resolver {
    private val client: OkHttpClient =
        client.newBuilder()
            .followRedirects(false)
            .followSslRedirects(false)
            .callTimeout(3, TimeUnit.SECONDS)
            .build()

    override fun resolve(url: String): String {
        val parsed = url.toHttpUrlOrNull() ?: return url
        val host = parsed.host
        val hostWithPort = "${parsed.host}:${parsed.port}"
        if (host !in shortenerHosts && hostWithPort !in shortenerHosts) return url
        return try {
            client.newCall(Request.Builder().url(url).head().build()).execute().use { resp ->
                val location = resp.header("Location")?.takeIf { it.isNotBlank() } ?: return url
                // Resolve the (possibly relative) Location against the request URL.
                // HttpUrl.resolve yields null for non-http(s) schemes, so a hostile
                // "javascript:"/"file:"/other-scheme Location falls back to the input
                // and can never reach an ACTION_VIEW intent downstream.
                parsed.resolve(location)?.toString() ?: url
            }
        } catch (_: Exception) {
            url
        }
    }
}

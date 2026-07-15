package app.linkclear.unshorten

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

private const val MAX_RESPONSE_BYTES = 1L * 1024 * 1024 // 1 MB cap.

/**
 * Known JSON field names, in priority order, that real resolver providers use
 * to carry the resolved target URL. The first present, non-blank, http(s)
 * value wins.
 */
private val FINAL_URL_FIELDS =
    listOf(
        "final_url",
        "unshortened_url",
        "resolvedURL",
        "resolved_url",
        "long_url",
        "longUrl",
        "url",
        "destination",
    )

/**
 * Parses a resolver JSON response body and returns the first top-level string
 * field (checked in [FINAL_URL_FIELDS] order) whose value looks like an
 * http(s) URL. Returns null if the body isn't a JSON object, or none of the
 * known fields hold an http(s) string.
 */
internal fun extractFinalUrl(json: String): String? {
    return try {
        val element = Json.parseToJsonElement(json).jsonObject
        for (field in FINAL_URL_FIELDS) {
            val value =
                element[field]?.let { it as? JsonPrimitive }?.takeIf { it.isString }
                    ?.jsonPrimitive?.content
            if (!value.isNullOrBlank() && (value.startsWith("http://") || value.startsWith("https://"))) {
                return value
            }
        }
        null
    } catch (_: Exception) {
        null
    }
}

/**
 * Off-device resolver: calls a configurable resolver endpoint (self-hosted or
 * otherwise trusted by the user) which fetches the target on the user's
 * behalf, hiding the user's IP from the shortener/tracker.
 *
 * Contract: the stored resolver URL is a full request PREFIX. The request is
 * built as `{resolverPrefix}{URLEncoder.encode(target, "UTF-8")}` — i.e. the
 * url-encoded target is appended directly, with no extra `?url=` inserted.
 * This lets presets whose templates already end in their provider's expected
 * query param (e.g. `...?url=`, `...?shortURL=`) work unmodified.
 *
 * The response JSON is scanned for the first known field name (see
 * [FINAL_URL_FIELDS]) holding an http(s) URL.
 *
 * Never throws: any failure (non-https prefix, network error, bad status,
 * malformed JSON, missing/invalid URL field) returns the input unchanged.
 */
class RemoteResolver internal constructor(
    private val resolverBase: String,
    client: OkHttpClient = OkHttpClient(),
    private val enforceHttps: Boolean = true,
) : Resolver {
    constructor(resolverBase: String, client: OkHttpClient = OkHttpClient()) :
        this(resolverBase, client, enforceHttps = true)

    private val client: OkHttpClient =
        client.newBuilder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()

    override fun resolve(url: String): String {
        if (enforceHttps && !resolverBase.startsWith("https://")) return url
        return try {
            val encoded = URLEncoder.encode(url, "UTF-8")
            val requestUrl = "$resolverBase$encoded"
            client.newCall(Request.Builder().url(requestUrl).get().build()).execute().use { resp ->
                if (!resp.isSuccessful) return url
                val body = resp.body ?: return url
                val text =
                    body.source().let { source ->
                        if (!source.request(MAX_RESPONSE_BYTES + 1)) {
                            source.buffer.readUtf8()
                        } else {
                            source.buffer.readUtf8(MAX_RESPONSE_BYTES)
                        }
                    }
                extractFinalUrl(text) ?: url
            }
        } catch (_: Exception) {
            url
        }
    }
}

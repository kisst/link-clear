package app.linkclear.update

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.linkclear.Engine
import app.linkclear.core.RuleLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Integrity note: ClearURLs publishes no signed releases, so the downloaded ruleset
 * cannot be cryptographically verified. Instead of trusting a bare parse, the update
 * applies [validateRuleset]: a provider-count band plus a check that well-known
 * providers are present. This rejects a truncated, empty-shell, or wildly-inflated
 * payload from a compromised or misbehaving endpoint. It is NOT authentication — a
 * sophisticated attacker able to serve well-formed rules over the pinned HTTPS host
 * could still pass. Future work: pin a signature or checksum once upstream signs.
 */
class RuleUpdateWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result =
        withContext(Dispatchers.IO) {
            try {
                val text = fetchRuleset(RULES_URL)
                val parsed = RuleLoader.parse(text)
                if (!validateRuleset(parsed)) return@withContext Result.retry()
                val dir = File(applicationContext.filesDir, "clearurls").apply { mkdirs() }
                val tmp = File(dir, "data.min.json.tmp")
                val dst = File(dir, "data.min.json")
                tmp.writeText(text)
                if (!tmp.renameTo(dst)) {
                    tmp.copyTo(dst, overwrite = true)
                    tmp.delete()
                }
                Engine.reloadFrom(text)
                Result.success()
            } catch (_: Exception) {
                Result.retry()
            }
        }

    companion object {
        const val RULES_URL = "https://rules2.clearurls.xyz/data.minify.json"
        const val MIN_PROVIDERS = 50

        // Upper bound: the real ClearURLs ruleset has a few hundred providers.
        // A payload an order of magnitude larger is almost certainly hostile or
        // corrupt, even if it parsed and cleared the size cap.
        const val MAX_PROVIDERS = 5000
        const val MAX_BYTES = 5 * 1024 * 1024
        const val TIMEOUT_MS = 10_000

        // A subset of long-standing ClearURLs provider keys. A genuine ruleset
        // always contains these; their absence signals a truncated or spoofed file.
        val REQUIRED_PROVIDERS = setOf("globalRules", "google", "amazon")
    }
}

/**
 * Structural sanity check for a parsed ruleset, factored out of [RuleUpdateWorker]
 * so it can be unit-tested. Accepts the ruleset only when the provider count is
 * within [RuleUpdateWorker.MIN_PROVIDERS]..[RuleUpdateWorker.MAX_PROVIDERS] and
 * every key in [RuleUpdateWorker.REQUIRED_PROVIDERS] is present. See the worker's
 * class KDoc for why this is validation, not authentication.
 */
internal fun validateRuleset(ruleSet: app.linkclear.core.RuleSet): Boolean {
    val count = ruleSet.providers.size
    if (count < RuleUpdateWorker.MIN_PROVIDERS || count > RuleUpdateWorker.MAX_PROVIDERS) {
        return false
    }
    val names = ruleSet.providers.map { it.name }.toSet()
    return RuleUpdateWorker.REQUIRED_PROVIDERS.all { it in names }
}

/**
 * Downloads [urlString] with bounded time and size, enforcing HTTPS.
 *
 * Guards against a hostile or misbehaving endpoint: rejects non-HTTPS schemes,
 * caps connect/read time, caps response size (throws before buffering unbounded
 * data), and requires a 2xx response. Any failure surfaces as an exception so the
 * caller can retry rather than trust a partial or oversized payload. This function
 * does not verify the ruleset's authenticity — see the class-level KDoc above.
 */
internal fun fetchRuleset(
    urlString: String,
    maxBytes: Int = RuleUpdateWorker.MAX_BYTES,
    connectTimeoutMs: Int = RuleUpdateWorker.TIMEOUT_MS,
    readTimeoutMs: Int = RuleUpdateWorker.TIMEOUT_MS,
): String {
    require(urlString.startsWith("https://")) { "refusing non-HTTPS ruleset URL" }

    val connection = URL(urlString).openConnection() as HttpURLConnection
    return readBoundedResponse(connection, maxBytes, connectTimeoutMs, readTimeoutMs)
}

/**
 * Applies the timeout/size/status guards to an already-opened [HttpURLConnection] and
 * returns the decoded body. Split out from [fetchRuleset] so the guard behavior itself
 * (size cap, timeouts, status handling) can be exercised in tests against a plain-HTTP
 * loopback server, independent of the HTTPS-enforcement check.
 */
internal fun readBoundedResponse(
    connection: HttpURLConnection,
    maxBytes: Int = RuleUpdateWorker.MAX_BYTES,
    connectTimeoutMs: Int = RuleUpdateWorker.TIMEOUT_MS,
    readTimeoutMs: Int = RuleUpdateWorker.TIMEOUT_MS,
): String {
    try {
        connection.connectTimeout = connectTimeoutMs
        connection.readTimeout = readTimeoutMs
        connection.requestMethod = "GET"

        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
            throw IOException("HTTP $responseCode")
        }

        val buffer = ByteArray(8192)
        val output = java.io.ByteArrayOutputStream()
        connection.inputStream.use { input ->
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                output.write(buffer, 0, read)
                if (output.size() > maxBytes) {
                    throw IOException("ruleset too large")
                }
            }
        }
        return output.toString("UTF-8")
    } finally {
        connection.disconnect()
    }
}

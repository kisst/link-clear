package app.linkclear

import app.linkclear.core.BundledRules
import app.linkclear.core.CleanResult
import app.linkclear.core.CleaningEngine
import app.linkclear.core.RuleLoader
import app.linkclear.core.UrlExtractor
import app.linkclear.settings.ResolverMode
import app.linkclear.unshorten.DirectHeadResolver
import app.linkclear.unshorten.NoopResolver
import app.linkclear.unshorten.RemoteResolver
import app.linkclear.unshorten.Resolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object Engine {
    @Volatile private var cached: CleaningEngine? = null

    @Volatile private var appContext: android.content.Context? = null

    fun init(context: android.content.Context) {
        appContext = context.applicationContext
    }

    fun get(): CleaningEngine = cached ?: build().also { cached = it }

    /**
     * Builds and caches the engine on [Dispatchers.Default] so the ruleset parse
     * and regex compilation (hundreds of providers) never runs on the main thread
     * at first [get]. Safe to call before any [get]; a concurrent [get] that wins
     * the race just builds an equivalent engine. No-op if already cached.
     */
    fun prewarm(scope: CoroutineScope) {
        if (cached != null) return
        scope.launch(Dispatchers.Default) { get() }
    }

    private fun build(): CleaningEngine {
        val f = appContext?.let { java.io.File(java.io.File(it.filesDir, "clearurls"), "data.min.json") }
        if (f != null && f.exists()) {
            runCatching { return CleaningEngine(RuleLoader.parse(f.readText())) }
        }
        return CleaningEngine(BundledRules.load())
    }

    /** Called after a successful rule update to swap in the downloaded ruleset. */
    fun reloadFrom(jsonText: String) {
        cached = CleaningEngine(RuleLoader.parse(jsonText))
    }
}

object UnshortenGate {
    // internal (not private) so the mode → implementation mapping — a
    // security-relevant branch, notably CUSTOM+non-https falling back to Noop —
    // is unit-testable.
    internal fun resolverFor(
        mode: ResolverMode,
        customUrl: String,
    ): Resolver =
        when (mode) {
            ResolverMode.OFF -> NoopResolver
            ResolverMode.DIRECT -> DirectHeadResolver()
            ResolverMode.CUSTOM -> if (customUrl.startsWith("https://")) RemoteResolver(customUrl) else NoopResolver
        }

    /** Resolve then clean; resolve step is a no-op (instant, no network) when [mode] is OFF. */
    suspend fun cleanUrlMaybeResolve(
        url: String,
        mode: ResolverMode,
        customUrl: String,
    ): CleanResult =
        withContext(Dispatchers.IO) {
            val resolver = resolverFor(mode, customUrl)
            val resolved = resolver.resolve(url)
            val result = Engine.get().cleanUrl(resolved)
            if (resolved != url) result.copy(unshortened = true) else result
        }

    /** Extracts URLs from [text] and resolves+cleans each. */
    suspend fun cleanTextMaybeResolve(
        text: String,
        mode: ResolverMode,
        customUrl: String,
    ): List<CleanResult> =
        withContext(Dispatchers.IO) {
            val resolver = resolverFor(mode, customUrl)
            UrlExtractor.extract(text).map { url ->
                val resolved = resolver.resolve(url)
                val result = Engine.get().cleanUrl(resolved)
                if (resolved != url) result.copy(unshortened = true) else result
            }
        }
}

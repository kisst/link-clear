package app.linkclear.core

import java.net.URLDecoder

class CleaningEngine(private val ruleSet: RuleSet) {
    private companion object {
        /** Max redirection hops followed per URL, to bound cost and prevent loops. */
        const val MAX_REDIRECTS = 5
    }

    private data class CompiledProvider(
        val provider: Provider,
        val urlPattern: Regex,
        val exceptions: List<Regex>,
        val redirections: List<Regex>,
        val rawRules: List<Regex>,
    )

    private val compiled: List<CompiledProvider> =
        ruleSet.providers.mapNotNull { p ->
            try {
                CompiledProvider(
                    provider = p,
                    urlPattern = Regex(p.urlPattern, RegexOption.IGNORE_CASE),
                    exceptions = p.exceptions.map { Regex(it, RegexOption.IGNORE_CASE) },
                    redirections = p.redirections.map { Regex(it, RegexOption.IGNORE_CASE) },
                    rawRules = p.rawRules.map { Regex(it, RegexOption.IGNORE_CASE) },
                )
            } catch (_: java.util.regex.PatternSyntaxException) {
                null
            }
        }

    fun cleanUrl(url: String): CleanResult {
        return try {
            cleanInternal(url)
        } catch (_: Exception) {
            CleanResult(url, url, emptyList(), wasChanged = false)
        }
    }

    private fun applicable(url: String): List<CompiledProvider> =
        compiled.filter { cp ->
            cp.urlPattern.containsMatchIn(url) &&
                cp.exceptions.none { it.containsMatchIn(url) }
        }

    private fun cleanInternal(url: String): CleanResult {
        var current = url
        var changed = false
        val removed = mutableListOf<RemovedParam>()

        // A provider marked `completeProvider` means the whole URL is a
        // tracking/redirect domain ClearURLs would block outright. We flag it
        // (non-destructively) so callers can warn; the URL is still cleaned and
        // returned. Evaluated on the input URL — the host the user is warned
        // about — before any redirect-following rewrites `current`.
        val blocked = applicable(current).any { it.provider.completeProvider }

        // 1. Follow redirections (bounded by MAX_REDIRECTS to prevent loops).
        var i = 0
        while (i < MAX_REDIRECTS) {
            val provs = applicable(current)
            // Takes the first redirection across all applicable providers, so the
            // hop chosen is order-dependent on the compiled provider list when
            // more than one provider matches. The bound above guarantees
            // termination regardless of ordering.
            val redirected =
                provs.asSequence()
                    .flatMap { it.redirections.asSequence() }
                    .mapNotNull { it.find(current)?.groupValues?.getOrNull(1) }
                    .firstOrNull() ?: break
            current = URLDecoder.decode(redirected, "UTF-8")
            changed = true
            i++
        }

        // Split off the fragment before any param stripping/tidying, so a
        // "#fragment" that happens to contain query-like punctuation ('?',
        // '&') is never touched by the param regex or by tidy()'s separator
        // cleanup. It is re-appended verbatim at the end.
        val fragmentIndex = current.indexOf('#')
        val fragment = if (fragmentIndex >= 0) current.substring(fragmentIndex) else null
        if (fragmentIndex >= 0) current = current.substring(0, fragmentIndex)

        // 2. Strip params (rules + referralMarketing) for all applicable providers.
        val provs = applicable(current)
        val paramNames = provs.flatMap { it.provider.rules + it.provider.referralMarketing }.toSet()
        for (name in paramNames) {
            // `name` is itself a regex pattern (ClearURLs rules use patterns like
            // "utm(?:_[a-z_]*)?", not literal param names), so it must not be escaped.
            val re = Regex("""(?:&|[/?#&])(?:(?:$name)=([^&]*))""")
            val matches = re.findAll(current).toList()
            if (matches.isNotEmpty()) {
                for (m in matches) {
                    val fullMatch = m.value.trimStart('&', '/', '?', '#')
                    val key = fullMatch.substringBefore('=')
                    val value = m.groupValues.getOrNull(1).orEmpty()
                    removed += RemovedParam(key, value)
                }
                current = re.replace(current, "")
                changed = true
            }
        }

        // 3. Raw rules.
        for (cp in provs) for (raw in cp.rawRules) {
            val next = raw.replace(current, "")
            if (next != current) {
                current = next
                changed = true
            }
        }

        current = tidy(current)
        if (fragment != null) current += fragment
        return CleanResult(url, current, removed, changed, blocked = blocked)
    }

    private fun tidy(url: String): String {
        var u = url.replace("?&", "?").replace("&&", "&")
        u = u.trimEnd('?', '&')
        // If the query separator itself was stripped (e.g. removing the first
        // param ate the "?"), the remaining params are left dangling after a
        // bare "&" with no "?" earlier in the same segment. Promote that first
        // stray "&" back to "?" so the URL has a well-formed query string.
        val pathAndQuery = u.substringAfter("://", missingDelimiterValue = u)
        val hostAndRest = if (pathAndQuery === u) null else u.substringBefore("://")
        if (hostAndRest != null && !pathAndQuery.contains('?') && pathAndQuery.contains('&')) {
            val fixedRest = pathAndQuery.replaceFirst('&', '?')
            u = "$hostAndRest://$fixedRest"
        }
        return u
    }
}

fun CleaningEngine.cleanText(text: String): List<CleanResult> = UrlExtractor.extract(text).map { cleanUrl(it) }

package app.linkclear.core

object UrlExtractor {
    // http/https followed by non-whitespace run
    private val urlRegex = Regex("""https?://\S+""", RegexOption.IGNORE_CASE)
    private val safePunct = charArrayOf('.', ',', '!', '?', ';', ':', '"', '\'')
    private val bracketPairs = mapOf(')' to '(', ']' to '[', '}' to '{')

    // Trims trailing sentence punctuation unconditionally, and trailing closing
    // brackets only when they are unbalanced (no matching opener earlier in the
    // candidate) -- so "Foo_(bar)" is kept intact but "https://a.com/x)" is trimmed.
    private fun trimTrailingJunk(candidate: String): String {
        var result = candidate
        while (result.isNotEmpty()) {
            val last = result.last()
            val opener = bracketPairs[last]
            val shouldTrim =
                when {
                    last in safePunct -> true
                    opener != null -> result.count { it == last } > result.count { it == opener }
                    else -> false
                }
            if (!shouldTrim) break
            result = result.dropLast(1)
        }
        return result
    }

    fun extract(text: String): List<String> =
        urlRegex.findAll(text)
            .map { trimTrailingJunk(it.value) }
            .filter { it.length > "https://".length }
            .toList()
}

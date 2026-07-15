package app.linkclear.core

object BundledRules {
    fun load(): RuleSet {
        val stream =
            javaClass.getResourceAsStream("/clearurls/data.min.json")
                ?: error("bundled clearurls/data.min.json missing from resources")
        val text = stream.bufferedReader().use { it.readText() }
        return RuleLoader.parse(text)
    }
}

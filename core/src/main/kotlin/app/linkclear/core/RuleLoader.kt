package app.linkclear.core

import kotlinx.serialization.json.Json

object RuleLoader {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(jsonText: String): RuleSet {
        val raw = json.decodeFromString(RawRuleFile.serializer(), jsonText)
        val providers =
            raw.providers.map { (name, p) ->
                Provider(
                    name = name,
                    urlPattern = p.urlPattern,
                    completeProvider = p.completeProvider,
                    rules = p.rules,
                    rawRules = p.rawRules,
                    referralMarketing = p.referralMarketing,
                    exceptions = p.exceptions,
                    redirections = p.redirections,
                )
            }
        return RuleSet(providers)
    }
}

package app.linkclear.core

import kotlinx.serialization.Serializable

@Serializable
internal data class RawProvider(
    val urlPattern: String,
    val completeProvider: Boolean = false,
    val rules: List<String> = emptyList(),
    val rawRules: List<String> = emptyList(),
    val referralMarketing: List<String> = emptyList(),
    val exceptions: List<String> = emptyList(),
    val redirections: List<String> = emptyList(),
)

@Serializable
internal data class RawRuleFile(
    val providers: Map<String, RawProvider>,
)

data class Provider(
    val name: String,
    val urlPattern: String,
    val completeProvider: Boolean,
    val rules: List<String>,
    val rawRules: List<String>,
    val referralMarketing: List<String>,
    val exceptions: List<String>,
    val redirections: List<String>,
)

data class RuleSet(val providers: List<Provider>)

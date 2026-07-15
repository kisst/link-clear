package app.linkclear.settings

/**
 * A named prefill for the custom resolver URL field. Presets are honest
 * starting points, not guaranteed-working keyless services — some require
 * the user's own API key. Selecting one only prefills the editable
 * custom-URL field; it does not change [ResolverMode].
 */
data class ResolverPreset(val label: String, val urlTemplate: String, val note: String)

val RESOLVER_PRESETS: List<ResolverPreset> =
    listOf(
        ResolverPreset(
            label = "unshorten.me",
            urlTemplate = "https://unshorten.me/api/v2/unshorten?url=",
            note =
                "Needs a free API token from unshorten.me — append it or configure per their docs. " +
                    "Response field: unshortened_url.",
        ),
        ResolverPreset(
            label = "unshorten.it",
            urlTemplate = "https://api.unshorten.it/?shortURL=",
            note = "Needs an apiKey query param from unshorten.it. Response field: resolvedURL.",
        ),
        ResolverPreset(
            label = "Self-hosted",
            urlTemplate = "https://your-resolver.example/resolve?url=",
            note =
                "Your own resolver endpoint. Contract: GET {base}?url=<encoded> returning JSON " +
                    "with final_url. Recommended for privacy.",
        ),
    )

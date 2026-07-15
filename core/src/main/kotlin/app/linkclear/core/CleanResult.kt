package app.linkclear.core

data class RemovedParam(val name: String, val value: String)

data class CleanResult(
    val original: String,
    val cleaned: String,
    val removed: List<RemovedParam>,
    val wasChanged: Boolean,
    val unshortened: Boolean = false,
    /**
     * True when an applicable provider is marked `completeProvider` in the
     * ruleset — ClearURLs' signal that the whole URL is a tracking/redirect
     * domain that should be blocked, not merely param-stripped. The cleaned
     * URL is still returned (non-destructive); callers surface a warning.
     */
    val blocked: Boolean = false,
)

val CleanResult.removedCount: Int get() = removed.size

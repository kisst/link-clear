package app.linkclear.unshorten

interface Resolver {
    /** Resolve [url] to its final destination. Returns the input unchanged on any failure.
     *  Runs the network call synchronously; callers invoke off the main thread. */
    fun resolve(url: String): String
}

/** Used when unshorten is off / no provider chosen. Does zero network. */
object NoopResolver : Resolver {
    override fun resolve(url: String): String = url
}

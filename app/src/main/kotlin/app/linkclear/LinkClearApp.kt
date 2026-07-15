package app.linkclear

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class LinkClearApp : Application() {
    // Application-lifetime scope for startup background work (engine pre-warm).
    private val appScope = CoroutineScope(SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        Engine.init(this)
        // Parse the ruleset + compile regexes off the main thread now, so the
        // first UI/tile use of the engine doesn't block on it.
        Engine.prewarm(appScope)
    }
}

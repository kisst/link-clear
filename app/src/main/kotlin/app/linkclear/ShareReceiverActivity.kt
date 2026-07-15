package app.linkclear

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import app.linkclear.core.CleanResult
import app.linkclear.settings.AutoAction
import app.linkclear.settings.SettingsStore
import app.linkclear.ui.ConfirmationScreen
import app.linkclear.ui.theme.LinkClearTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ShareReceiverActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val shared = extractSharedText(intent)
        val store = SettingsStore(applicationContext)

        showLoading()
        lifecycleScope.launch {
            // Read settings inside the coroutine (not runBlocking in onCreate): the
            // first DataStore read touches disk, which would jank/ANR the main thread
            // during activity creation. The loading spinner is already showing.
            val resolverMode = store.resolverMode.first()
            val customResolverUrl = store.customResolverUrl.first()
            val autoEnabled = store.autoActionEnabled.first()
            val autoType = store.autoActionType.first()

            val results = UnshortenGate.cleanTextMaybeResolve(shared, resolverMode, customResolverUrl)
            val joined = results.joinToString("\n") { it.cleaned }

            if (autoEnabled && results.isNotEmpty()) {
                when (autoType) {
                    AutoAction.RESHARE -> {
                        val send =
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, joined)
                            }
                        startActivity(Intent.createChooser(send, null))
                        finish()
                    }
                    AutoAction.COPY -> {
                        writeClipboard(this@ShareReceiverActivity, joined)
                        toast(this@ShareReceiverActivity, "Copied")
                        finish()
                    }
                    AutoAction.OPEN -> {
                        val url = results.firstOrNull()?.cleaned
                        if (url != null && tryOpen(url)) {
                            finish()
                        } else {
                            showConfirmationScreen(results, joined)
                        }
                    }
                }
            } else {
                showConfirmationScreen(results, joined)
            }
        }
    }

    /** Reads shared text from either a single ACTION_SEND or an ACTION_SEND_MULTIPLE intent. */
    private fun extractSharedText(intent: Intent?): String {
        val baseType = intent?.type?.substringBefore(';')?.trim()
        if (baseType != "text/plain") return ""
        return when (intent.action) {
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
            Intent.ACTION_SEND_MULTIPLE -> {
                val items = intent.getCharSequenceArrayListExtra(Intent.EXTRA_TEXT)
                items?.joinToString("\n") { it.toString() }.orEmpty()
            }
            else -> ""
        }
    }

    private fun showLoading() {
        setContent {
            LinkClearTheme {
                Surface {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }

    /** Launches an ACTION_VIEW for [url]. Returns false if no app can handle it. */
    private fun tryOpen(url: String): Boolean =
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            true
        } catch (e: ActivityNotFoundException) {
            false
        } catch (e: Exception) {
            false
        }

    private fun showConfirmationScreen(
        results: List<CleanResult>,
        joined: String,
    ) {
        setContent {
            LinkClearTheme {
                Surface {
                    ConfirmationScreen(
                        results = results,
                        onCopy = {
                            writeClipboard(this, joined)
                            toast(this, "Copied")
                            finish()
                        },
                        onShare = {
                            val send =
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, joined)
                                }
                            startActivity(Intent.createChooser(send, null))
                            finish()
                        },
                        onOpen = {
                            val url = results.firstOrNull()?.cleaned
                            if (url != null && !tryOpen(url)) {
                                toast(this, "No app can open this link")
                            }
                            finish()
                        },
                    )
                }
            }
        }
    }
}

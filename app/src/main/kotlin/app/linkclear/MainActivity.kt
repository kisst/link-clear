package app.linkclear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import app.linkclear.core.UrlExtractor
import app.linkclear.settings.AutoAction
import app.linkclear.settings.ResolverMode
import app.linkclear.settings.SettingsStore
import app.linkclear.ui.EditorScreen
import app.linkclear.ui.SettingsScreen
import app.linkclear.ui.theme.LinkClearTheme
import app.linkclear.update.RuleUpdateWorker
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    // Holds a link found on the clipboard at first focus, to pre-fill the editor.
    // Read at focus time (not onCreate): Android 10+ only lets an app read the
    // clipboard while it has window focus, so a read in onCreate returns null.
    private val clipboardLink = mutableStateOf("")
    private var clipboardChecked = false

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // Once, on the first focus of a fresh launch: if the clipboard holds a URL,
        // surface it so the editor pre-fills and the cleaned result shows with no
        // tap. Gated on UrlExtractor so arbitrary clipboard text is never inserted.
        if (hasFocus && !clipboardChecked) {
            clipboardChecked = true
            clipboardLink.value =
                readClipboard(this)?.let { UrlExtractor.extract(it).firstOrNull() }.orEmpty()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Skip auto-fill on a restored instance (config change / process death) so
        // we don't clobber restored editor state.
        clipboardChecked = savedInstanceState != null
        setContent {
            LinkClearTheme {
                val store = remember { SettingsStore(applicationContext) }
                val resolverMode by store.resolverMode.collectAsState(initial = ResolverMode.OFF)
                val customResolverUrl by store.customResolverUrl.collectAsState(initial = "")
                val autoActionEnabled by store.autoActionEnabled.collectAsState(initial = false)
                val autoActionType by store.autoActionType.collectAsState(initial = AutoAction.RESHARE)
                val scope = rememberCoroutineScope()
                var showSettings by remember { mutableStateOf(false) }
                Surface {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text(if (showSettings) "Settings" else "Link Clear") },
                                navigationIcon = {
                                    if (showSettings) {
                                        IconButton(onClick = { showSettings = false }) {
                                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                        }
                                    }
                                },
                                actions = {
                                    if (!showSettings) {
                                        IconButton(onClick = { showSettings = true }) {
                                            Icon(Icons.Filled.Settings, contentDescription = "Settings")
                                        }
                                    }
                                },
                            )
                        },
                    ) { padding ->
                        if (showSettings) {
                            SettingsScreen(
                                resolverMode = resolverMode,
                                onSelectResolverMode = { scope.launch { store.setResolverMode(it) } },
                                customResolverUrl = customResolverUrl,
                                onCustomResolverUrlChange = { scope.launch { store.setCustomResolverUrl(it) } },
                                onSelectPreset = { preset ->
                                    scope.launch { store.setCustomResolverUrl(preset.urlTemplate) }
                                },
                                autoActionEnabled = autoActionEnabled,
                                onToggleAutoAction = { scope.launch { store.setAutoAction(it) } },
                                autoActionType = autoActionType,
                                onSelectAutoActionType = { scope.launch { store.setAutoActionType(it) } },
                                onUpdateRules = {
                                    WorkManager.getInstance(applicationContext).enqueue(
                                        OneTimeWorkRequestBuilder<RuleUpdateWorker>()
                                            .setConstraints(
                                                Constraints.Builder()
                                                    .setRequiredNetworkType(NetworkType.UNMETERED)
                                                    .build(),
                                            )
                                            .build(),
                                    )
                                    toast(this@MainActivity, "Updating rules…")
                                },
                                modifier = Modifier.padding(padding),
                            )
                        } else {
                            EditorScreen(
                                engine = Engine.get(),
                                clipboardText = { readClipboard(this) },
                                onCopy = {
                                    writeClipboard(this, it)
                                    toast(this, "Copied")
                                },
                                resolveClean = { text ->
                                    UnshortenGate.cleanTextMaybeResolve(text, resolverMode, customResolverUrl)
                                },
                                initialText = clipboardLink.value,
                                modifier = Modifier.padding(padding),
                            )
                        }
                    }
                }
            }
        }
    }
}

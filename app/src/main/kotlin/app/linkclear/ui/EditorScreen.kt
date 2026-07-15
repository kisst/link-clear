package app.linkclear.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.linkclear.core.CleanResult
import app.linkclear.core.CleaningEngine
import app.linkclear.core.cleanText
import kotlinx.coroutines.launch

/**
 * [resolveClean] performs the (possibly network) resolve+clean of a single
 * pasted blob of text, used only by the explicit "Paste Clean" button. Live
 * per-keystroke cleaning below always stays on the offline [engine] path.
 *
 * [initialText] pre-fills the field (e.g. a link already on the clipboard), so
 * the cleaned result shows immediately with no tap needed. It may arrive slightly
 * after first composition (the clipboard is read once the activity gains focus),
 * so it is adopted whenever it becomes non-blank while the field is still empty —
 * the user's own edits are never overwritten.
 */
@Composable
fun EditorScreen(
    engine: CleaningEngine,
    clipboardText: () -> String?,
    onCopy: (String) -> Unit,
    resolveClean: suspend (String) -> List<CleanResult>,
    initialText: String = "",
    modifier: Modifier = Modifier,
) {
    var input by remember { mutableStateOf(initialText) }
    var seeded by remember { mutableStateOf(initialText.isNotEmpty()) }
    LaunchedEffect(initialText) {
        if (!seeded && initialText.isNotEmpty() && input.isEmpty()) {
            input = initialText
            seeded = true
        }
    }
    var resolving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val results: List<CleanResult> =
        remember(input) {
            if (input.isBlank()) emptyList() else engine.cleanText(input)
        }
    val firstClean = results.firstOrNull()?.cleaned

    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("Paste a link") },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { input = clipboardText().orEmpty() }) { Text("Paste Raw") }
            Button(
                enabled = !resolving,
                onClick = {
                    val pasted = clipboardText().orEmpty()
                    resolving = true
                    scope.launch {
                        val cleaned = resolveClean(pasted)
                        input = cleaned.firstOrNull()?.cleaned ?: pasted
                        resolving = false
                    }
                },
            ) { Text(if (resolving) "Cleaning…" else "Paste Clean") }
        }
        results.forEach { DiffView(it) }
        if (firstClean != null) {
            Button(onClick = { onCopy(firstClean) }) { Text("Copy Clean") }
        }
    }
}

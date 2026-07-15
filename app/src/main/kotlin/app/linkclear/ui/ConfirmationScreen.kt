package app.linkclear.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.linkclear.core.CleanResult

@Composable
fun ConfirmationScreen(
    results: List<CleanResult>,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (results.isEmpty()) {
            Text("No link found")
            return@Column
        }
        results.forEach { DiffView(it) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onCopy) { Text("Copy") }
            OutlinedButton(onClick = onShare) { Text("Share") }
            OutlinedButton(onClick = onOpen) { Text("Open") }
        }
    }
}

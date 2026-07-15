package app.linkclear.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.linkclear.core.CleanResult
import app.linkclear.core.removedCount

@Composable
fun DiffView(
    result: CleanResult,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(result.cleaned, style = MaterialTheme.typography.titleMedium)
        if (result.blocked) {
            Text(
                "⚠ This whole link is a known tracking/redirect domain.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Text(summaryLine(result), style = MaterialTheme.typography.bodyMedium)
        for (p in result.removed) {
            Text("− ${p.name}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

internal fun summaryLine(result: CleanResult): String =
    when {
        !result.wasChanged -> "Already clean"
        result.removedCount == 1 -> "1 tracker removed"
        result.removedCount > 1 -> "${result.removedCount} trackers removed"
        else -> "Cleaned"
    }

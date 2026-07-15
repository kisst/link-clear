package app.linkclear.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.linkclear.settings.AutoAction
import app.linkclear.settings.RESOLVER_PRESETS
import app.linkclear.settings.ResolverMode
import app.linkclear.settings.ResolverPreset

/**
 * A titled, rounded grouping card. Sections in a settings screen read as distinct
 * concerns, so each gets its own surface rather than living in one flat list.
 */
@Composable
private fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            content()
        }
    }
}

/**
 * One selectable option: a radio aligned to the FIRST text line (so a wrapping
 * title never floats the control to the middle of the block), a bold title, and a
 * muted supporting line.
 */
@Composable
private fun OptionRow(
    selected: Boolean,
    onSelect: () -> Unit,
    title: String,
    description: String,
) {
    Row(
        Modifier.fillMaxWidth().selectable(
            selected = selected,
            onClick = onSelect,
            role = Role.RadioButton,
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(
            Modifier.padding(top = 2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    resolverMode: ResolverMode,
    onSelectResolverMode: (ResolverMode) -> Unit,
    customResolverUrl: String,
    onCustomResolverUrlChange: (String) -> Unit,
    onSelectPreset: (ResolverPreset) -> Unit,
    autoActionEnabled: Boolean,
    onToggleAutoAction: (Boolean) -> Unit,
    autoActionType: AutoAction,
    onSelectAutoActionType: (AutoAction) -> Unit,
    onUpdateRules: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SettingsSection("Resolve shortened links") {
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OptionRow(
                    selected = resolverMode == ResolverMode.OFF,
                    onSelect = { onSelectResolverMode(ResolverMode.OFF) },
                    title = "Off",
                    description = "Shortened links are left as-is. No network use.",
                )
                OptionRow(
                    selected = resolverMode == ResolverMode.DIRECT,
                    onSelect = { onSelectResolverMode(ResolverMode.DIRECT) },
                    title = "Direct (from this device)",
                    description = "Your device contacts the shortener directly to reveal the real URL.",
                )
                OptionRow(
                    selected = resolverMode == ResolverMode.CUSTOM,
                    onSelect = { onSelectResolverMode(ResolverMode.CUSTOM) },
                    title = "Custom resolver (off-device)",
                    description =
                        "An external resolver you trust fetches the link instead of your " +
                            "device. Enter its https URL below.",
                )
            }
            if (resolverMode == ResolverMode.CUSTOM) {
                var selectedPresetNote by remember { mutableStateOf<String?>(null) }
                Text(
                    "Starting points — pick one, then edit:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RESOLVER_PRESETS.forEach { preset ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                selectedPresetNote = preset.note
                                onSelectPreset(preset)
                            },
                            label = { Text(preset.label) },
                        )
                    }
                }
                OutlinedTextField(
                    value = customResolverUrl,
                    onValueChange = onCustomResolverUrlChange,
                    label = { Text("Custom resolver URL") },
                    placeholder = { Text("https://resolver.example.com/resolve?url=") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    selectedPresetNote
                        ?: "Must be https and a resolver you trust — it will see the links you " +
                        "share. The url-encoded target is appended directly to this URL.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        SettingsSection("Automatic action") {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Skip the confirmation screen and run an action automatically when you share a link.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = autoActionEnabled, onCheckedChange = onToggleAutoAction)
            }
            if (autoActionEnabled) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = autoActionType == AutoAction.RESHARE,
                        onClick = { onSelectAutoActionType(AutoAction.RESHARE) },
                        label = { Text("Re-share") },
                    )
                    FilterChip(
                        selected = autoActionType == AutoAction.COPY,
                        onClick = { onSelectAutoActionType(AutoAction.COPY) },
                        label = { Text("Copy") },
                    )
                    FilterChip(
                        selected = autoActionType == AutoAction.OPEN,
                        onClick = { onSelectAutoActionType(AutoAction.OPEN) },
                        label = { Text("Open") },
                    )
                }
            }
        }

        SettingsSection("Rules & about") {
            Text(
                "URL-cleaning rules from the ClearURLs project (LGPL-3.0). " +
                    "Link Clear is licensed GPL-3.0.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onUpdateRules) { Text("Update rules now") }
        }
    }
}

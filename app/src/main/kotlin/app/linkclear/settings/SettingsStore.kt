package app.linkclear.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")
private val RESOLVER_MODE = stringPreferencesKey("resolver_mode")
private val CUSTOM_RESOLVER_URL = stringPreferencesKey("custom_resolver_url")
private val AUTO_ACTION_ENABLED = booleanPreferencesKey("auto_action_enabled")
private val AUTO_ACTION_TYPE = stringPreferencesKey("auto_action_type")

enum class AutoAction { RESHARE, COPY, OPEN }

/**
 * How (if at all) shortened links get resolved before cleaning.
 * OFF: no network, instant (default).
 * DIRECT: the device itself contacts the shortener (on-device HEAD request).
 * CUSTOM: a user-supplied https resolver endpoint fetches the target instead.
 */
enum class ResolverMode { OFF, DIRECT, CUSTOM }

class SettingsStore(private val context: Context) {
    val resolverMode: Flow<ResolverMode> =
        context.dataStore.data.map { prefs ->
            val stored = prefs[RESOLVER_MODE]
            if (stored == null) {
                ResolverMode.OFF
            } else {
                try {
                    ResolverMode.valueOf(stored)
                } catch (e: IllegalArgumentException) {
                    ResolverMode.OFF
                }
            }
        }

    suspend fun setResolverMode(mode: ResolverMode) {
        context.dataStore.edit { it[RESOLVER_MODE] = mode.name }
    }

    val customResolverUrl: Flow<String> =
        context.dataStore.data.map { it[CUSTOM_RESOLVER_URL] ?: "" }

    suspend fun setCustomResolverUrl(url: String) {
        context.dataStore.edit { it[CUSTOM_RESOLVER_URL] = url }
    }

    val autoActionEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[AUTO_ACTION_ENABLED] ?: false }

    suspend fun setAutoAction(enabled: Boolean) {
        context.dataStore.edit { it[AUTO_ACTION_ENABLED] = enabled }
    }

    val autoActionType: Flow<AutoAction> =
        context.dataStore.data.map { prefs ->
            val stored = prefs[AUTO_ACTION_TYPE]
            if (stored == null) {
                AutoAction.RESHARE
            } else {
                try {
                    AutoAction.valueOf(stored)
                } catch (e: IllegalArgumentException) {
                    AutoAction.RESHARE
                }
            }
        }

    suspend fun setAutoActionType(type: AutoAction) {
        context.dataStore.edit { it[AUTO_ACTION_TYPE] = type.name }
    }
}

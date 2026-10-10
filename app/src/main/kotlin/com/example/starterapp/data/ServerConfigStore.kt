package com.example.starterapp.data

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "server_config")

/**
 * Backs [ServerConfig.authHeaderValue], the one secret this store holds. It stays out of the
 * plain-text `server_config` DataStore by using Keystore-backed AES256-GCM encrypted prefs.
 */
private const val ENCRYPTED_PREFS_NAME = "server_config_secure"
private const val KEY_AUTH_HEADER_VALUE = "auth_header_value"

private fun encryptedPrefs(context: Context): SharedPreferences {
    val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    return EncryptedSharedPreferences.create(
        context,
        ENCRYPTED_PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
}

data class ServerConfig(
    val baseUrl: String = "",
    val authHeaderName: String = "",
    val authHeaderValue: String = ""
) {
    val isConfigured: Boolean get() = baseUrl.isNotBlank()
}

/**
 * Persists a user-supplied server URL plus one static auth header (name and value). Use this
 * when the app talks to a self-hosted or per-customer backend whose auth is enforced by a
 * proxy or gateway. If your app talks to one fixed backend with a real login flow, replace this
 * with a build-time base URL and a token store.
 */
class ServerConfigStore(private val context: Context) {
    private val keyBaseUrl = stringPreferencesKey("base_url")
    private val keyHeaderName = stringPreferencesKey("auth_header_name")
    private val securePrefs by lazy { encryptedPrefs(context) }

    val config: Flow<ServerConfig> =
        context.dataStore.data.map { prefs ->
            ServerConfig(
                baseUrl = prefs[keyBaseUrl] ?: "",
                authHeaderName = prefs[keyHeaderName] ?: "",
                authHeaderValue = securePrefs.getString(KEY_AUTH_HEADER_VALUE, "") ?: ""
            )
        }

    suspend fun current(): ServerConfig = config.first()

    suspend fun save(config: ServerConfig) {
        context.dataStore.edit { prefs ->
            prefs[keyBaseUrl] =
                if (config.baseUrl.isBlank()) "" else config.baseUrl.trimEnd('/') + "/"
            prefs[keyHeaderName] = config.authHeaderName
        }
        securePrefs.edit().putString(KEY_AUTH_HEADER_VALUE, config.authHeaderValue).apply()
    }
}

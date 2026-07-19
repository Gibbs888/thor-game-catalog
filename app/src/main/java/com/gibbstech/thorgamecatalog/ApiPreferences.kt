package com.gibbstech.thorgamecatalog

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class ApiConfig(
    val igdbClientId: String = "",
    val igdbClientSecret: String = "",
    val screenScraperDeveloperId: String = "",
    val screenScraperDeveloperPassword: String = "",
    val screenScraperSoftName: String = "ThorGameCatalog",
    val screenScraperUsername: String = "",
    val screenScraperPassword: String = "",
) {
    val isIgdbReady: Boolean
        get() = igdbClientId.isNotBlank() && igdbClientSecret.isNotBlank()

    val isScreenScraperReady: Boolean
        get() = screenScraperDeveloperId.isNotBlank() &&
            screenScraperDeveloperPassword.isNotBlank()
}

class ApiPreferences(context: Context) {
    private val secureStore = SecureValueStore(context.applicationContext)

    fun load(): ApiConfig = ApiConfig(
        igdbClientId = secureStore.get(KEY_IGDB_CLIENT_ID),
        igdbClientSecret = secureStore.get(KEY_IGDB_CLIENT_SECRET),
        screenScraperDeveloperId = secureStore.get(KEY_SS_DEVELOPER_ID),
        screenScraperDeveloperPassword = secureStore.get(KEY_SS_DEVELOPER_PASSWORD),
        screenScraperSoftName = secureStore.get(KEY_SS_SOFT_NAME).ifBlank { "ThorGameCatalog" },
        screenScraperUsername = secureStore.get(KEY_SS_USERNAME),
        screenScraperPassword = secureStore.get(KEY_SS_PASSWORD),
    )

    fun save(config: ApiConfig) {
        secureStore.put(KEY_IGDB_CLIENT_ID, config.igdbClientId.trim())
        secureStore.put(KEY_IGDB_CLIENT_SECRET, config.igdbClientSecret.trim())
        secureStore.put(KEY_SS_DEVELOPER_ID, config.screenScraperDeveloperId.trim())
        secureStore.put(KEY_SS_DEVELOPER_PASSWORD, config.screenScraperDeveloperPassword.trim())
        secureStore.put(KEY_SS_SOFT_NAME, config.screenScraperSoftName.trim())
        secureStore.put(KEY_SS_USERNAME, config.screenScraperUsername.trim())
        secureStore.put(KEY_SS_PASSWORD, config.screenScraperPassword.trim())
    }

    private companion object {
        const val KEY_IGDB_CLIENT_ID = "igdb_client_id"
        const val KEY_IGDB_CLIENT_SECRET = "igdb_client_secret"
        const val KEY_SS_DEVELOPER_ID = "ss_developer_id"
        const val KEY_SS_DEVELOPER_PASSWORD = "ss_developer_password"
        const val KEY_SS_SOFT_NAME = "ss_soft_name"
        const val KEY_SS_USERNAME = "ss_username"
        const val KEY_SS_PASSWORD = "ss_password"
    }
}

private class SecureValueStore(context: Context) {
    private val preferences = context.getSharedPreferences(
        "secure_api_credentials",
        Context.MODE_PRIVATE,
    )

    fun put(key: String, value: String) {
        if (value.isBlank()) {
            preferences.edit().remove(key).apply()
            return
        }

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val encodedIv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
        val encodedValue = Base64.encodeToString(encrypted, Base64.NO_WRAP)
        preferences.edit().putString(key, "$encodedIv:$encodedValue").apply()
    }

    fun get(key: String): String {
        val storedValue = preferences.getString(key, null) ?: return ""
        return runCatching {
            val (encodedIv, encodedValue) = storedValue.split(":", limit = 2)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                secretKey(),
                GCMParameterSpec(128, Base64.decode(encodedIv, Base64.NO_WRAP)),
            )
            val decrypted = cipher.doFinal(Base64.decode(encodedValue, Base64.NO_WRAP))
            String(decrypted, Charsets.UTF_8)
        }.getOrElse {
            preferences.edit().remove(key).apply()
            ""
        }
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build(),
            )
            generateKey()
        }
    }

    private companion object {
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val KEY_ALIAS = "thor_catalog_api_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}

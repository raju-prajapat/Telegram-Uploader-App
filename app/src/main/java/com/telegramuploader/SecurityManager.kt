package com.telegramuploader

import android.content.Context
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class SecurityManager(private val context: Context) {

    private val prefs =
        context.getSharedPreferences(
            "secure_settings",
            Context.MODE_PRIVATE
        )

    private val keyAlias = "TelegramUploaderSettingsKey"

    fun hasPassword(): Boolean {
        return prefs.contains("password_hash") &&
                prefs.contains("password_salt")
    }

    fun setupPassword(password: String): Boolean {

        if (password.length < 4) {
            return false
        }

        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)

        val hash = derivePassword(password, salt)

        prefs.edit()
            .putString(
                "password_salt",
                Base64.encodeToString(
                    salt,
                    Base64.NO_WRAP
                )
            )
            .putString(
                "password_hash",
                Base64.encodeToString(
                    hash,
                    Base64.NO_WRAP
                )
            )
            .apply()

        return true
    }

    fun verifyPassword(password: String): Boolean {

        val saltText =
            prefs.getString(
                "password_salt",
                null
            ) ?: return false

        val hashText =
            prefs.getString(
                "password_hash",
                null
            ) ?: return false

        return try {

            val salt =
                Base64.decode(
                    saltText,
                    Base64.NO_WRAP
                )

            val storedHash =
                Base64.decode(
                    hashText,
                    Base64.NO_WRAP
                )

            val enteredHash =
                derivePassword(
                    password,
                    salt
                )

            storedHash.contentEquals(
                enteredHash
            )

        } catch (_: Exception) {
            false
        }
    }

    fun saveTelegramConfig(
        token: String,
        chatId: String
    ) {

        putEncrypted(
            "telegram_token",
            token
        )

        putEncrypted(
            "telegram_chat",
            chatId
        )
    }

    fun getTelegramToken(): String {
        return getEncrypted(
            "telegram_token"
        )
    }

    fun getTelegramChatId(): String {
        return getEncrypted(
            "telegram_chat"
        )
    }

    fun isTelegramConfigured(): Boolean {

        return getTelegramToken().isNotEmpty() &&
                getTelegramChatId().isNotEmpty()
    }

    fun resetAllCredentials(): Boolean {

        return try {

            val keyStore =
                KeyStore.getInstance(
                    "AndroidKeyStore"
                )

            keyStore.load(null)

            if (
                keyStore.containsAlias(keyAlias)
            ) {
                keyStore.deleteEntry(keyAlias)
            }

            prefs.edit()
                .clear()
                .apply()

            true

        } catch (_: Exception) {
            false
        }
    }

    private fun derivePassword(
        password: String,
        salt: ByteArray
    ): ByteArray {

        val spec =
            PBEKeySpec(
                password.toCharArray(),
                salt,
                120000,
                256
            )

        val factory =
            SecretKeyFactory.getInstance(
                "PBKDF2WithHmacSHA256"
            )

        return factory
            .generateSecret(spec)
            .encoded
    }

    private fun getSecretKey(): SecretKey {

        val keyStore =
            KeyStore.getInstance(
                "AndroidKeyStore"
            )

        keyStore.load(null)

        val existing =
            keyStore.getKey(
                keyAlias,
                null
            )

        if (existing is SecretKey) {
            return existing
        }

        val generator =
            KeyGenerator.getInstance(
                "AES",
                "AndroidKeyStore"
            )

        val spec =
            android.security.keystore.KeyGenParameterSpec.Builder(
                keyAlias,
                android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or
                        android.security.keystore.KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(
                    android.security.keystore.KeyProperties.BLOCK_MODE_GCM
                )
                .setEncryptionPaddings(
                    android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE
                )
                .build()

        generator.init(spec)

        return generator.generateKey()
    }

    private fun putEncrypted(
        name: String,
        value: String
    ) {

        if (value.isEmpty()) {
            prefs.edit()
                .remove(name)
                .remove("${name}_iv")
                .apply()
            return
        }

        val cipher =
            Cipher.getInstance(
                "AES/GCM/NoPadding"
            )

        cipher.init(
            Cipher.ENCRYPT_MODE,
            getSecretKey()
        )

        val encrypted =
            cipher.doFinal(
                value.toByteArray(
                    StandardCharsets.UTF_8
                )
            )

        prefs.edit()
            .putString(
                name,
                Base64.encodeToString(
                    encrypted,
                    Base64.NO_WRAP
                )
            )
            .putString(
                "${name}_iv",
                Base64.encodeToString(
                    cipher.iv,
                    Base64.NO_WRAP
                )
            )
            .apply()
    }

    private fun getEncrypted(
        name: String
    ): String {

        val encryptedText =
            prefs.getString(
                name,
                null
            ) ?: return ""

        val ivText =
            prefs.getString(
                "${name}_iv",
                null
            ) ?: return ""

        return try {

            val encrypted =
                Base64.decode(
                    encryptedText,
                    Base64.NO_WRAP
                )

            val iv =
                Base64.decode(
                    ivText,
                    Base64.NO_WRAP
                )

            val cipher =
                Cipher.getInstance(
                    "AES/GCM/NoPadding"
                )

            cipher.init(
                Cipher.DECRYPT_MODE,
                getSecretKey(),
                GCMParameterSpec(
                    128,
                    iv
                )
            )

            String(
                cipher.doFinal(encrypted),
                StandardCharsets.UTF_8
            )

        } catch (_: Exception) {
            ""
        }
    }
}

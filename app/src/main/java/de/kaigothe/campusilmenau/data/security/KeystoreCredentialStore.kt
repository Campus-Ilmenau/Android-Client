package de.kaigothe.campusilmenau.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Verschlüsselt die Zugangsdaten mit AES-GCM, der Schlüssel liegt im Android Keystore und verlässt ihn nie. Die
 * verschlüsselten Bytes stehen in einer eigenen Preferences-Datei (`paia_credentials`), die von der Sicherung
 * ausgenommen ist: Auf einem anderen Gerät gäbe es den Schlüssel ohnehin nicht. Klartext steht nirgends, auch nicht in Logs.
 */
@Singleton
class KeystoreCredentialStore @Inject constructor(@ApplicationContext context: Context) : CredentialStore {

    private val preferences = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    override fun save(credentials: StoredCredentials) {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key()) }
        val plain = (credentials.username + SEPARATOR + credentials.password).toByteArray(Charsets.UTF_8)
        val encrypted = cipher.doFinal(plain)
        preferences.edit()
            .putString(KEY_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putString(KEY_DATA, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .apply()
    }

    override fun load(): StoredCredentials? {
        val iv = preferences.getString(KEY_IV, null) ?: return null
        val data = preferences.getString(KEY_DATA, null) ?: return null
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, Base64.decode(iv, Base64.NO_WRAP)))
            }
            val text = String(cipher.doFinal(Base64.decode(data, Base64.NO_WRAP)), Charsets.UTF_8)
            val index = text.indexOf(SEPARATOR)
            if (index < 0) null.also { clear() } else StoredCredentials(text.substring(0, index), text.substring(index + 1))
        } catch (_: Exception) {
            // Schlüssel ungültig (zum Beispiel nach einer Wiederherstellung) oder Daten beschädigt: nicht erneut versuchen.
            clear()
            null
        }
    }

    override fun clear() {
        preferences.edit().clear().apply()
        runCatching { keyStore().deleteEntry(ALIAS) }
    }

    private fun keyStore(): KeyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }

    private fun key(): SecretKey {
        (keyStore().getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        generator.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val FILE = "paia_credentials"
        const val KEY_IV = "iv"
        const val KEY_DATA = "data"
        const val ALIAS = "paia_credentials_key"
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val TAG_BITS = 128

        /** Trennt Benutzernummer und Passwort. Die Benutzernummer enthält nur Ziffern und Buchstaben, das Zeichen kommt dort nie vor. */
        const val SEPARATOR = '\u0000'
    }
}

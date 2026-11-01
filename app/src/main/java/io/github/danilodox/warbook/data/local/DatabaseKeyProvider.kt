package io.github.danilodox.warbook.data.local

import io.github.danilodox.warbook.data.datastore.SecureTokenProvider
import java.util.UUID

class DatabaseKeyProvider(private val secureTokenProvider: SecureTokenProvider) {

    // A unique key to identify the passphrase in your secure storage
    private val passphraseKey = "db_passphrase"

    fun getKey(): ByteArray {
        // Try to get the existing passphrase from secure storage
        var passphrase = secureTokenProvider.getCustomString(passphraseKey)

        // If it doesn't exist (first time the app is run)
        if (passphrase == null) {
            // Generate a new, strong, random passphrase
            passphrase = UUID.randomUUID().toString()
            // Save it securely for all future launches
            secureTokenProvider.saveCustomString(passphraseKey, passphrase)
        }

        return passphrase.toByteArray()
    }
}
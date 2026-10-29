package com.daro.agentapp.data.local.session

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import java.io.File
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** AES-GCM ciphertext only, stored outside cloud backup/device transfer. Requires API 23+. */
internal class KeystoreSessionStore(context: Context, storageId: String = "session") : SessionStore {
    init { require(storageId.matches(Regex("[A-Za-z0-9_-]+"))) }
    private val directory = File(context.applicationContext.noBackupFilesDir, storageId)
    private val file = AtomicFile(File(directory, "session.enc"))
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }
    private val alias = "${context.packageName}.$storageId.aes.v1"

    override suspend fun read(): StoredSession? = withContext(Dispatchers.IO) {
        mutex.withLock {
            val bytes = try {
                file.openRead().use { input ->
                    // Bounded reads also reject truncated/corrupt stores without raw-data logging.
                    val output = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(4096)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        if (output.size() + count > MAX_BYTES) throw IOException("Session file exceeds limit")
                        output.write(buffer, 0, count)
                    }
                    output.toByteArray()
                }
            } catch (e: java.io.FileNotFoundException) {
                if (file.baseFile.exists() || File(file.baseFile.path + ".bak").exists()) throw e
                return@withLock null
            }
            if (bytes.size < 30 || bytes[0] != VERSION) throw IOException("Invalid session file")
            val iv = bytes.copyOfRange(1, 13)
            val key = keyStore().getKey(alias, null) as? SecretKey
                ?: throw GeneralSecurityException("Session key unavailable")
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
            val plaintext = cipher.doFinal(bytes.copyOfRange(13, bytes.size))
            json.decodeFromString<StoredSession>(plaintext.toString(Charsets.UTF_8))
        }
    }

    override suspend fun write(session: StoredSession) = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!directory.exists() && !directory.mkdirs()) throw IOException("Session directory unavailable")
            val plaintext = json.encodeToString(session).toByteArray(Charsets.UTF_8)
            if (plaintext.size > MAX_BYTES - 29) throw IOException("Session exceeds limit")
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey())
            check(cipher.iv.size == 12) { "Unexpected GCM IV size" }
            val ciphertext = byteArrayOf(VERSION) + cipher.iv + cipher.doFinal(plaintext)
            val stream = file.startWrite()
            try {
                stream.write(ciphertext)
                file.finishWrite(stream)
            } catch (e: Exception) {
                file.failWrite(stream)
                throw e
            }
        }
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        mutex.withLock {
            // Destroy the key first: old ciphertext cannot become usable after a failed file deletion.
            keyStore().deleteEntry(alias)
            file.delete()
            if (file.baseFile.exists() || File(file.baseFile.path + ".bak").exists()) {
                throw IOException("Session file could not be deleted")
            }
        }
    }

    private fun keyStore() = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    private fun encryptionKey(): SecretKey {
        val existing = keyStore().getKey(alias, null) as? SecretKey
        if (existing != null) return existing
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build())
        }.generateKey()
    }

    private companion object {
        const val VERSION: Byte = 1
        const val MAX_BYTES = 65_536
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}

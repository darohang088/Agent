package com.daro.agentapp.data.local.session

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

@RunWith(AndroidJUnit4::class)
class KeystoreSessionStoreTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun encryptedFileRestoresAcrossStoreInstancesAndClearRemovesIt() = runBlocking {
        val id = "session-test-${UUID.randomUUID()}"
        val store = KeystoreSessionStore(context, id)
        val file = File(context.noBackupFilesDir, "$id/session.enc")
        try {
            val value = StoredSession("synthetic-access", "synthetic-refresh", "Bearer", Long.MAX_VALUE, 7, "USER")
            store.write(value)
            val firstCiphertext = file.readBytes()
            assertFalse(firstCiphertext.toString(Charsets.UTF_8).contains("synthetic-access"))
            assertFalse(firstCiphertext.toString(Charsets.UTF_8).contains("synthetic-refresh"))
            val restored = KeystoreSessionStore(context, id).read()!!
            assertEquals("synthetic-access", restored.accessToken)
            assertEquals(7L, restored.userId)
            store.write(value)
            assertFalse(firstCiphertext.contentEquals(file.readBytes()))
            store.clear()
            assertNull(KeystoreSessionStore(context, id).read())
            assertFalse(file.exists())
        } finally { store.clear() }
    }

    @Test fun corruptedCiphertextCannotBeRead() = runBlocking {
        val id = "session-test-${UUID.randomUUID()}"
        val store = KeystoreSessionStore(context, id)
        val file = File(context.noBackupFilesDir, "$id/session.enc")
        try {
            store.write(StoredSession("synthetic-access", "synthetic-refresh", "Bearer", Long.MAX_VALUE, 7, "USER"))
            val bytes = file.readBytes()
            bytes[bytes.lastIndex] = (bytes.last().toInt() xor 1).toByte()
            file.writeBytes(bytes)
            try {
                KeystoreSessionStore(context, id).read()
                fail("Expected authenticated decryption to reject tampered ciphertext")
            } catch (_: java.security.GeneralSecurityException) { }
        } finally { store.clear() }
    }
}

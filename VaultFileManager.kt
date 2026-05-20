package com.dacksec.appvault.data

import android.content.Context
import android.net.Uri
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import java.io.File
import java.io.FileInputStream

data class VaultFile(
    val name: String,
    val file: File,
    val mimeType: String,
    val sizeBytes: Long
)

class VaultFileManager(private val context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    // Vault directory inside app's private storage (not accessible without root)
    private val vaultDir: File = File(context.filesDir, "vault").also { it.mkdirs() }

    /** Import a file from external URI into the encrypted vault */
    fun importFile(uri: Uri, originalName: String): VaultFile? {
        return try {
            val destFile = File(vaultDir, "${System.currentTimeMillis()}_$originalName.enc")
            val encryptedFile = buildEncryptedFile(destFile)

            context.contentResolver.openInputStream(uri)?.use { input ->
                encryptedFile.openFileOutput().use { output ->
                    input.copyTo(output)
                }
            }

            VaultFile(
                name = originalName,
                file = destFile,
                mimeType = guessMime(originalName),
                sizeBytes = destFile.length()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** Read a vault file back to a temp decrypted copy for viewing */
    fun decryptToTemp(vaultFile: VaultFile): File? {
        return try {
            val tempFile = File(context.cacheDir, "temp_${vaultFile.name}")
            val encryptedFile = buildEncryptedFile(vaultFile.file)
            encryptedFile.openFileInput().use { input ->
                tempFile.outputStream().use { out -> input.copyTo(out) }
            }
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** List all files in the vault */
    fun listFiles(): List<VaultFile> {
        return vaultDir.listFiles()?.map { f ->
            // Strip leading timestamp and trailing .enc
            val displayName = f.name
                .replace(Regex("^\\d+_"), "")
                .removeSuffix(".enc")
            VaultFile(
                name = displayName,
                file = f,
                mimeType = guessMime(displayName),
                sizeBytes = f.length()
            )
        }?.sortedByDescending { it.file.lastModified() } ?: emptyList()
    }

    /** Permanently delete a file from the vault */
    fun deleteFile(vaultFile: VaultFile): Boolean {
        return vaultFile.file.delete()
    }

    private fun buildEncryptedFile(file: File) = EncryptedFile.Builder(
        context,
        file,
        masterKey,
        EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
    ).build()

    private fun guessMime(name: String): String = when {
        name.endsWith(".jpg", true) || name.endsWith(".jpeg", true) -> "image/jpeg"
        name.endsWith(".png", true) -> "image/png"
        name.endsWith(".mp4", true) -> "video/mp4"
        name.endsWith(".pdf", true) -> "application/pdf"
        else -> "application/octet-stream"
    }

    fun formatSize(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${"%.1f".format(bytes / 1024f / 1024f)} MB"
    }
}

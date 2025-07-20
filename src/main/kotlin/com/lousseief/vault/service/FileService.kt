package com.lousseief.vault.service

import com.lousseief.vault.crypto.Conversion
import com.lousseief.vault.exception.FileException
import com.lousseief.vault.exception.InternalException
import com.lousseief.vault.model.Profile
import com.lousseief.vault.model.Vault
import java.io.File
import java.io.IOException
import java.time.Instant

object FileService {

    val FILE_SUFFIX = ".vault"

    fun userExists(user: String): Boolean {
        // why can't you put the dot in the expression template instead of in the SUFFIX?'
        val parentDir = File("../")
        val parentDirContent = parentDir.list()
        if(parentDirContent !== null) {
            return parentDirContent.contains(user + FILE_SUFFIX) && File("../$user$FILE_SUFFIX").isFile
        }
        return false
    }

    fun readFile(user: String): Profile {
        if(!userExists(user))
            throw FileException(FileException.FileExceptionCause.NOT_FOUND,
                IOException("User doesn't exist (no .vault file was found)")
            )
        try {
            val userFile = File("../" + user + FILE_SUFFIX)
            val fileBytes = userFile.readBytes()
            val fileText = Conversion.bytesToUTF8(fileBytes) // content is Base64 but with line endings
            val parts = fileText.split("\n")
            assert(parts.size == 6, { "Expected .vault file to contain 6 parts but was ${parts.size}" })
            return Profile(user, parts[0], parts[1], parts[2], parts[3], parts[4], parts[5])
        }
        catch(e: AssertionError) {
            throw FileException(FileException.FileExceptionCause.CORRUPT_FILE, e)
        }
        catch(e: Exception) {
            throw FileException(FileException.FileExceptionCause.READ_ERROR, e)
        }

    }

    fun writeVaultFile(username: String, userData: String, overwrite: Boolean) {
        try {
            assert(overwrite || !userExists(username))
            val userFile = File("../" + username + FILE_SUFFIX)
            userFile.writeBytes(Conversion.UTF8ToBytes(userData))
        }
        catch(e: AssertionError) {
            throw InternalException(InternalException.InternalExceptionCause.FILE_EXISTS, e)
        }
        catch(e: Exception) {
            throw FileException(FileException.FileExceptionCause.WRITE_ERROR, e)
        }
    }

    fun writeExportFile(username: String, vault: Vault): String {
        try {
            val filename = "../${username}_export_${Instant.now()}.txt"
            val userFile = File(filename)
            val buffer = StringBuffer()
            buffer.appendLine("VAULT EXPORT ${Instant.now()}")
            buffer.appendLine()
            buffer.appendLine("Settings:")
            buffer.appendLine("\tDefault password length: ${vault.first.passwordLength}")
            buffer.appendLine("\tPassword active time: ${vault.first.savePasswordForMinutes}")
            buffer.appendLine("\tCategories: ${vault.first.categories.ifEmpty { null }?.joinToString(",") ?: "no categories"}")
            buffer.appendLine("Data:")
            vault.second.keys.forEach {
                val assoc = vault.second.get(it)!!
                buffer.appendLine("\t$it:")
                buffer.appendLine("\t\tMain identifier: ${assoc.association.mainIdentifier}")
                buffer.appendLine("\t\tSecondary identifier(s): ${assoc.association.secondaryIdentifiers.ifEmpty { null }?.joinToString(", ") ?: "(none)" } ")
                assoc.credentials.forEach {
                    buffer.appendLine("\t\t\tCredential: ${it.identities.ifEmpty { null }?.joinToString(" / ") ?: "(no usernames)" }: ${it.password}")
                }
                buffer.appendLine()
            }
            buffer.appendLine()
            userFile.writeBytes(Conversion.UTF8ToBytes(buffer.toString()))
            return filename
        }
        catch(e: AssertionError) {
            throw InternalException(InternalException.InternalExceptionCause.FILE_EXISTS, e)
        }
        catch(e: Exception) {
            throw FileException(FileException.FileExceptionCause.WRITE_ERROR, e)
        }
    }


}

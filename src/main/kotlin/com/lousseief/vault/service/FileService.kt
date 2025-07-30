package com.lousseief.vault.service

import com.lousseief.vault.crypto.Conversion
import com.lousseief.vault.exception.FileException
import com.lousseief.vault.exception.InternalException
import com.lousseief.vault.model.Profile
import com.lousseief.vault.model.Vault
import com.lousseief.vault.utils.OSPlatform
import java.io.File
import java.io.IOException
import java.time.Instant

object FileService {

    const val FILE_SUFFIX = ".vault"
    private var profilesLocation: String? = null

    fun getSettingsLocation(): String {
        // Check if the environment variable IS_DEVELOPMENT is set to "true"
        val isDevelopment = System.getenv("IS_DEVELOPMENT")?.lowercase() == "true"
        if (isDevelopment) {
            return "."
        }

        val vaultDirPath = if (OSPlatform.isMac) {
            // path should be ~/Library/Application Support/Vault/vault.settings
            System.getProperty("user.home") + "/Library/Application Support/Vault"
        } else if (OSPlatform.isWindows) {
            // path should be %appdata%\Vault\vault.settings
            val appData = System.getenv("APPDATA")
                ?: throw IllegalStateException("APPDATA environment variable not set on Windows")
            "$appData\\Vault"
        } else {
            // if none of the above, throw an exception
            throw UnsupportedOperationException("Unsupported operating system: ${OSPlatform.os}")
        }

        val vaultDir = File(vaultDirPath)
        if (!vaultDir.exists()) {
            val created = vaultDir.mkdirs()
            if (!created) {
                throw IllegalStateException("Failed to create directory: $vaultDirPath")
            }
        }
        return vaultDirPath
    }

    fun getCurrentProfilesLocation(): String {
        return profilesLocation ?: throw IllegalStateException("Profiles location must be set")
    }
    fun userExists(user: String): Boolean {
        return fileExists(getCurrentProfilesLocation(), user + FILE_SUFFIX)
    }

    fun programSettingsExists(): Boolean {
        return fileExists(getSettingsLocation(), "vault.settings")
    }

    fun fileExists(startDir: String, fileNameToLookFor: String): Boolean {
        // why can't you put the dot in the expression template instead of in the SUFFIX?'
        val currentDir = File(startDir)
        val currentDirContent = currentDir.list()
        if(currentDirContent !== null) {
            return currentDirContent.contains(fileNameToLookFor) && File("$startDir/$fileNameToLookFor").isFile
        }
        return false
    }

    fun readFile(user: String): Profile {
        if(!userExists(user)) {
            throw FileException(
                FileException.FileExceptionCause.NOT_FOUND,
                IOException("User doesn't exist (no .vault file was found)")
            )
        }
        return getCurrentProfilesLocation().let {
            try {
                val userFile = File("$it/$user$FILE_SUFFIX")
                val fileBytes = userFile.readBytes()
                val fileText = Conversion.bytesToUTF8(fileBytes) // content is Base64 but with line endings
                val parts = fileText.split("\n")
                assert(parts.size == 6, { "Expected .vault file to contain 6 parts but was ${parts.size}" })
                Profile(user, parts[0], parts[1], parts[2], parts[3], parts[4], parts[5])
            } catch (e: AssertionError) {
                throw FileException(FileException.FileExceptionCause.CORRUPT_FILE, e)
            } catch (e: Exception) {
                throw FileException(FileException.FileExceptionCause.READ_ERROR, e)
            }
        }
    }

    fun setupSystemSettings(): Boolean {
        if(!programSettingsExists()) {
            return false
        } else {
            try {
                val settingsFile = File("${getSettingsLocation()}/vault.settings")
                val fileBytes = settingsFile.readBytes()
                val fileText = Conversion.bytesToUTF8(fileBytes)
                val parts = fileText.split("\n")
                assert(parts.size == 1, { "Expected .settings file to contain 1 part but was ${parts.size}" })
                profilesLocation = parts[0]
                return true
            } catch (e: AssertionError) {
                throw FileException(FileException.FileExceptionCause.CORRUPT_FILE, e)
            } catch (e: Exception) {
                throw FileException(FileException.FileExceptionCause.READ_ERROR, e)
            }
        }
    }

    fun writeSystemSettingsFile(inputProfilesLocation: String) {
        profilesLocation = inputProfilesLocation
        try {
            val settingsFile = File("${getSettingsLocation()}/vault.settings")
            settingsFile.writeBytes(Conversion.UTF8ToBytes(inputProfilesLocation))
        }
        catch(e: AssertionError) {
            throw InternalException(InternalException.InternalExceptionCause.FILE_EXISTS, e)
        }
        catch(e: Exception) {
            throw FileException(FileException.FileExceptionCause.WRITE_ERROR, e)
        }
    }

    fun writeVaultFile(username: String, userData: String, overwrite: Boolean) {
        try {
            assert(overwrite || !userExists(username))
            getCurrentProfilesLocation().let {
                val userFile = File("$it/$username$FILE_SUFFIX")
                userFile.writeBytes(Conversion.UTF8ToBytes(userData))
            }
        }
        catch(e: AssertionError) {
            throw InternalException(InternalException.InternalExceptionCause.FILE_EXISTS, e)
        }
        catch(e: Exception) {
            throw FileException(FileException.FileExceptionCause.WRITE_ERROR, e)
        }
    }

    fun writeExportFile(username: String, directoryPath: String, vault: Vault): String {
        try {
            val filename = "$directoryPath/${username}_export_${Instant.now()}.txt"
            val userFile = File(filename)
            val buffer = StringBuffer()
            buffer.appendLine("VAULT EXPORT ${Instant.now()}")
            buffer.appendLine()
            buffer.appendLine("Settings:")
            buffer.appendLine("\tDefault password length: ${vault.first.passwordLength}")
            buffer.appendLine("\tPassword active time: ${vault.first.savePasswordForMinutes}")
            buffer.appendLine("\tCategories: ${vault.first.categories.ifEmpty { null }?.joinToString(",") ?: "no categories"}")
            buffer.appendLine("Data:")
            vault.second.entries.forEach { (key, assoc) ->
                buffer.appendLine("\t$key:")
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

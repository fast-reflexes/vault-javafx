package com.lousseief.vault.service

import com.lousseief.vault.crypto.Conversion
import com.lousseief.vault.exception.FileException
import com.lousseief.vault.exception.UserException
import com.lousseief.vault.model.Profile
import com.lousseief.vault.model.Vault
import com.lousseief.vault.utils.Log
import com.lousseief.vault.utils.OSPlatform
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.attribute.AclEntry
import java.nio.file.attribute.AclEntryFlag
import java.nio.file.attribute.AclEntryPermission
import java.nio.file.attribute.AclEntryType
import java.nio.file.attribute.AclFileAttributeView
import java.nio.file.attribute.PosixFilePermissions
import java.nio.file.attribute.UserPrincipal
import java.nio.file.attribute.UserPrincipalNotFoundException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.EnumSet

object FileService {

    const val FILE_SUFFIX = ".vault"
    const val USERNAME_MAX_LENGTH = 64

    /* User names become file names, so the accepted set is deliberately narrow: letters of either
    case, digits, hyphen and underscore. No dots and no separators means no path traversal is
    expressible. Case is accepted on input but not significant: names are lowercased before they
    ever reach the file system, so "Elias" and "elias" are the same user. */
    private val USERNAME_PATTERN = Regex("^[A-Za-z0-9_-]+$")

    /* Vault files and the settings file hold (encrypted) secrets and must not be readable by other
    local accounts. Exports hold cleartext passwords and matter even more. */
    private val OWNER_ONLY_FILE = PosixFilePermissions.fromString("rw-------")
    private val OWNER_ONLY_DIR = PosixFilePermissions.fromString("rwx------")

    /* Instant.toString() contains colons, which are not legal in Windows file names. */
    private val EXPORT_TIMESTAMP: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss").withZone(ZoneId.systemDefault())

    private var profilesLocation: String? = null

    fun normalizeUserName(rawName: String): String =
        rawName.trim().lowercase()

    fun isValidUserName(name: String): Boolean =
        name.length <= USERNAME_MAX_LENGTH && USERNAME_PATTERN.matches(name)

    /**
     * Normalizes a user supplied name and rejects anything that could escape the profiles directory
     * or produce a file name the OS can't represent. Returns the normalized name to use from here on.
     *
     * Call this once, where a name enters the application from user input (the login and register
     * views). Everything downstream takes the normalized result and does not re-check it.
     */
    fun validateUserName(rawName: String): String {
        val normalized = normalizeUserName(rawName)
        if (normalized.isEmpty())
            throw UserException(UserException.UserExceptionCause.EMPTY_USERNAME)
        if (normalized.length > USERNAME_MAX_LENGTH)
            throw UserException(UserException.UserExceptionCause.USERNAME_TOO_LONG)
        if (!USERNAME_PATTERN.matches(normalized))
            throw UserException(UserException.UserExceptionCause.INVALID_USERNAME)
        return normalized
    }

    private fun profileFile(validatedUserName: String): File =
        File(getCurrentProfilesLocation(), validatedUserName + FILE_SUFFIX)

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
            applyOwnerOnlyPermissions(vaultDir)
        }
        return vaultDirPath
    }

    fun getCurrentProfilesLocation(): String {
        return profilesLocation ?: throw IllegalStateException("Profiles location must be set")
    }

    /** @param validatedUserName a name that has already been through [validateUserName]. */
    fun userExists(validatedUserName: String): Boolean =
        profileFile(validatedUserName).isFile

    fun programSettingsExists(): Boolean {
        return fileExists(getSettingsLocation(), "vault.settings")
    }

    fun fileExists(startDir: String, fileNameToLookFor: String): Boolean {
        // why can't you put the dot in the expression template instead of in the SUFFIX?'
        val currentDirContent = File(startDir).list()
        if(currentDirContent !== null) {
            return currentDirContent.contains(fileNameToLookFor) && File("$startDir/$fileNameToLookFor").isFile
        }
        return false
    }

    /** @param validatedUserName a name that has already been through [validateUserName]. */
    fun readFile(validatedUserName: String): Profile {
        if (!userExists(validatedUserName)) {
            throw FileException(
                FileException.FileExceptionCause.NOT_FOUND,
                IOException("User doesn't exist (no .vault file was found)")
            )
        }
        /* resolved outside the try so that an IllegalStateException from a missing profiles location
        is not misreported as a corrupt file */
        val userFile = profileFile(validatedUserName)
        try {
            val fileBytes = userFile.readBytes()
            val fileText = Conversion.bytesToUTF8(fileBytes) // content is Base64 but with line endings
            val parts = fileText.split("\n")
            check(parts.size == 6) { "Expected .vault file to contain 6 parts but was ${parts.size}" }
            return Profile(validatedUserName, parts[0], parts[1], parts[2], parts[3], parts[4], parts[5])
        } catch (e: IllegalStateException) {
            throw FileException(FileException.FileExceptionCause.CORRUPT_FILE, e)
        } catch (e: Exception) {
            throw FileException(FileException.FileExceptionCause.READ_ERROR, e)
        }
    }

    fun setupSystemSettings(): Boolean {
        if(!programSettingsExists()) {
            return false
        }
        // resolved outside the try, see readFile
        val settingsFile = File("${getSettingsLocation()}/vault.settings")
        try {
            val fileBytes = settingsFile.readBytes()
            val fileText = Conversion.bytesToUTF8(fileBytes)
            val parts = fileText.split("\n")
            check(parts.size == 1) { "Expected .settings file to contain 1 part but was ${parts.size}" }
            profilesLocation = parts[0]
            return true
        } catch (e: IllegalStateException) {
            throw FileException(FileException.FileExceptionCause.CORRUPT_FILE, e)
        } catch (e: Exception) {
            throw FileException(FileException.FileExceptionCause.READ_ERROR, e)
        }
    }

    fun writeSystemSettingsFile(inputProfilesLocation: String) {
        require(inputProfilesLocation.isNotBlank()) { "The profiles location must not be blank" }
        profilesLocation = inputProfilesLocation
        val settingsFile = File("${getSettingsLocation()}/vault.settings")
        try {
            writeRestrictedBytes(settingsFile, Conversion.UTF8ToBytes(inputProfilesLocation))
        }
        catch(e: Exception) {
            throw FileException(FileException.FileExceptionCause.WRITE_ERROR, e)
        }
    }

    /** @param validatedUserName a name that has already been through [validateUserName]. */
    fun writeVaultFile(validatedUserName: String, userData: String, overwrite: Boolean) {
        check(overwrite || !userExists(validatedUserName)) {
            "Refusing to overwrite the existing .vault file for '$validatedUserName' without the overwrite flag"
        }
        val userFile = profileFile(validatedUserName)
        try {
            writeRestrictedBytes(userFile, Conversion.UTF8ToBytes(userData))
        }
        catch(e: Exception) {
            throw FileException(FileException.FileExceptionCause.WRITE_ERROR, e)
        }
    }

    /** @param validatedUserName a name that has already been through [validateUserName]. */
    fun writeExportFile(validatedUserName: String, directoryPath: String, vault: Vault): String {
        require(directoryPath.isNotBlank()) { "The export directory must not be blank" }
        val timestamp = EXPORT_TIMESTAMP.format(Instant.now())
        val userFile = File(directoryPath, "${validatedUserName}_export_$timestamp.txt")
        try {
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
            writeRestrictedBytes(userFile, Conversion.UTF8ToBytes(buffer.toString()))
            return userFile.path
        }
        catch(e: Exception) {
            throw FileException(FileException.FileExceptionCause.WRITE_ERROR, e)
        }
    }

    /**
     * Writes a file that only the current user may read or write. On POSIX systems the permissions
     * are applied at creation time so the content is never briefly world readable; on Windows, where
     * the POSIX view is unavailable, we fall back to the java.io.File permission flags.
     */
    private fun writeRestrictedBytes(file: File, bytes: ByteArray) {
        if (!Files.exists(file.toPath())) {
            createOwnerOnlyFile(file)
        } else {
            /* a .vault file copied in by hand - which the change-profiles-location flow explicitly asks
            the user to do - carries whatever permissions the copy gave it, and writing to an existing
            file leaves its mode alone, so owner-only is (re)applied on every write */
            applyOwnerOnlyPermissions(file)
        }
        file.writeBytes(bytes)
    }

    private fun createOwnerOnlyFile(file: File) {
        val path = file.toPath()
        try {
            // the permissions are part of the creation, so the content is never briefly world readable
            Files.createFile(path, PosixFilePermissions.asFileAttribute(OWNER_ONLY_FILE))
            return
        } catch (e: UnsupportedOperationException) {
            // no POSIX view, i.e. Windows: create plainly and restrict below instead
        } catch (e: IOException) {
            Log.debug { "could not create ${file.name} with owner-only permissions: ${e.message}" }
        }
        // a file must exist for the write to follow, so this failing is a genuine error
        if (!Files.exists(path)) {
            Files.createFile(path)
        }
        applyOwnerOnlyPermissions(file)
    }

    /**
     * Restricts a file or directory to its owner. This is best effort on purpose: a file system that
     * cannot express these permissions must never stop the user from saving their vault.
     */
    private fun applyOwnerOnlyPermissions(fileOrDirectory: File) {
        /* deliberately catches everything: restricting permissions is a hardening step, and failing it
        must never be the reason the user cannot save their vault */
        try {
            try {
                Files.setPosixFilePermissions(
                    fileOrDirectory.toPath(),
                    if (fileOrDirectory.isDirectory) OWNER_ONLY_DIR else OWNER_ONLY_FILE
                )
                return
            } catch (e: UnsupportedOperationException) {
                // no POSIX view, i.e. Windows: fall through to ACLs below
            }
            restrictToOwnerViaAcl(fileOrDirectory)
        } catch (e: Exception) {
            Log.debug { "could not restrict permissions on ${fileOrDirectory.name}: $e" }
        }
    }

    /**
     * The Windows equivalent of owner-only POSIX permissions: replaces the ACL with entries that grant
     * full control to us and to nobody else, which also drops any permissive entry inherited from the
     * parent directory.
     *
     * java.io.File's permission methods are useless here - on Windows they map to DOS attributes only,
     * so setReadable does nothing at all and ownerOnly is ignored - which is why this goes through
     * AclFileAttributeView instead.
     */
    private fun restrictToOwnerViaAcl(fileOrDirectory: File) {
        val path = fileOrDirectory.toPath()
        val aclView = Files.getFileAttributeView(path, AclFileAttributeView::class.java)
        if (aclView === null) {
            Log.debug { "${fileOrDirectory.name}: neither POSIX permissions nor ACLs are supported" }
            return
        }
        /* grant to the account running the application AND to the file's owner: on Windows those can
        differ (a file created by an elevated process ends up owned by Administrators), and granting
        only one of them risks locking the other out of the vault entirely */
        val principals = LinkedHashSet<UserPrincipal>()
        try {
            principals.add(
                path.fileSystem.userPrincipalLookupService
                    .lookupPrincipalByName(System.getProperty("user.name"))
            )
        } catch (e: UserPrincipalNotFoundException) {
            Log.debug { "could not resolve the current user as a principal: ${e.message}" }
        }
        Files.getOwner(path)?.let { principals.add(it) }
        if (principals.isEmpty()) {
            Log.debug { "${fileOrDirectory.name}: no principal to grant access to, ACL left alone" }
            return
        }
        // a directory must pass the restriction on to whatever is created inside it
        val flags =
            if (fileOrDirectory.isDirectory)
                setOf(AclEntryFlag.FILE_INHERIT, AclEntryFlag.DIRECTORY_INHERIT)
            else
                emptySet()
        aclView.acl = principals.map { principal ->
            AclEntry.newBuilder()
                .setType(AclEntryType.ALLOW)
                .setPrincipal(principal)
                .setPermissions(EnumSet.allOf(AclEntryPermission::class.java))
                .setFlags(flags)
                .build()
        }
        Log.debug { "restricted the ACL on ${fileOrDirectory.name} to $principals" }
    }
}

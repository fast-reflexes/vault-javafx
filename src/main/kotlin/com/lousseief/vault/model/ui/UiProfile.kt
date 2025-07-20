package com.lousseief.vault.model

import com.lousseief.vault.crypto.Conversion
import com.lousseief.vault.crypto.Hmac
import com.lousseief.vault.exception.AuthenticationException
import com.lousseief.vault.exception.InternalException
import com.lousseief.vault.model.ui.UiAssociation
import com.lousseief.vault.model.ui.UiPasswordData
import com.lousseief.vault.model.ui.UiSettings
import com.lousseief.vault.service.*
import javafx.beans.property.SimpleMapProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections

class UiProfile(
    val name: SimpleStringProperty,
    var keyMaterialSalt: String,
    var verificationSalt: String,
    var verificationHash: String,
    var iv: String,
    var encryptedData: String,
    var checkSum: String = "",
    var settings: UiSettings = UiSettings(),
    var userNames: SimpleMapProperty<String, Int> = SimpleMapProperty(FXCollections.observableHashMap()),
    val associations: SimpleMapProperty<String, UiAssociation> = SimpleMapProperty(FXCollections.observableHashMap()),
    val passwordData: UiPasswordData,
) {

    companion object {
        fun fromProfile(
            user: Profile,
            associations: Map<String, Association>,
            settings: Settings,
            userNames: Map<String, Int>,
            password: String
        ): UiProfile {
            val profile = UiProfile(
                name = SimpleStringProperty(user.name),
                keyMaterialSalt = user.keyMaterialSalt,
                verificationSalt = user.verificationSalt,
                verificationHash = user.verificationHash,
                iv = user.iv,
                encryptedData = user.encryptedData,
                checkSum = user.checkSum,
                settings = UiSettings.fromSettings(settings),
                userNames = SimpleMapProperty(
                    FXCollections.observableHashMap<String, Int>().apply { putAll(userNames) }
                ),
                associations = SimpleMapProperty(FXCollections.observableHashMap<String, UiAssociation>().apply {
                    putAll(associations.mapValues { (key, value) -> UiAssociation.fromAssociation(value) })
                }),
                passwordData = UiPasswordData()
            )
            profile.setPassword(password)
            return profile
        }
    }

    fun containsChange() {
        associations.any {
            val aP = it.value.savedAssociation
            val a = it.value
            aP.mainIdentifier != a.mainIdentifier.value
                || aP.category != a.category.value
        }
    }

    fun setPassword(nextPassword: String?) {
        if(nextPassword === null) {
            passwordData.cancelSavedMasterPassword()
        }
        else if (settings.savePasswordForMinutes.value > 0) {
            passwordData.resetSavedMasterPassword(nextPassword, settings.savePasswordForMinutes.value)
        }
    }

    fun toContentString(): String =
        "${keyMaterialSalt}\n${verificationSalt}\n${verificationHash}\n${iv}\n${encryptedData}"


    override fun toString(): String =
        "${toContentString()}\n${checkSum}"

    fun passwordRequiredAction(requireFreshPassword: Boolean = false): String? {
        return passwordData.passwordRequiredAction(this, settings.savePasswordForMinutes.value, requireFreshPassword)
    }

    fun add(identifier: String, password: String): Association {
        val associationToAdd = Association(mainIdentifier = identifier)
        accessVault(
            password,
            { (settings, associations) ->
                associations[identifier] = AssociationWithCredentials(association = associationToAdd)
                Pair(settings, associations)
            },
            true
        )
        return associationToAdd
    }

    fun addEntry(name: String, password: String?) {
        if(password === null || password.length == 0)
            throw AuthenticationException(AuthenticationException.AuthenticationExceptionCause.EMPTY_PASSWORDS_NOT_ALLOWED, null)
        val addedAssociation = add(name, password)
        //val addition = Association()
        //addition.mainIdentifier = name
        //user!!.associations.put(name, addition)
        associations.put(name, UiAssociation.fromAssociation(addedAssociation))
        //items.sortWith { a, b -> a.mainIdentifierProperty.compareTo(b.mainIdentifierProperty) }
        //altered.set(true)
    }

    fun accessVault(
        password: String,
        vaultManipulation: ((vault: MutableVault) -> Vault)? = null,
        encrypt: Boolean = false,
        updatedPassword: String = password
    ): Vault =

        VerificationService.authorize(
            password, keyMaterialSalt, verificationHash, verificationSalt
        )
            .let {
                Pair(it.sliceArray(0 until 32), it.sliceArray(32 until 64))
            }
            .also {
                    (_, hMacKeyBytes) ->
                VerificationService.verify(hMacKeyBytes, toContentString(), checkSum)
            }
            .let {
                    (encryptionKeyBytes, hMacKeyBytes) ->
                VaultService.decryptVault(encryptedData, iv, encryptionKeyBytes)
                    .let { if (vaultManipulation !== null) vaultManipulation(it) else it }
                    .let { vault ->
                        if (encrypt) {
                            var encryptionKeyBytesToUse = encryptionKeyBytes
                            var hmacKeyBytesToUse = hMacKeyBytes
                            if(password != updatedPassword) {
                                val (
                                    updatedSaltBytes,
                                    updatedHashBytes,
                                    updatedHashSalt,
                                    updatedEncryptionKeyBytes,
                                    updatedHmacKeyBytes
                                ) = UserService.createKeyMaterial(updatedPassword)
                                keyMaterialSalt = Conversion.bytesToBase64(updatedSaltBytes)
                                verificationHash = Conversion.bytesToBase64(updatedHashBytes)
                                verificationSalt = Conversion.bytesToBase64(updatedHashSalt)
                                encryptionKeyBytesToUse = updatedEncryptionKeyBytes
                                hmacKeyBytesToUse = updatedHmacKeyBytes
                            }
                            val (nextIv, nextCipherText) = VaultService.encryptVault(encryptionKeyBytesToUse, vault)
                            iv = nextIv
                            encryptedData = nextCipherText
                            checkSum = Conversion.bytesToBase64(
                                Hmac.generateMac(
                                    Conversion.UTF8ToBytes(toContentString()),
                                    hmacKeyBytesToUse
                                ))
                        }
                        vault
                    }
            }

    fun addUsername(newUserName: String) {
        if (userNames.containsKey(newUserName))
            userNames[newUserName] = userNames[newUserName]!!.plus(1)
        else
            userNames[newUserName] = 1
    }

    fun removeUsername(userNameToRemove: String) {
        if(!userNames.containsKey(userNameToRemove))
            throw InternalException(InternalException.InternalExceptionCause.USERNAME_TO_REMOVE_NOT_FOUND)
        if(userNames[userNameToRemove]!! <= 0)
            throw InternalException(InternalException.InternalExceptionCause.USERNAME_TO_REMOVE_ZERO_OR_LESS)
        userNames[userNameToRemove] = userNames[userNameToRemove]!!.minus(1)
        if(userNames[userNameToRemove]!!.compareTo(0) == 0)
            userNames.remove(userNameToRemove)
    }

    /*fun initialize(password: String): MutableMap<String, Association> {
        val (fetchedSettings, fetchedAssociationsWithCredentials) = accessVault(password)
        settings = fetchedSettings
        val associations = fetchedAssociationsWithCredentials.mapValues { it.value.association }.toMutableMap()
        userNames = fetchedAssociationsWithCredentials
            .map {
                it.value.credentials
                    .map { it.identities }
                    .flatten()
            }
            .flatten()
            .groupBy { it }
            .mapValues { it.value.size }
            .toMutableMap()
        //accessVault(password)
        //    .also {(settings, associations) ->
        //        this.settings = settings
        //        this.associations = associations.mapValues { it.value.association }.toMutableMap()
        //    }
        return associations
    }*/

    /*fun getCredentials(identifier: String, password: String): List<Credential> {
        val (_, fetchedAssociationsWithCredentials) = accessVault(password)
        return fetchedAssociationsWithCredentials.getOrElse(
            identifier,
            { throw InternalException(InternalException.InternalExceptionCause.MISSING_IDENTIFIER) }
        ).credentials
    }*/
    fun save() {
        updateAssociations()
        FileService.writeVaultFile(this.name.value, this.toString(), true)
    }

    fun export(vault: Vault): String {
        return FileService.writeExportFile(this.name.value, vault)
    }

    /*fun updateAssociation(oldIdentifier: String, assoc: Association, password: String, newIdentifier: String = oldIdentifier) {
       accessVault(
           password,
           { (_, associations) ->
               val existingAssociation = associations[oldIdentifier]
               if(existingAssociation === null)
                   throw Exception("CRAZYS!")
               if(oldIdentifier !== newIdentifier)
                   associations.remove(oldIdentifier)
               associations[newIdentifier] = existingAssociation.copy(association = assoc)
               Pair(this.settings, associations)
           },
           true
       )

   }*/

    fun updateAssociations() {
        passwordRequiredAction(true)?.let { password ->
            accessVault(
                password,
                { (previousSettings, previousVaultData) ->
                    val out = mutableMapOf<String, AssociationWithCredentials>()
                    associations.value.forEach { key, value ->
                        val ass = value.toAssociation()
                        out[ass.mainIdentifier] = AssociationWithCredentials(
                            credentials = previousVaultData[key]?.credentials ?: emptyList(),
                            association = ass
                        )
                    }
                    Pair(settings.toSettings(), out)
                },
                true
            )
        }
    }

    fun updateCredentials(identifier: String, credentials: List<Credential>, password: String) {
        accessVault(
            password,
            { (settings, associations) ->
                val existingAssociation = associations[identifier]
                if(existingAssociation === null) {
                    println(identifier)
                    println(associations.keys)
                    throw Exception("CRAZYS!CREDS")
                }
                associations[identifier] = existingAssociation.copy(credentials = credentials)
                Pair(settings, associations)
            },
            true
        )
    }

    /*fun remove(identifier: String, password: String) {
        accessVault(
            password,
            { (settings, associations) ->
                Pair(settings, associations.filterKeys { !it.equals(identifier) })
            },
            true)
    }*/

    /*fun add(identifier: String, password: String): Association {
        val associationToAdd = Association(mainIdentifier = identifier)
        accessVault(
            password,
            { (settings, associations) ->
                associations[identifier] = AssociationWithCredentials(association = associationToAdd)
                Pair(settings, associations)
            },
            true
        )
        return associationToAdd
    }*/
}

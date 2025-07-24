package com.lousseief.vault.model.ui

import com.lousseief.vault.crypto.Conversion
import com.lousseief.vault.crypto.Hmac
import com.lousseief.vault.exception.InternalException
import com.lousseief.vault.model.Association
import com.lousseief.vault.model.AssociationWithCredentials
import com.lousseief.vault.model.Credential
import com.lousseief.vault.model.MutableVault
import com.lousseief.vault.model.Profile
import com.lousseief.vault.model.Settings
import com.lousseief.vault.model.Vault
import com.lousseief.vault.service.*
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleMapProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList

class UiProfile(
    val savedProfile: Profile,
    val name: SimpleStringProperty,
    var keyMaterialSalt: String,
    var verificationSalt: String,
    var verificationHash: String,
    var iv: String,
    var encryptedData: String,
    var checkSum: String = "",
    var settings: UiSettings,
    var userNames: SimpleMapProperty<String, Int> = SimpleMapProperty(FXCollections.observableHashMap()),
    val associations: SimpleMapProperty<String, UiAssociation> = SimpleMapProperty(FXCollections.observableHashMap()),
    val orderedAssociations: ObservableList<UiAssociation>,
    val passwordData: UiPasswordData,
) {

    companion object {
        fun fromProfile(
            user: Profile,
            inputAssociations: Map<String, Association>,
            settings: Settings,
            userNames: Map<String, Int>,
            password: String
        ): UiProfile {
            val associations = SimpleMapProperty(FXCollections.observableHashMap<String, UiAssociation>().apply {
                putAll(inputAssociations.mapValues { (key, value) -> UiAssociation.fromAssociation(value) })
            })
            val profile = UiProfile(
                savedProfile = user,
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
                associations = associations,
                passwordData = UiPasswordData(),
                orderedAssociations = FXCollections.observableList<UiAssociation>(
                mutableListOf(),
                { assoc -> arrayOf(
                    assoc.mainIdentifier,
                    assoc.secondaryIdentifiers,
                    assoc.category,
                    assoc.comment,
                    assoc.shouldBeDeactivated,
                    assoc.isNeeded,
                    assoc.isDeactivated
                )}
                ).apply {
                    addAll(associations.value.values)
                    sortWith { a, b -> a.mainIdentifier.value.compareTo(b.mainIdentifier.value) }
                    addListener(ListChangeListener {
                        // TODO maybe trigger sorting here instead of doing it manually from places
                        println("hej")
                        /*if (c.next()) {
                                                println(c.getFrom())
                                            }*/
                    })
                }
            )
            profile.setPassword(password)
            return profile
        }
    }

    val nonObservablePropertyChanged = SimpleBooleanProperty(false)
    val isDirty = Bindings.createBooleanBinding(
        {
            println("Triggered reevaluation of dirty flag in UiProfile")
            containsChange() || nonObservablePropertyChanged.value
        },
        associations, nonObservablePropertyChanged, settings.isDirty, orderedAssociations
    )

    fun containsChange(): Boolean {
        val sP = savedProfile
        return (
            keyMaterialSalt != sP.keyMaterialSalt
                || verificationHash != sP.verificationHash
                || verificationSalt != sP.verificationSalt
                || iv != sP.iv
                || encryptedData != sP.encryptedData
                || checkSum != sP.checkSum
                || associations.values.any { it.containsChange() }
                || settings.isDirty.value
        )
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

    fun addAssociation(identifier: String, password: String) {
        val associationToAdd = Association(mainIdentifier = identifier)
        accessVault(
            password,
            { (settings, associations) ->
                associations[identifier] = AssociationWithCredentials(association = associationToAdd)
                Pair(settings, associations)
            },
            true
        )
        associations.put(identifier, UiAssociation.fromAssociation(associationToAdd))
        //items.sortWith { a, b -> a.mainIdentifierProperty.compareTo(b.mainIdentifierProperty) }
        //altered.set(true)
    }

    fun removeAssociation(savedMainIdentifier: String, password: String) {
        accessVault(
            password,
            { (settings, associations) ->
                val assoc = associations[savedMainIdentifier]
                assoc?.credentials?.forEach {
                    it.identities.forEach {
                        removeUsername(it)
                    }
                }
                associations.remove(savedMainIdentifier)
                Pair(settings, associations)
            },
            true
        )
        associations.remove(savedMainIdentifier)
    }

    fun accessVault(
        password: String,
        vaultManipulation: ((vault: MutableVault) -> Vault)? = null,
        encrypt: Boolean = false,
        updatedPassword: String = password,
        requireNewIv: Boolean = false,
    ): Vault {
        return VerificationService.authorize(
            password, keyMaterialSalt, verificationHash, verificationSalt
        )
            .let {
                Pair(it.sliceArray(0 until 32), it.sliceArray(32 until 64))
            }
            .also { (_, hMacKeyBytes) ->
                VerificationService.verify(hMacKeyBytes, toContentString(), checkSum)
            }
            .let { (encryptionKeyBytes, hMacKeyBytes) ->
                VaultService.decryptVault(encryptedData, iv, encryptionKeyBytes)
                    .let { if (vaultManipulation !== null) vaultManipulation(it) else it }
                    .let { vault ->
                        if (encrypt) {
                            var encryptionKeyBytesToUse = encryptionKeyBytes
                            var hmacKeyBytesToUse = hMacKeyBytes
                            if (password != updatedPassword) {
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
                            val (nextIv, nextCipherText) = VaultService.encryptVault(
                                encryptionKeyBytesToUse,
                                vault,
                                if (requireNewIv) null else iv
                            )
                            iv = nextIv
                            encryptedData = nextCipherText
                            checkSum = Conversion.bytesToBase64(
                                Hmac.generateMac(
                                    Conversion.UTF8ToBytes(toContentString()),
                                    hmacKeyBytesToUse
                                )
                            )
                            println("NEW")
                            println(encryptedData)
                            println()
                            println("OLD")
                            println(savedProfile.encryptedData)
                            println("IV differs: ${savedProfile.iv != iv}")
                            println("Verification hash differs: ${savedProfile.verificationHash != verificationHash}")
                            println("Verification salt differs: ${savedProfile.verificationSalt != verificationSalt}")
                            println("Encryption salt differs: ${savedProfile.keyMaterialSalt != keyMaterialSalt}")
                            println("Encrypted data differs: ${savedProfile.encryptedData != encryptedData}")
                            // TODO investigate why this doesn't trigger binding without manually checking it
                            //println("Manual flag set: ${nonObservablePropertyChanged.value}") // needed why?
                            /*println("Checksum differs: ${savedProfile.checkSum != checkSum}")
                            associations.forEach { (identifier, association) ->
                                if (association.containsChange()) {
                                    println("Association witbh identifier $identifier contains a change")
                                }
                            }*/
                            nonObservablePropertyChanged.set(containsChange())
                            //println("Manual flag set (after): ${nonObservablePropertyChanged.value}")
                        }
                        vault
                    }
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

    /*fun getCredentials(identifier: String, password: String): List<Credential> {
        val (_, fetchedAssociationsWithCredentials) = accessVault(password)
        return fetchedAssociationsWithCredentials.getOrElse(
            identifier,
            { throw InternalException(InternalException.InternalExceptionCause.MISSING_IDENTIFIER) }
        ).credentials
    }*/
    fun save() {
        persistUpdatedInMemoryAssociationsToEncryptedDataBeforePersistingVaultToDisk()
        FileService.writeVaultFile(this.name.value, this.toString(), true)
    }

    fun export(vault: Vault): String {
        return FileService.writeExportFile(this.name.value, vault)
    }

    private fun persistUpdatedInMemoryAssociationsToEncryptedDataBeforePersistingVaultToDisk() {
        passwordRequiredAction(true)?.let { password ->
            accessVault(
                password,
                { (_, previousVaultData) ->
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
                true,
                password,
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

}

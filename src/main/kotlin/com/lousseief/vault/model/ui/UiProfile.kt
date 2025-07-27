package com.lousseief.vault.model.ui

import com.lousseief.vault.exception.InternalException
import com.lousseief.vault.model.Association
import com.lousseief.vault.model.AssociationWithCredentials
import com.lousseief.vault.model.Credential
import com.lousseief.vault.model.IProfile
import com.lousseief.vault.model.MutableVault
import com.lousseief.vault.model.Profile
import com.lousseief.vault.model.Settings
import com.lousseief.vault.model.Vault
import com.lousseief.vault.service.*
import com.lousseief.vault.utils.sortInPlaceByMainIdentifier
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleMapProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.MapChangeListener
import javafx.collections.ObservableList
import kotlin.collections.set

class UiProfile(
    var savedProfile: Profile,
    val name: SimpleStringProperty,
    keyMaterialSalt: String,
    verificationSalt: String,
    verificationHash: String,
    iv: String,
    encryptedData: String,
    checkSum: String = "",
    val settings: UiSettings,
    var userNames: SimpleMapProperty<String, Int>,
    val associations: SimpleMapProperty<String, UiAssociation>,
    val orderedAssociations: ObservableList<UiAssociation>,
    val persistedAssociations: MutableMap<String, Association>,
    val passwordData: UiPasswordData,
): IProfile(
   keyMaterialSalt,
    verificationSalt,
    verificationHash,
    iv,
    encryptedData,
    checkSum
) {

    companion object {
        fun fromProfile(
            user: Profile,
            inputAssociations: MutableMap<String, Association>,
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
                persistedAssociations = inputAssociations,
                passwordData = UiPasswordData(),
                orderedAssociations = FXCollections.observableList<UiAssociation>(
                mutableListOf(),
                { assoc ->
                    arrayOf(
                        assoc.mainIdentifier,
                        assoc.secondaryIdentifiers,
                        assoc.category,
                        assoc.comment,
                        assoc.shouldBeDeactivated,
                        assoc.isNeeded,
                        assoc.isDeactivated
                    )
                }).apply {
                    addAll(associations.value.values)
                    sortInPlaceByMainIdentifier()
                }
            )
            profile.setPassword(password)
            return profile
        }

        const val DEBUG = false
    }

    val isDirty = SimpleBooleanProperty(false)

    init {
        associations.addListener(MapChangeListener {
            println("Change in associations")
            reevaluateDirtyFlag()
        })
        orderedAssociations.addListener(ListChangeListener {
            println("Change in orderedAssociations")
            reevaluateDirtyFlag()
        })
        settings.isDirty.addListener {
            println("Settings change")
            reevaluateDirtyFlag()
        }
    }

    private fun printDirtyFlagTrace() {
        println("IV differs: ${savedProfile.iv != iv}")
        println("Verification hash differs: ${savedProfile.verificationHash != verificationHash}")
        println("Verification salt differs: ${savedProfile.verificationSalt != verificationSalt}")
        println("Encryption salt differs: ${savedProfile.keyMaterialSalt != keyMaterialSalt}")
        println("Encrypted data differs: ${savedProfile.encryptedData != encryptedData}")
        println("Checksum differs: ${savedProfile.checkSum != checkSum}")
        println("Settings is dirty: ${settings.isDirty.value}")
        println(
            "Associations are dirty: ${
                associations.values.any {
                    it.containsChange(
                        persistedAssociations[it.mainIdentifier.value]
                    )
                }
            }"
        )
    }
    fun reevaluateDirtyFlag() {
        println("Running evaluation of dirty flag")
        val sP = savedProfile
        val isCurrentlyDirty = (
            keyMaterialSalt != sP.keyMaterialSalt
                || verificationHash != sP.verificationHash
                || verificationSalt != sP.verificationSalt
                || iv != sP.iv
                || encryptedData != sP.encryptedData
                || checkSum != sP.checkSum
                || associations.values.any {
                    it.containsChange(persistedAssociations[it.mainIdentifier.value])
                }
                || settings.isDirty.value
        )
        if(DEBUG) {
            printDirtyFlagTrace()
        }
        isDirty.set(isCurrentlyDirty)
    }

    fun accessVault(
        password: String,
        vaultManipulation: ((vault: MutableVault) -> Vault)? = null,
        encrypt: Boolean = false,
        updatedPassword: String = password,
        requireNewIv: Boolean = false
    ): Vault {
        return accessVault(password, vaultManipulation, encrypt, updatedPassword, requireNewIv, ::reevaluateDirtyFlag)
    }

    fun setPassword(nextPassword: String?) {
        if(nextPassword === null) {
            passwordData.cancelSavedMasterPassword()
        }
        else if (settings.savePasswordForMinutes.value > 0) {
            passwordData.resetSavedMasterPassword(nextPassword, settings.savePasswordForMinutes.value)
        }
    }

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
        orderedAssociations.add(associations[identifier])
        orderedAssociations.sortInPlaceByMainIdentifier()
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
        orderedAssociations.removeIf { it.mainIdentifier.value == savedMainIdentifier }
    }

    fun updateAssociationMainIdentifier(oldIdentifier: String, newIdentifier: String, password: String) {
        accessVault(
            password,
            { (settings, associations) ->
                val association = associations[oldIdentifier]
                    ?: throw InternalException(InternalException.InternalExceptionCause.MISSING_IDENTIFIER)
                associations[newIdentifier] = association
                associations.remove(oldIdentifier)
                Pair(settings, associations)
            },
            true
        )
        val association = associations[oldIdentifier]
            ?: throw InternalException(InternalException.InternalExceptionCause.MISSING_IDENTIFIER)
        association.mainIdentifier.set(newIdentifier)
        associations.remove(oldIdentifier)
        associations[newIdentifier] = association
        orderedAssociations.sortInPlaceByMainIdentifier()
    }

    fun addUsername(newUserName: String) {
        val nextValue = userNames[newUserName]?.let { it + 1 } ?: 1
        userNames[newUserName] = nextValue
    }

    fun removeUsername(userNameToRemove: String) {
        val currentValue = userNames[userNameToRemove]
            ?: throw InternalException(InternalException.InternalExceptionCause.USERNAME_TO_REMOVE_NOT_FOUND)
        if(currentValue <= 0)
            throw InternalException(InternalException.InternalExceptionCause.USERNAME_TO_REMOVE_ZERO_OR_LESS)
        val nextValue = currentValue - 1
        userNames[userNameToRemove] = nextValue
        if(nextValue == 0) {
            userNames.remove(userNameToRemove)
        }
    }

    fun getCredentials(savedIdentifier: String, password: String): Pair<List<Credential>, List<UiCredential>> {
        val vault = accessVault(password)
        val credentials = (vault.second[savedIdentifier]?.credentials)?.toMutableList()
            ?: throw InternalException(InternalException.InternalExceptionCause.MISSING_IDENTIFIER)
                .apply { println("searching for ${savedIdentifier}, found ${associations.keys}") }
        val uiCredentials = credentials.map(UiCredential::fromCredentials)
        return credentials to uiCredentials
    }

    fun save() {
        persistUpdatedInMemoryAssociationsToEncryptedDataBeforePersistingVaultToDisk()
        reevaluateDirtyFlag()
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
            FileService.writeVaultFile(this.name.value, this.toString(), true)

            // get the user data from file anew
            val freshUser = UserService.loadUser(name.value)
            val (freshAssociations, freshSettings, freshUserNames) = freshUser.initialize(password)

            /* we could just reload the view, but this creates an ugly glitch, so let's instead replace and update the
            existing view */
            //val freshUiProfile = fromProfile(freshUser, freshAssociations, freshSettings, freshUserNames, password)
            //passwordData.cancelSavedMasterPassword()
            //onSaved(freshUiProfile)

            // update existing data
            savedProfile = freshUser
            settings.savedSettings = freshSettings

            val userNamesToRemove = userNames.keys.toMutableSet() - freshUserNames.keys
            freshUserNames.forEach { (key, value) -> userNames[key] = value }
            userNamesToRemove.forEach { userNames.remove(it) }

            // must replace associations in existing structures
            val associationKeysPresent = freshAssociations.keys
            val associationsToRemove = associations.keys.toMutableSet() - associationKeysPresent
            freshAssociations.forEach { (key, value) ->
                val uiAssociation = UiAssociation.fromAssociation(value)
                associations[key] = uiAssociation
                val indexOfOrderedAssociations = orderedAssociations.indexOfFirst { it.mainIdentifier.value == key }
                if(indexOfOrderedAssociations != -1) {
                    orderedAssociations.set(indexOfOrderedAssociations, uiAssociation)
                } else {
                    orderedAssociations.add(uiAssociation)
                }
            }
            associationsToRemove.forEach { key ->
                associations.remove(key)
                val indexOfOrderedAssociations = orderedAssociations.indexOfFirst { it.mainIdentifier.value == key }
                if(indexOfOrderedAssociations != -1) {
                    orderedAssociations.removeAt(indexOfOrderedAssociations)
                }
            }
            orderedAssociations.sortInPlaceByMainIdentifier()

            persistedAssociations.clear()
            persistedAssociations.putAll(freshAssociations)

            printDirtyFlagTrace()
        }
    }

    fun updateCredentials(identifier: String, credentials: List<Credential>, password: String) {
        accessVault(
            password,
            { (settings, associations) ->
                val existingAssociation = associations[identifier]
                    ?: throw InternalException(InternalException.InternalExceptionCause.MISSING_IDENTIFIER)
                        .apply { println("searching for ${identifier}, found ${associations.keys}") }
                associations[identifier] = existingAssociation.copy(credentials = credentials)
                Pair(settings, associations)
            },
            true
        )
    }

}

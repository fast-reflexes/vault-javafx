package com.lousseief.vault.controller

import com.lousseief.vault.dialog.AddPasswordDialog
import com.lousseief.vault.dialog.AddUsernameDialog
import com.lousseief.vault.model.UiProfile
import com.lousseief.vault.model.ui.UiCredential
import com.lousseief.vault.utils.copySelectionToClipboard
import com.lousseief.vault.utils.timeToStringDate
import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleStringProperty
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.ButtonBar
import javafx.scene.control.ButtonType
import javafx.scene.control.CheckBox
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.control.PasswordField
import javafx.scene.control.TextField
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCombination
import javafx.scene.layout.ColumnConstraints
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import java.time.Instant

class CredentialController(
    val user: UiProfile,
    val credential: UiCredential,
    val onDeleteCredential: () -> Unit,
) {

    @FXML
    private lateinit var copyPasswordButton: Button

    @FXML
    private lateinit var updatePasswordButton: Button

    @FXML
    private lateinit var middleButtonPane: GridPane

    @FXML
    private lateinit var addUsernameButton: Button

    @FXML
    private lateinit var removeUsernameButton: Button

    @FXML
    private lateinit var lowerButtonPane: GridPane

    @FXML
    private lateinit var associatedUsernamesList: ListView<String>

    @FXML
    private lateinit var createdLabel: Label

    @FXML
    private lateinit var lastUpdatedLabel: Label

    @FXML
    private lateinit var deleteCredentialsButton: Button

    @FXML
    private lateinit var passwordInputHolder: VBox

    @FXML
    private lateinit var showPasswordCheckbox: CheckBox

    @FXML
    private lateinit var credentialContainer: StackPane

    private val passwordProperty = SimpleStringProperty()
    private val identity: SimpleStringProperty = SimpleStringProperty()

    private val lastUpdatedString = Bindings.createStringBinding(
        { timeToStringDate(credential.lastUpdated.value) }
        , credential.lastUpdated
    )
    private val createdString = Bindings.createStringBinding(
        { timeToStringDate(credential.created.value) }
        , credential.created
    )

    private fun onChangePassword() {
        val newPassword = AddPasswordDialog(
            user.settings.passwordLength.value,
            { input: String?, _: ActionEvent ->
                if (input === null || input.isEmpty()) {
                    throw Exception("Empty password is neither advisable nor allowed.")
                }
            },
            "Generate or enter a new password for your credential"
        ).showAndWait()
        if (newPassword.isPresent) {
            credential.password.set(newPassword.get())
        }
    }

    private fun onAddUsername() {
        val newLoginName =
            AddUsernameDialog(
                user,
                credential,
                { input: String?, _: ActionEvent ->
                    if (input === null || input.isEmpty())
                        throw Exception("Empty login name not allowed, please try again.")
                    else if (credential.identities.value.any {
                        it.lowercase() == input.lowercase()
                    }) {
                        throw Exception("Login name already exists, please try again.")
                    }
                }
            ).showAndWait()
        if (newLoginName.isPresent) {
            user.addUsername(newLoginName.get())
            credential.identities.add(newLoginName.get())
            credential.lastUpdated.set(Instant.now())
        }
    }

    fun onDeleteUsername() {
        val deleteButtonType = ButtonType("Delete login name", ButtonBar.ButtonData.OK_DONE)
        val deleteConfirmation = Alert(Alert.AlertType.WARNING).apply {
            headerText = "Confirm delete"
            contentText = "Are you sure you want to delete this login name?"
            dialogPane.buttonTypes.add(ButtonType.CANCEL)
            dialogPane.buttonTypes.remove(ButtonType.OK)
            dialogPane.buttonTypes.add(deleteButtonType)
            dialogPane
                .lookupButton(deleteButtonType)
                .addEventFilter(ActionEvent.ACTION) {
                    if(identity.isNotNull.value && identity.isNotEmpty.value) {
                        user.removeUsername(identity.get())
                        credential.identities.value.remove(identity.value)
                        credential.lastUpdated.set(Instant.now())
                        associatedUsernamesList.selectionModel.select(null)
                    }
                }
        }
        deleteConfirmation.showAndWait()
    }

    private fun addPasswordFieldWithPassword() {
        if(passwordInputHolder.children.size > 1) {
            passwordInputHolder.children.removeAt(1)
        }
        val field = PasswordField().apply {
            HBox.setHgrow(this, Priority.ALWAYS)
            maxWidth = Double.MAX_VALUE
            isEditable = false
            isMouseTransparent = false
            isFocusTraversable = false
            textProperty().bind(passwordProperty)
            val keyCodeCopy = KeyCodeCombination(KeyCode.C, KeyCombination.META_DOWN) // cmd on Mac, ctrl on Windows
            setOnKeyPressed { event ->
                if(keyCodeCopy.match(event)) {
                    copySelectionToClipboard(this.selectedText)
                }
            }
        }
        passwordInputHolder.children.add(field)
    }

    private fun addTextFieldWithPassword() {
        if(passwordInputHolder.children.size > 1) {
            passwordInputHolder.children.removeAt(1)
        }
        val field = TextField().apply {
            HBox.setHgrow(this, Priority.ALWAYS)
            maxWidth = Double.MAX_VALUE
            isEditable = false
            isMouseTransparent = false
            isFocusTraversable = false
            textProperty().bind(passwordProperty)
            val keyCodeCopy = KeyCodeCombination(KeyCode.C, KeyCombination.META_DOWN) // cmd on Mac, ctrl on Windows
            setOnKeyPressed { event ->
                if(keyCodeCopy.match(event)) {
                    copySelectionToClipboard(this.selectedText)
                }
            }
        }
        passwordInputHolder.children.add(field)
    }

    @FXML
    fun initialize() {
        middleButtonPane.columnConstraints.add(0, ColumnConstraints().apply { percentWidth = 50.0 })
        middleButtonPane.columnConstraints.add(1, ColumnConstraints().apply { percentWidth = 50.0 })
        lowerButtonPane.columnConstraints.add(0, ColumnConstraints().apply { percentWidth = 50.0 })
        lowerButtonPane.columnConstraints.add(1, ColumnConstraints().apply { percentWidth = 50.0 })

        passwordProperty.bindBidirectional(credential.password)

        addPasswordFieldWithPassword()
        copyPasswordButton.setOnAction {
            copySelectionToClipboard(credential.password.value)
        }

        showPasswordCheckbox.selectedProperty().addListener { _, _, newValue ->
            if(newValue) {
                addTextFieldWithPassword()
            } else {
                addPasswordFieldWithPassword()
            }
        }

        identity.bind(associatedUsernamesList.selectionModel.selectedItemProperty())
        associatedUsernamesList.itemsProperty().bind(credential.identities)

        addUsernameButton.setOnAction { onAddUsername() }
        removeUsernameButton.disableProperty().bind(identity.isNull)
        removeUsernameButton.setOnAction { onDeleteUsername() }

        createdLabel.textProperty().bind(Bindings.concat("Created: " ).concat(createdString))
        lastUpdatedLabel.textProperty().bind(Bindings.concat("Last updated: " ).concat(lastUpdatedString))

        deleteCredentialsButton.setOnAction { onDeleteCredential() }

        updatePasswordButton.setOnAction { onChangePassword() }

        Platform.runLater {
            copyPasswordButton.requestFocus()
        }
    }

    /*val credentials: ObservableList<CredentialModel> = FXCollections.observableArrayList(CREDENTIAL_LIST_OBSERVABLE_VALUES)

    val originalCredentials: ObservableList<CredentialModel> = FXCollections.observableArrayList(CREDENTIAL_LIST_OBSERVABLE_VALUES)

    val userNames: MutableMap<String, Int> = mutableMapOf()

    var credentialsSaved = false;*/

    /*val altered = Bindings.createBooleanBinding(
        Callable<Boolean> { val b = isAltered(originalCredentials, credentials); println("RAN: " + b); b },
        credentials, originalCredentials
    )

    private fun isAltered(oldList: List<CredentialModel>, newList: List<CredentialModel>): Boolean =
        oldList.size != newList.size ||
        oldList.zip(newList).any { (oldCred, newCred) ->
            println("${oldCred.identities} ${newCred.identities}")
            oldCred.password != newCred.password ||
            oldCred.created.compareTo(newCred.created) != 0 ||
            oldCred.lastUpdated.compareTo(newCred.lastUpdated) != 0 ||
            oldCred.identities.size != newCred.identities.size ||
            oldCred.identities.zip(newCred.identities).any { (oldId, newId) ->
                oldId != newId
            }
        }

    fun setCredentials(inputCredentials: List<Credential>) {
        originalCredentials.setAll(inputCredentials.map{ CredentialModel(it) })
        credentials.setAll(inputCredentials.map{ CredentialModel(it) })
        credentialsSaved = false;
    }

    fun setUserNames(inputUserNames: MutableMap<String, Int>) {
        userNames.apply {
            clear()
            putAll(inputUserNames)
        }
    }

    fun addUserName(newUserName: String) {
        if (userNames.containsKey(newUserName))
            userNames[newUserName] = userNames[newUserName]!!.plus(1)
        else
            userNames[newUserName] = 1
    }

    fun removeUserName(userNameToRemove: String) {
        if(!userNames.containsKey(userNameToRemove))
            throw InternalException(InternalException.InternalExceptionCause.USERNAME_TO_REMOVE_NOT_FOUND)
        if(userNames[userNameToRemove]!!.compareTo(0) <= 0)
            throw InternalException(InternalException.InternalExceptionCause.USERNAME_TO_REMOVE_ZERO_OR_LESS)
        userNames[userNameToRemove] = userNames[userNameToRemove]!!.minus(1)
        if(userNames[userNameToRemove]!!.compareTo(0) == 0)
            userNames.remove(userNameToRemove)
    }

    fun addCredential(credentialPassword: String) {
        credentials.add(CredentialModel(credentialPassword))
    }

    fun removeCredential(credentialPassword: String) {
        credentials.removeIf{ it.password == credentialPassword }
    }

    fun saved() {
        originalCredentials.setAll(credentials)
        credentialsSaved = true
    }*/
}

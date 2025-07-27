package com.lousseief.vault.controller

import com.lousseief.vault.dialog.AddPasswordDialog
import com.lousseief.vault.dialog.AddUsernameDialog
import com.lousseief.vault.dialog.Dialogs
import com.lousseief.vault.model.ui.UiProfile
import com.lousseief.vault.model.ui.UiCredential
import com.lousseief.vault.utils.Colors
import com.lousseief.vault.utils.copySelectionToClipboard
import com.lousseief.vault.utils.timeToStringDate
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIconView
import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.scene.control.Button
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
import javafx.scene.paint.Paint
import java.time.Instant

class CredentialController(
    val user: UiProfile,
    val credential: UiCredential,
    val showClearTextPasswordsProperty: SimpleBooleanProperty,
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
        val delete = Dialogs.openConfirmSensitiveOperationDialog(
            "Delete login name",
            null,
            "Confirm delete",
            "Are you sure you want to delete this login name?"
        )
        if(delete) {
            if(identity.isNotNull.value && identity.isNotEmpty.value) {
                user.removeUsername(identity.get())
                credential.identities.value.remove(identity.value)
                credential.lastUpdated.set(Instant.now())
                associatedUsernamesList.selectionModel.select(null)
            }
        }
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

        if(showClearTextPasswordsProperty.value) {
            addTextFieldWithPassword()
        } else {
            addPasswordFieldWithPassword()
        }

        copyPasswordButton.setOnAction {
            copySelectionToClipboard(credential.password.value)
        }
        copyPasswordButton.graphic = FontAwesomeIconView(FontAwesomeIcon.COPY).apply {
            fill = Paint.valueOf(Colors.GRAY_DARK)
        }

        showPasswordCheckbox.selectedProperty().bindBidirectional(showClearTextPasswordsProperty)
        showClearTextPasswordsProperty.addListener { _, _, newValue ->
            if(newValue) {
                addTextFieldWithPassword()
            } else {
                addPasswordFieldWithPassword()
            }
        }

        identity.bind(associatedUsernamesList.selectionModel.selectedItemProperty())
        associatedUsernamesList.itemsProperty().bind(credential.identities)

        addUsernameButton.setOnAction { onAddUsername() }
        addUsernameButton.graphic = MaterialDesignIconView(MaterialDesignIcon.PLUS_CIRCLE).apply {
            size = "16px"
            fill = Paint.valueOf(Colors.GREEN)
        }
        removeUsernameButton.disableProperty().bind(identity.isNull)
        removeUsernameButton.setOnAction { onDeleteUsername() }
        removeUsernameButton.graphic = MaterialDesignIconView(MaterialDesignIcon.MINUS_CIRCLE).apply {
            size = "16px"
            fill = Paint.valueOf("red")
        }

        createdLabel.textProperty().bind(Bindings.concat("Created: " ).concat(createdString))
        lastUpdatedLabel.textProperty().bind(Bindings.concat("Last updated: " ).concat(lastUpdatedString))

        deleteCredentialsButton.setOnAction { onDeleteCredential() }
        deleteCredentialsButton.graphic = FontAwesomeIconView(FontAwesomeIcon.TRASH).apply {
            size = "20px"
            fill = Paint.valueOf("white")
        }


        updatePasswordButton.setOnAction { onChangePassword() }
        updatePasswordButton.graphic = MaterialDesignIconView(MaterialDesignIcon.CACHED).apply {
            fill = Paint.valueOf("black")
        }

        Platform.runLater {
            copyPasswordButton.requestFocus()
        }
    }
}

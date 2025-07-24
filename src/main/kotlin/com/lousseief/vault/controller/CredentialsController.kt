package com.lousseief.vault.controller

import com.lousseief.vault.dialog.AddPasswordDialog
import com.lousseief.vault.dialog.Dialogs
import com.lousseief.vault.model.Credential
import javafx.collections.ObservableList
import com.lousseief.vault.model.ui.UiProfile
import com.lousseief.vault.model.ui.UiAssociation
import com.lousseief.vault.model.ui.UiCredential
import com.lousseief.vault.utils.Colors
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIconView
import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.VBox
import javafx.scene.paint.Paint
import java.time.Instant

class CredentialsController(
    val user: UiProfile,
    val initialMainIdentifier: String,
    val association: UiAssociation,
    val credentials: ObservableList<UiCredential>,
    val initialCredentials: List<Credential>,
    val onClose: () -> Unit
) {

    @FXML
    private lateinit var headerText: Label

    @FXML
    private lateinit var leftButton: Button

    @FXML
    private lateinit var rightButton: Button

    @FXML
    private lateinit var addCredentialButton: Button

    @FXML
    private lateinit var doneButton: Button

    @FXML
    private lateinit var credentialHolder: VBox

    @FXML
    private lateinit var currentCredentialLabel: Label

    @FXML
    private lateinit var headerHolder: VBox

    private val currentCredentialProperty = SimpleIntegerProperty(-1)

    private val currentCredentialLabelText = Bindings.createStringBinding(
    { "${currentCredentialProperty.value + 1} / ${credentials.size}" },
    credentials, currentCredentialProperty
    )

    private val disableRightArrow = Bindings.createBooleanBinding(
        { currentCredentialProperty.value == -1 || currentCredentialProperty.value == (credentials.size - 1) },
        currentCredentialProperty
    )

    private val disableLeftArrow = Bindings.createBooleanBinding(
        { currentCredentialProperty.value <= 0 },
        currentCredentialProperty
    )

    private fun setCredential(credentialIndex: Int?) {
        if(credentialIndex != null) {
            val loader = FXMLLoader(javaClass.getResource("/Credential.fxml"))
            loader.setController(
                CredentialController(
                    user,
                    credentials[credentialIndex],
                    { onDeleteCredential(credentialIndex) }
                )
            )
            val credentialView: Parent = loader.load()
            credentialHolder.children.clear()
            credentialHolder.children.add(credentialView)
            currentCredentialProperty.value = credentialIndex
        } else {
            credentialHolder.children.clear()
            credentialHolder.children.add(Label("You have no credentials yet for this association"))
            currentCredentialProperty.value = -1
        }
    }

    private fun onAddCredential() {
        val newPassword = AddPasswordDialog(
            user.settings.passwordLength.value,
            { input: String?, _: ActionEvent ->
                if (input === null || input.isEmpty()) {
                    throw Exception("Empty password is neither advisable nor allowed.")
                }
            },
            "Generate or enter a password for your new credential"
        ).showAndWait()
        if (newPassword.isPresent) {
            credentials.add(
                UiCredential(
                    password = SimpleStringProperty(newPassword.get()),
                    created = SimpleObjectProperty(Instant.now()),
                    lastUpdated = SimpleObjectProperty(Instant.now()),
                    identities = SimpleListProperty(FXCollections.observableArrayList())
                )
            )
            setCredential(credentials.size - 1)
        }
    }

    private fun onDeleteCredential(credentialIndex: Int) {
        val proceed = Dialogs.openConfirmSensitiveOperationDialog(
            "Delete credential",
            null,
            "Confirm delete",
            "Are you sure you want to delete this login credential?"
        )
        if(proceed) {
            credentials[credentialIndex].identities.value.forEach {
                user.removeUsername(it)
            }
            credentials.removeAt(credentialIndex)
            if(credentials.isNotEmpty()) {
                setCredential((credentialIndex - 1).coerceAtLeast(0))
            } else {
                setCredential(null)
            }
            Alert(Alert.AlertType.INFORMATION).apply {
                title = "Success"
                headerText = "Credential successfully removed"
                contentText = "The credential was successfully deleted from your association. If you don't save the vault, the delete credential will reappear on next login. Upon saving the vault, the credential cannot be restored."
            }.showAndWait()
        }
    }

    private fun onClickLeft() {
        if(currentCredentialProperty.value > 0) {
            setCredential(currentCredentialProperty.value - 1)
        }
    }

    private fun onClickRight() {
        if(currentCredentialProperty.value < credentials.size - 1) {
            setCredential(currentCredentialProperty.value + 1)
        }
    }

    private fun keyListener(event: KeyEvent) {
        if(event.code === KeyCode.LEFT) {
            onClickLeft()
        } else if(event.code === KeyCode.RIGHT) {
            onClickRight()
        }
        event.consume()
    }

    private fun credentialsAreAltered(oldList: List<Credential>, newList: List<UiCredential>): Boolean =
        oldList.size != newList.size
            || oldList.zip(newList).any { (oldCred, newCred) ->
                oldCred.password != newCred.password.value
                    || oldCred.created.compareTo(newCred.created.value) != 0
                    || oldCred.lastUpdated.compareTo(newCred.lastUpdated.value) != 0
                    || oldCred.identities.size != newCred.identities.size
                    || oldCred.identities.zip(newCred.identities).any { (oldId, newId) ->
                        oldId != newId
                    }
            }

    @FXML
    fun initialize() {
        headerHolder.children.addFirst(MaterialDesignIconView(MaterialDesignIcon.INCOGNITO).apply {
            size = "40px"
            fill = Paint.valueOf("black")
        })
        leftButton.graphic = FontAwesomeIconView(FontAwesomeIcon.CHEVRON_LEFT)
        leftButton.disableProperty().bind(disableLeftArrow)
        leftButton.setOnAction { onClickLeft() }
        rightButton.graphic = FontAwesomeIconView(FontAwesomeIcon.CHEVRON_RIGHT)
        rightButton.disableProperty().bind(disableRightArrow)
        rightButton.setOnAction { onClickRight() }
        currentCredentialLabel.textProperty().bind(currentCredentialLabelText)
        headerText.text = "Credentials for association\n" + (association.mainIdentifier.value.ifEmpty { null } ?: "(unnamed association)")

        if(credentials.isNotEmpty()) {
            setCredential(0)
        } else {
            credentialHolder.children.add(Label("You have no credentials yet for this association"))
        }

        addCredentialButton.setOnAction { onAddCredential() }
        addCredentialButton.graphic = MaterialDesignIconView(MaterialDesignIcon.PLUS_CIRCLE).apply {
            size = "16px"
            fill = Paint.valueOf(Colors.GREEN)
        }

        doneButton.setOnAction {
            user.passwordRequiredAction()?.let { password ->
                if(credentialsAreAltered(initialCredentials, credentials)) {
                    user.updateCredentials(initialMainIdentifier, credentials.map { it.toCredential() }, password)
                } else {
                    println("Did not need to save credentials")
                }
                onClose()
            }
        }

        Platform.runLater {
            currentCredentialLabel.scene.apply {
                addEventFilter(KeyEvent.KEY_PRESSED, ::keyListener)
                window.setOnCloseRequest {
                    if(credentialsAreAltered(initialCredentials, credentials)) {
                        val proceed = Dialogs.openConfirmSensitiveOperationDialog(
                            "Close anyway",
                            null,
                            "Do you really want to close this window?",
                            "You have unsaved changes in these credentials, do you really want to close this window without saving first? If no, press \"Cancel\" and then press \"Save\"."
                        )
                        if(!proceed) {
                            it.consume()
                        }
                    }
                }
            }
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

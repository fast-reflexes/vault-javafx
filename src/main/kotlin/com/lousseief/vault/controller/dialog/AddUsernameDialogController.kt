package com.lousseief.vault.controller.dialog

import com.lousseief.vault.list.UsernameListCellFactory
import com.lousseief.vault.list.UsernameListCell
import com.lousseief.vault.model.ui.UiProfile
import com.lousseief.vault.model.ui.UiCredential
import com.lousseief.vault.utils.setupErrorMessageHandling
import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.scene.control.ButtonType
import javafx.scene.control.ComboBox
import javafx.scene.control.Dialog
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.layout.ColumnConstraints
import javafx.scene.layout.GridPane
import javafx.scene.layout.VBox

class AddUsernameDialogController(
    val user: UiProfile,
    val credential: UiCredential,
    val evaluator: (String, ActionEvent) -> Unit) {

    private val errorProperty = SimpleStringProperty("")
    private val usernameProperty = SimpleStringProperty("")

    private val existingUsernames = FXCollections.observableArrayList(listOf("") + user.userNames.keys.sorted())

    @FXML
    private lateinit var mainHolder: GridPane

    @FXML
    private lateinit var okButton: ButtonType

    @FXML
    private lateinit var cancelButton: ButtonType

    @FXML
    private lateinit var verticalHolder: VBox

    @FXML
    private lateinit var usernameField: TextField

    @FXML
    private lateinit var existingUsernamesBox: ComboBox<String>

    @FXML
    fun initialize() {
        existingUsernamesBox.cellFactory = UsernameListCellFactory(credential, existingUsernames)
        existingUsernamesBox.buttonCell = UsernameListCell(credential, existingUsernames)
        existingUsernamesBox.items = existingUsernames
        existingUsernamesBox.selectionModel.selectFirst()
        existingUsernamesBox.disableProperty().bind(Bindings.size(existingUsernames).isEqualTo(1))
        usernameField.textProperty().bindBidirectional(usernameProperty)
        existingUsernamesBox.selectionModel.selectedItemProperty().addListener { _, _, newValue ->
            usernameProperty.set(newValue)
        }
        mainHolder.columnConstraints.add(0, ColumnConstraints().apply { percentWidth = 100.0 })
    }

    fun finalize(readyDialog: Dialog<String?>) {
        val icon = Label().apply {
            styleClass.addAll("alert", "confirmation", "dialog-pane")
        }
        readyDialog.title = "Add username"
        readyDialog.headerText = "Add a login name to add to your credential"
        readyDialog.graphic = icon
        readyDialog.setResultConverter { type: ButtonType ->
            when (type) {
                okButton -> usernameField.textProperty().value
                cancelButton -> null
                else -> null
            }
        }

        readyDialog.dialogPane.apply {
            lookupButton(okButton).apply {
                addEventFilter(ActionEvent.ACTION) {
                        event ->
                    try {
                        evaluator(usernameField.textProperty().value, event)
                    }
                    catch(e: Exception) {
                        event.consume()
                        errorProperty.set(e.message)
                        readyDialog.dialogPane.scene.window.sizeToScene()
                    }
                }
                disableProperty().bind(usernameField.textProperty().isEmpty.or(errorProperty.isNotEmpty))
            }

        }

        Platform.runLater {
            setupErrorMessageHandling(
                errorProperty,
                readyDialog.dialogPane.width,
                verticalHolder,
                1
            )
            usernameField.requestFocus()
        }
    }

}

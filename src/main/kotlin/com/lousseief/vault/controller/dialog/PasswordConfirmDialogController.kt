package com.lousseief.vault.controller.dialog

import com.lousseief.vault.utils.setupErrorMessageHandling
import javafx.application.Platform
import javafx.beans.property.SimpleStringProperty
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.scene.control.ButtonType
import javafx.scene.control.Dialog
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.layout.VBox

class PasswordConfirmDialogController(val evaluator: (String, ActionEvent) -> Unit) {

    private val errorProperty = SimpleStringProperty("")
    private val passwordInputProperty = SimpleStringProperty("")

    @FXML
    private lateinit var okButton: ButtonType

    @FXML
    private lateinit var cancelButton: ButtonType

    @FXML
    private lateinit var verticalHolder: VBox

    @FXML
    private lateinit var passwordField: TextField

    @FXML
    fun initialize() {
        passwordInputProperty.bind(passwordField.textProperty())
    }

    fun finalize(readyDialog: Dialog<String?>) {
        val icon = Label().apply {
            styleClass.addAll("alert", "confirmation", "dialog-pane")
        }
        readyDialog.title = "Enter password"
        readyDialog.headerText = "Enter your vault password"
        readyDialog.graphic = icon
        readyDialog.setResultConverter { type: ButtonType ->
            when (type) {
                okButton -> passwordInputProperty.value
                cancelButton -> null
                else -> null
            }
        }
        readyDialog.dialogPane.apply {
            lookupButton(okButton).apply {
                addEventFilter(ActionEvent.ACTION) {
                        event ->
                    try {
                        evaluator(passwordInputProperty.value, event)
                    }
                    catch(e: Exception) {
                        event.consume()
                        errorProperty.set(e.message)
                        readyDialog.dialogPane.scene.window.sizeToScene()
                    }
                }
                disableProperty().bind(passwordInputProperty.isEmpty)
            }

        }

        Platform.runLater {
            setupErrorMessageHandling(
                errorProperty,
                readyDialog.dialogPane.width,
                verticalHolder,
                1
            )
            passwordField.requestFocus()
        }
    }

}

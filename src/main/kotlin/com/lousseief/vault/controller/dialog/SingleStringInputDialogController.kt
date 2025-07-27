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

class SingleStringInputDialogController(val evaluator: (String, ActionEvent) -> Unit, val initialValue: String?) {

    private val errorProperty = SimpleStringProperty("")
    private val stringInputProperty = SimpleStringProperty("")

    @FXML
    private lateinit var okButton: ButtonType

    @FXML
    private lateinit var cancelButton: ButtonType

    @FXML
    private lateinit var verticalHolder: VBox

    @FXML
    private lateinit var stringInputField: TextField

    @FXML
    fun initialize() {
        if(initialValue != null) {
            stringInputField.text = initialValue
        }
        stringInputProperty.bind(stringInputField.textProperty())
    }

    fun finalize(readyDialog: Dialog<String?>, title: String, header: String) {
        val icon = Label().apply {
            styleClass.addAll("alert", "confirmation", "dialog-pane")
        }
        readyDialog.title = title
        readyDialog.headerText = header
        readyDialog.graphic = icon
        readyDialog.setResultConverter { type: ButtonType ->
            when (type) {
                okButton -> stringInputProperty.value
                cancelButton -> null
                else -> null
            }
        }
        readyDialog.dialogPane.apply {
            lookupButton(okButton).apply {
                addEventFilter(ActionEvent.ACTION) {
                        event ->
                    try {
                        evaluator(stringInputProperty.value, event)
                    }
                    catch(e: Exception) {
                        event.consume()
                        errorProperty.set(e.message)
                        readyDialog.dialogPane.scene.window.sizeToScene()
                    }
                }
                disableProperty().bind(stringInputProperty.isEmpty)
            }

        }

        Platform.runLater {
            setupErrorMessageHandling(
                errorProperty,
                readyDialog.dialogPane.width,
                verticalHolder,
                1
            )
            stringInputField.requestFocus()
        }
    }

}

package com.lousseief.vault.controller.dialog

import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleStringProperty
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.geometry.Pos
import javafx.scene.control.ButtonType
import javafx.scene.control.Dialog
import javafx.scene.control.Label
import javafx.scene.control.PasswordField
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.text.TextAlignment

class ChangeMasterPasswordDialogController(val evaluator: (String, String) -> Unit) {

    private val errorProperty = SimpleStringProperty("")
    private val oldPasswordProperty = SimpleStringProperty("")
    private val passwordProperty = SimpleStringProperty("")
    private val passwordRepetitionProperty = SimpleStringProperty("")

    private val hasError = Bindings.createBooleanBinding(
        { !errorProperty.value.isEmpty() },
        errorProperty
    )

    private val isIncomplete = Bindings.createBooleanBinding(
        { passwordProperty.value.isEmpty() || oldPasswordProperty.value.isEmpty() || passwordRepetitionProperty.value.isEmpty() },
        oldPasswordProperty, passwordProperty, passwordRepetitionProperty
    )

    @FXML
    private lateinit var okButton: ButtonType

    @FXML
    private lateinit var cancelButton: ButtonType

    @FXML
    private lateinit var verticalHolder: VBox

    @FXML
    private lateinit var oldPasswordField: PasswordField

    @FXML
    private lateinit var passwordField: PasswordField

    @FXML
    private lateinit var passwordRepetitionField: PasswordField

    private fun validate() =
        when {
            passwordProperty.value != passwordRepetitionProperty.value -> {
                passwordRepetitionField.requestFocus()
                passwordRepetitionField.selectAll()
                throw Exception("The password and password repetition didn't match.")
            }
            passwordProperty.value.isEmpty() -> {
                passwordField.requestFocus()
                throw Exception("Empty password is not allowed.")
            }
            else -> {
                oldPasswordField.requestFocus()
                oldPasswordField.selectAll()
            }
        }

    @FXML
    fun initialize() {
        oldPasswordProperty.bind(oldPasswordField.textProperty())
        passwordProperty.bind(passwordField.textProperty())
        passwordRepetitionProperty.bind(passwordRepetitionField.textProperty())
    }

    fun finalize(readyDialog: Dialog<String?>) {
        val icon = Label().apply {
            styleClass.addAll("alert", "confirmation", "dialog-pane")
        }
        readyDialog.headerText = "Change vault master password"
        readyDialog.graphic = icon
        readyDialog.setResultConverter { type: ButtonType ->
            when (type) {
                okButton -> passwordProperty.value
                cancelButton -> null
                else -> null
            }
        }
        readyDialog.dialogPane.apply {
            lookupButton(okButton).apply {
                addEventFilter(ActionEvent.ACTION) {
                        event ->
                    try {
                        validate()
                        evaluator(oldPasswordProperty.value, passwordProperty.value)
                    }
                    catch(e: Exception) {
                        event.consume()
                        errorProperty.set(e.message)
                        readyDialog.dialogPane.scene.window.sizeToScene()
                    }
                }
                disableProperty().bind(isIncomplete)
            }

        }

        Platform.runLater{
            errorProperty.addListener { _, _, newValue ->
                if(newValue.isNullOrEmpty()) {
                    if(verticalHolder.children.size > 1) {
                        (1..verticalHolder.children.size - 1).forEach { verticalHolder.children.removeAt(it) }
                    }
                } else {
                    if(verticalHolder.children.size == 1) {
                        verticalHolder.children.add(
                            Label(errorProperty.value).apply {
                                HBox.setHgrow(this, Priority.ALWAYS)
                                VBox.setVgrow(this, Priority.ALWAYS)
                                maxHeight = Double.MAX_VALUE
                                textAlignment = TextAlignment.RIGHT
                                style="-fx-text-fill: red"
                                alignment = Pos.CENTER_RIGHT
                                isWrapText = true
                                maxWidth = readyDialog.dialogPane.width
                            }
                        )
                    }
                }
            }
            oldPasswordField.requestFocus()
        }
    }

}

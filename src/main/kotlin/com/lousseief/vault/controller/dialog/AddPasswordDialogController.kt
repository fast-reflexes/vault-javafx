package com.lousseief.vault.controller.dialog

import com.lousseief.vault.crypto.CryptoUtils
import com.lousseief.vault.crypto.CryptoUtils.getCharPoolContent
import javafx.application.Platform
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.ButtonBar
import javafx.scene.control.ButtonType
import javafx.scene.control.CheckBox
import javafx.scene.control.Dialog
import javafx.scene.control.DialogPane
import javafx.scene.control.Label
import javafx.scene.control.Spinner
import javafx.scene.control.SpinnerValueFactory
import javafx.scene.control.TextField
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.text.TextAlignment
import javafx.util.StringConverter

class AddPasswordDialogController(
    val defaultPasswordLength: Int,
    val evaluator: (String, ActionEvent) -> Unit,
    val header: String
) {

    companion object {
        const val STRING_LENGTH_MAX = 40
        const val STRING_LENGTH_MIN = 5
    }

    private val errorProperty = SimpleStringProperty("")
    private val lowerCaseProperty = SimpleBooleanProperty(true)
    private val upperCaseProperty = SimpleBooleanProperty(true)
    private val numbersProperty = SimpleBooleanProperty(true)
    private val specialCharsProperty = SimpleBooleanProperty(true)
    private val stringLengthProperty = SimpleIntegerProperty(defaultPasswordLength)
    private val generatedStringProperty = SimpleStringProperty("")

    @FXML
    private lateinit var dialogPane: DialogPane

    @FXML
    private lateinit var stringField: TextField

    @FXML
    private lateinit var intSpinner: Spinner<Number>

    @FXML
    private lateinit var lowerCaseCheckbox: CheckBox

    @FXML
    private lateinit var upperCaseCheckbox: CheckBox

    @FXML
    private lateinit var numbersCheckbox: CheckBox

    @FXML
    private lateinit var specialCharsCheckbox: CheckBox

    @FXML
    private lateinit var cancelButton: ButtonType

    @FXML
    private lateinit var okButton: ButtonType

    @FXML
    private lateinit var generatePasswordButton: Button

    @FXML
    private lateinit var verticalHolder: VBox

    @FXML
    fun initialize() {
        stringField.textProperty().bindBidirectional(generatedStringProperty)
        stringLengthProperty.bind(intSpinner.valueProperty())
        lowerCaseCheckbox.selectedProperty().bindBidirectional(lowerCaseProperty)
        upperCaseCheckbox.selectedProperty().bindBidirectional(upperCaseProperty)
        numbersCheckbox.selectedProperty().bindBidirectional(numbersProperty)
        specialCharsCheckbox.selectedProperty().bindBidirectional(specialCharsProperty)
        ButtonBar.setButtonUniformSize(dialogPane.lookupButton(cancelButton), false)
        ButtonBar.setButtonUniformSize(dialogPane.lookupButton(okButton), false)

        val factory = intSpinner.valueFactory as SpinnerValueFactory.IntegerSpinnerValueFactory
        factory.max = STRING_LENGTH_MAX
        factory.min = STRING_LENGTH_MIN
        factory.value = defaultPasswordLength
        intSpinner.isEditable = true
        intSpinner.getValueFactory().converter = object : StringConverter<Number?>() {

            override fun toString(value: Number?): String =
                value?.toInt()?.toString() ?: "0"

            override fun fromString(value: String?): Number =
                (value
                    ?.let {
                        try {
                            Integer.parseInt(value.trim())
                        } catch(e: Exception) { null }
                    }
                    ?.let {
                        when {
                            it > STRING_LENGTH_MAX -> STRING_LENGTH_MAX
                            it < STRING_LENGTH_MIN -> STRING_LENGTH_MIN
                            else -> it
                        }
                    }
                    ?: stringLengthProperty.value)
                    .also {
                        intSpinner.editor.text = toString(it)
                    }

        }

        generatePasswordButton.setOnAction {
            generatedStringProperty.set(
                CryptoUtils.generateRandomString(
                    getCharPoolContent(
                        lowerCaseProperty.value,
                        upperCaseProperty.value,
                        numbersProperty.value,
                        specialCharsProperty.value
                    ),
                    stringLengthProperty.value
                )
            )
        }
    }

    fun finalize(readyDialog: Dialog<String?>) {
        readyDialog.headerText = header

        val icon = Label()
        icon.styleClass.addAll("alert", "confirmation", "dialog-pane")
        readyDialog.graphic = icon
        readyDialog.setOnCloseRequest { readyDialog.close() }

        readyDialog.setResultConverter { type: ButtonType ->
            when(type) {
                okButton -> generatedStringProperty.value
                else -> null
            }
        }

        readyDialog.dialogPane.apply {
            lookupButton(okButton).apply {
                disableProperty().bind(generatedStringProperty.isEmpty)
                addEventFilter(ActionEvent.ACTION) { event ->
                    try {
                        evaluator(generatedStringProperty.value, event)
                    }
                    catch(e: Exception) {
                        event.consume()
                        errorProperty.set(e.message)
                        readyDialog.dialogPane.scene.window.sizeToScene()
                    }
                }
            }
        }
        readyDialog.dialogPane.scene.stylesheets.add("/styles.css")

        Platform.runLater {
            errorProperty.addListener { _, _, newValue ->
                if(newValue.isNullOrEmpty()) {
                    if(verticalHolder.children.size > 2) {
                        (2..verticalHolder.children.size - 1).forEach { verticalHolder.children.removeAt(it) }
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
                                maxWidth = readyDialog.dialogPane.width // or stringField.width
                            }
                        )
                    }
                }
            }
            stringField.requestFocus()
        }

    }
}

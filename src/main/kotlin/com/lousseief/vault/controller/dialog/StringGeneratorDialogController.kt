package com.lousseief.vault.controller.dialog

import com.lousseief.vault.crypto.CryptoUtils
import com.lousseief.vault.crypto.CryptoUtils.getCharPoolContent
import com.lousseief.vault.utils.copySelectionToClipboard
import com.lousseief.vault.utils.initializeSpinner
import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.scene.control.ButtonBar
import javafx.scene.control.ButtonType
import javafx.scene.control.CheckBox
import javafx.scene.control.Dialog
import javafx.scene.control.DialogPane
import javafx.scene.control.Label
import javafx.scene.control.Spinner
import javafx.scene.control.TextField

class StringGeneratorDialogController(val defaultPasswordLength: Int) {

    companion object {
        const val STRING_LENGTH_MAX = 100
        const val STRING_LENGTH_MIN = 1
    }

    val lowerCaseProperty = SimpleBooleanProperty(true)
    val upperCaseProperty = SimpleBooleanProperty(true)
    val numbersProperty = SimpleBooleanProperty(true)
    val specialCharsProperty = SimpleBooleanProperty(true)
    val stringLengthProperty = SimpleIntegerProperty(defaultPasswordLength)
    val generatedStringProperty = SimpleStringProperty("")

    @FXML
    private lateinit var dialogPane: DialogPane
    @FXML
    private lateinit var stringField: TextField

    @FXML
    private lateinit var intSpinner: Spinner<Int>

    @FXML
    private lateinit var lowerCaseCheckbox: CheckBox

    @FXML
    private lateinit var upperCaseCheckbox: CheckBox

    @FXML
    private lateinit var numbersCheckbox: CheckBox

    @FXML
    private lateinit var specialCharsCheckbox: CheckBox

    @FXML
    private lateinit var copyButton: ButtonType

    @FXML
    private lateinit var generateButton: ButtonType

    @FXML
    private lateinit var closeButton: ButtonType

    val stringHasNotBeenGenerated = Bindings.createBooleanBinding(
        { generatedStringProperty.value.isNullOrEmpty() },
        generatedStringProperty
    )

    @FXML
    fun initialize() {
        stringField.textProperty().bind(generatedStringProperty)
        stringLengthProperty.bind(intSpinner.valueProperty())
        lowerCaseCheckbox.selectedProperty().bindBidirectional(lowerCaseProperty)
        upperCaseCheckbox.selectedProperty().bindBidirectional(upperCaseProperty)
        numbersCheckbox.selectedProperty().bindBidirectional(numbersProperty)
        specialCharsCheckbox.selectedProperty().bindBidirectional(specialCharsProperty)

        ButtonBar.setButtonUniformSize(dialogPane.lookupButton(copyButton), false)
        ButtonBar.setButtonUniformSize(dialogPane.lookupButton(generateButton), false)
        ButtonBar.setButtonUniformSize(dialogPane.lookupButton(closeButton), false)

        dialogPane.lookupButton(generateButton).apply {
            addEventFilter(ActionEvent.ACTION) { event ->
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
                event.consume()
            }
            Platform.runLater { this.requestFocus() }
        }
        dialogPane.lookupButton(copyButton).apply {
            addEventFilter(ActionEvent.ACTION) { event ->
                copySelectionToClipboard(generatedStringProperty.value)
                event.consume()
            }
            disableProperty().bind(stringHasNotBeenGenerated)
        }

        initializeSpinner(stringLengthProperty, intSpinner,  STRING_LENGTH_MAX, STRING_LENGTH_MIN)
    }

    fun finalize(readyDialog: Dialog<String?>) {
        readyDialog.headerText = "Generate a random string for general use"

        val icon = Label()
        icon.styleClass.addAll("alert", "confirmation", "dialog-pane")
        readyDialog.graphic = icon
        readyDialog.setOnCloseRequest { readyDialog.close() }

        readyDialog.dialogPane.scene.stylesheets.add("/styles.css");
    }
}

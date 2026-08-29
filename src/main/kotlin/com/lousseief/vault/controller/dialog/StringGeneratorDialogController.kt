package com.lousseief.vault.controller.dialog

import com.lousseief.vault.crypto.CryptoUtils
import com.lousseief.vault.crypto.CryptoUtils.getCharPoolContent
import com.lousseief.vault.utils.Colors
import com.lousseief.vault.utils.OSPlatform
import com.lousseief.vault.utils.copySecretToClipboard
import com.lousseief.vault.utils.initializeSpinner
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIconView
import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.ButtonBar
import javafx.scene.control.ButtonType
import javafx.scene.control.CheckBox
import javafx.scene.control.Dialog
import javafx.scene.control.DialogPane
import javafx.scene.control.Label
import javafx.scene.control.Spinner
import javafx.scene.control.TextField
import javafx.scene.paint.Paint

class StringGeneratorDialogController(defaultPasswordLength: Int) {

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

    /* with no category selected the character pool is empty, which would make generation divide by
    zero - so generation is blocked until at least one category is picked */
    val noCategorySelected = Bindings.createBooleanBinding(
        {
            !lowerCaseProperty.value && !upperCaseProperty.value &&
                !numbersProperty.value && !specialCharsProperty.value
        },
        lowerCaseProperty, upperCaseProperty, numbersProperty, specialCharsProperty
    )

    @FXML
    fun initialize() {
        stringField.textProperty().bind(generatedStringProperty)
        lowerCaseCheckbox.selectedProperty().bindBidirectional(lowerCaseProperty)
        upperCaseCheckbox.selectedProperty().bindBidirectional(upperCaseProperty)
        numbersCheckbox.selectedProperty().bindBidirectional(numbersProperty)
        specialCharsCheckbox.selectedProperty().bindBidirectional(specialCharsProperty)

        ButtonBar.setButtonUniformSize(dialogPane.lookupButton(copyButton), false)
        ButtonBar.setButtonUniformSize(dialogPane.lookupButton(generateButton), false)
        ButtonBar.setButtonUniformSize(dialogPane.lookupButton(closeButton), false)

        /* close is CANCEL_CLOSE so escape and the window cross both reach it - the default flag is set here on top so
        enter closes as well, since no single button data makes a button both the enter and the escape one */
        (dialogPane.lookupButton(closeButton) as Button).isDefaultButton = true

        /* the stock order strings already order these buttons the way we want, but on mac they place the growing
        spacer BETWEEN generate and close, and it eats every pixel of slack. These minimal strings keep the same order
        and move the grow to the front, so the three sit together at the right edge - H is copy and generate, C is
        close. The bar is a direct child of the DialogPane from construction, so no css lookup or runLater is needed;
        should it ever not be found the stock string stays and only the gap comes back, never the wrong order. */
        dialogPane.childrenUnmodifiable.filterIsInstance<ButtonBar>().firstOrNull()?.buttonOrder =
            if (OSPlatform.isWindows) "+CH" else "+HC"

        dialogPane.lookupButton(generateButton).apply {
            this as Button
            graphic = MaterialDesignIconView(MaterialDesignIcon.CREATION).apply {
                size = "16px"
                fill = Paint.valueOf(Colors.GOLD)
            }
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
            disableProperty().bind(noCategorySelected)
            Platform.runLater { this.requestFocus() }
        }
        dialogPane.lookupButton(copyButton).apply {
            this as Button
            graphic = FontAwesomeIconView(FontAwesomeIcon.COPY).apply { fill = Paint.valueOf(Colors.GRAY_DARK)}
            addEventFilter(ActionEvent.ACTION) { event ->
                copySecretToClipboard(generatedStringProperty.value)
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

        readyDialog.dialogPane.scene.stylesheets.add("/styles/styles.css")
    }
}

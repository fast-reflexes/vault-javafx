package com.lousseief.vault.controller.dialog

import com.lousseief.vault.utils.setupErrorMessageHandling
import javafx.application.Platform
import javafx.beans.property.SimpleStringProperty
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.ButtonType
import javafx.scene.control.Dialog
import javafx.scene.control.Label
import javafx.scene.layout.VBox
import javafx.scene.text.Text
import javafx.stage.DirectoryChooser
import javafx.stage.Stage
import java.io.File

class DirectoryPathInputDialogController() {

    private val errorProperty = SimpleStringProperty("")
    private val directoryProperty = SimpleStringProperty("")

    @FXML
    private lateinit var okButton: ButtonType

    @FXML
    private lateinit var verticalHolder: VBox

    @FXML
    private lateinit var directoryChooserButton: Button

    @FXML
    private lateinit var infoLabel: Label

    @FXML
    private lateinit var selectedLabel: Text

    @FXML
    fun initialize() {}

    fun finalize(
        readyDialog: Dialog<String?>,
        header: String,
        description: String
    ) {
        val directoryChooser = DirectoryChooser()
        directoryChooser.initialDirectory = File(".")

        directoryChooserButton.setOnAction({ e ->
            val selectedDirectory = directoryChooser.showDialog(Stage())
            if(selectedDirectory != null && selectedDirectory.absolutePath.isNotEmpty()) {
                directoryProperty.set(selectedDirectory.absolutePath)
                selectedLabel.text = "Selected: '${selectedDirectory.absolutePath}'"
                selectedLabel.scene.window.sizeToScene()
                selectedLabel.scene.window.centerOnScreen()
            }

        })
        val icon = Label().apply {
            styleClass.addAll("alert", "confirmation", "dialog-pane")
        }
        readyDialog.title = "Choose directory"
        readyDialog.headerText = header
        infoLabel.text = description
        readyDialog.graphic = icon
        readyDialog.setResultConverter { type: ButtonType? ->
            // button type if null when closing with cross since no cancel button exists in the dialog
            when (type) {
                okButton -> directoryProperty.value
                else -> null
            }
        }
        readyDialog.dialogPane.apply {
            lookupButton(okButton).apply {
                addEventFilter(ActionEvent.ACTION) { event ->
                    try {
                        if(directoryProperty.value.isNullOrBlank()) {
                            throw Exception("Must select a valid directory to proceed")
                        }
                    }
                    catch(e: Exception) {
                        event.consume()
                        errorProperty.set(e.message)
                        readyDialog.dialogPane.scene.window.sizeToScene()
                    }
                }
                disableProperty().bind(directoryProperty.isEmpty)
            }

        }

        Platform.runLater {
            setupErrorMessageHandling(
                errorProperty,
                readyDialog.dialogPane.width,
                verticalHolder,
                1
            )
            directoryChooserButton.requestFocus()
        }
    }

}

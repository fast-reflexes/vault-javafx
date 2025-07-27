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

class ChooseProfilesLocationDialogController() {

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

    fun finalize(readyDialog: Dialog<String?>, isInitialSetup: Boolean) {
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
        readyDialog.headerText = "Choose which directory you want to store your profiles in"
        if(isInitialSetup) {
            infoLabel.text = "It looks like this is the first time you start this application, for the application to work, it must know " +
                "where it should store profile data. Please click the button and point out where profile data should be " +
                "stored. This location can be changed later should it be needed.\n\nIf you ever experience problems with " +
                "the settings file, simply delete the file name vault.settings and then this dialog will prompt you when you " +
                "start the app the next time."
        } else {
            infoLabel.text = "It looks like you want to change the location where profiles are stored. To do this without " +
                "causing any problems in using this app, do it in the following way. First save any work and log out and " +
                "close the app. Then restart it and without doing any changes, update the profiles location. Then immediately " +
                "log out and close the app without doing any work. Now, copy your profiles (if you want to) from the previous " +
                "location to the new. After this, you are ready to start using Vault with your new profiles location."
        }
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

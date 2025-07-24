package com.lousseief.vault.controller.dialog

import com.lousseief.vault.list.UsernameListCellFactory
import com.lousseief.vault.list.UsernameListCell
import com.lousseief.vault.model.ui.UiProfile
import com.lousseief.vault.model.ui.UiCredential
import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.geometry.Pos
import javafx.scene.control.ButtonType
import javafx.scene.control.ComboBox
import javafx.scene.control.Dialog
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.layout.ColumnConstraints
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.text.TextAlignment

class AddUsernameDialogController(
    val user: UiProfile,
    val credential: UiCredential,
    val evaluator: (String, ActionEvent) -> Unit) {

    private val errorProperty = SimpleStringProperty("")
    private val usernameProperty = SimpleStringProperty("")

    private val existingUsernames = FXCollections.observableArrayList(listOf("") + user.userNames.keys)

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
                                maxWidth = readyDialog.dialogPane.width // or usernameField.width
                            }
                        )
                    }
                }
            }
            usernameField.requestFocus()
        }
    }

}

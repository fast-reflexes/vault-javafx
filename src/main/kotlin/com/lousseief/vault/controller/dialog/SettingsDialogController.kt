package com.lousseief.vault.controller.dialog

import com.lousseief.vault.dialog.SingleStringInputDialog
import com.lousseief.vault.list.SettingsCategoryListCellFactory
import com.lousseief.vault.model.ui.UiPasswordData
import com.lousseief.vault.model.ui.UiProfile
import com.lousseief.vault.utils.Colors
import com.lousseief.vault.utils.initializeSpinner
import com.lousseief.vault.utils.setupErrorMessageHandling
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIconView
import javafx.application.Platform
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.ButtonType
import javafx.scene.control.Dialog
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.control.Spinner
import javafx.scene.layout.ColumnConstraints
import javafx.scene.layout.GridPane
import javafx.scene.layout.VBox
import javafx.scene.paint.Paint

class SettingsDialogController(val user: UiProfile) {

    companion object {
        const val STRING_LENGTH_MAX = 100
        const val STRING_LENGTH_MIN = 1
    }

    private val savePasswordForMinutesProperty = SimpleIntegerProperty(user.settings.savePasswordForMinutes.value)
    private val errorProperty = SimpleStringProperty("")
    private val selectedCategory = SimpleStringProperty()

    @FXML
    private lateinit var okButton: ButtonType

    @FXML
    private lateinit var verticalHolder: VBox

    @FXML
    private lateinit var passwordLengthSpinner: Spinner<Int>

    @FXML
    private lateinit var dedupeTimeSpinner: Spinner<Int>

    @FXML
    private lateinit var dedupeTimeNoteLabel: Label

    @FXML
    private lateinit var categoriesView: ListView<String>

    @FXML
    private lateinit var newCategoryButton: Button

    @FXML
    private lateinit var removeCategoryButton: Button

    @FXML
    private lateinit var categoryButtonsPane: GridPane

    val categories = user.associations.value.values
        .groupBy {it.category.value }
        .mapValues { it.value.size }
        .filterKeys { !it.isNullOrEmpty() }

    private fun setupNewCategoryButton() {
        newCategoryButton.setOnAction {
            val newCat = SingleStringInputDialog(
                "Add category",
                "Enter a name for your new category",
                { input: String?, _: ActionEvent ->
                    if (user.settings.categories.any { input != "" && input !== null && it.compareTo(input, true) == 0 })
                        throw Exception("Category already exists, all categories must have unique names.")
                    else if (input === null || input.isEmpty())
                        throw Exception("Empty category name not allowed, please try again.")
                }
            ).showAndWait()
            if (newCat.isPresent) {
                user.settings.addCategory(newCat.get())
            }
        }
        newCategoryButton.graphic = MaterialDesignIconView(MaterialDesignIcon.PLUS_CIRCLE).apply {
            size = "16px"
            fill = Paint.valueOf(Colors.GREEN)
        }
    }

    private fun setupRemoveCategoryButton() {
        removeCategoryButton.disableProperty().bind(selectedCategory.isNull)
        removeCategoryButton.setOnAction {
            if(selectedCategory.value in categories) {
                throw Error("Category has associations bound to it")
            }
            user.settings.removeCategory(selectedCategory.value)
        }
        removeCategoryButton.graphic = MaterialDesignIconView(MaterialDesignIcon.MINUS_CIRCLE).apply {
            size = "16px"
            fill = Paint.valueOf("red")
        }

    }

    @FXML
    fun initialize() {
        categoriesView.itemsProperty().bind(user.settings.categories)
        categoriesView.cellFactory = SettingsCategoryListCellFactory(categories)
        selectedCategory.bind(categoriesView.selectionModel.selectedItemProperty())

        initializeSpinner(user.settings.passwordLength, passwordLengthSpinner, STRING_LENGTH_MAX, STRING_LENGTH_MIN)
        /* capped at the hard limit in UiPasswordData: anything above it would silently have no effect, since the hard
        expiry always wins over the sliding one. initializeSpinner clamps both the stepper and typed input to the max */
        initializeSpinner(
            savePasswordForMinutesProperty,
            dedupeTimeSpinner,
            UiPasswordData.MAX_SAVED_PASSWORD_MINUTES.toInt(),
            0
        )
        dedupeTimeNoteLabel.text =
            "Password is never reused for more than ${UiPasswordData.MAX_SAVED_PASSWORD_MINUTES} minutes"

        setupNewCategoryButton()
        setupRemoveCategoryButton()

        categoryButtonsPane.columnConstraints.add(0, ColumnConstraints().apply { percentWidth = 50.0 })
        categoryButtonsPane.columnConstraints.add(1, ColumnConstraints().apply { percentWidth = 50.0 })
    }

    fun finalize(readyDialog: Dialog<Int?>) {
        val icon = Label().apply {
            styleClass.addAll("alert", "confirmation", "dialog-pane") // can use "error" or "warning" instead of "confirmation" obv
        }
        readyDialog.headerText = "Profile settings"
        readyDialog.graphic = icon
        readyDialog.setResultConverter { type: ButtonType? ->
            /* the type is null when the dialog is closed with the window cross instead of the save button, since this
            dialog has no button with button data CANCEL_CLOSE or NO for JavaFX to substitute (the cross works at all
            only because there is exactly one button type). Treat that as "discard" and return null, mirroring
            DirectoryPathInputDialogController. Note that the other two settings in this dialog - default password
            length and the categories - are bound straight to the live profile settings and therefore apply
            immediately, no matter how the dialog is closed.
            See README, "Dialogs and the window close button". */
            when (type) {
                okButton -> savePasswordForMinutesProperty.value
                else -> null
            }
        }

        Platform.runLater{
            setupErrorMessageHandling(
                errorProperty,
                readyDialog.dialogPane.width,
                verticalHolder,
                1
            )
        }
    }

}

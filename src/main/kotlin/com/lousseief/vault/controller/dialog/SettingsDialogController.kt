package com.lousseief.vault.controller.dialog

import com.lousseief.vault.dialog.SingleStringInputDialog
import com.lousseief.vault.list.SettingsCategoryListCellFactory
import com.lousseief.vault.model.ui.UiProfile
import com.lousseief.vault.utils.Colors
import com.lousseief.vault.utils.initializeSpinner
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIconView
import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleStringProperty
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.ButtonType
import javafx.scene.control.Dialog
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.control.Spinner
import javafx.scene.layout.ColumnConstraints
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.paint.Paint
import javafx.scene.text.TextAlignment

class SettingsDialogController(val user: UiProfile) {

    companion object {
        const val STRING_LENGTH_MAX = 100
        const val STRING_LENGTH_MIN = 1
    }

    private val errorProperty = SimpleStringProperty("")
    private val selectedCategory = SimpleStringProperty()

    private val hasError = Bindings.createBooleanBinding(
        { !errorProperty.value.isEmpty() },
        errorProperty
    )

    @FXML
    private lateinit var okButton: ButtonType

    @FXML
    private lateinit var verticalHolder: VBox

    @FXML
    private lateinit var passwordLengthSpinner: Spinner<Int>

    @FXML
    private lateinit var dedupeTimeSpinner: Spinner<Int>

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
                user.settings.categories.value.add(newCat.get())
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
            user.settings.categories.remove(selectedCategory.value)
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
        initializeSpinner(user.settings.savePasswordForMinutes, dedupeTimeSpinner, 30, 0)

        setupNewCategoryButton()
        setupRemoveCategoryButton()

        categoryButtonsPane.columnConstraints.add(0, ColumnConstraints().apply { percentWidth = 50.0 })
        categoryButtonsPane.columnConstraints.add(1, ColumnConstraints().apply { percentWidth = 50.0 })
    }

    fun finalize(readyDialog: Dialog<Unit>) {
        val icon = Label().apply {
            styleClass.addAll("alert", "confirmation", "dialog-pane")
        }
        readyDialog.headerText = "Profile settings"
        readyDialog.graphic = icon

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
        }
    }

}

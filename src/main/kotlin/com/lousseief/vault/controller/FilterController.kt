package com.lousseief.vault.controller

import com.lousseief.vault.list.BooleanListButtonCell
import com.lousseief.vault.list.BooleanListCellFactory
import com.lousseief.vault.model.ui.UiAssociation
import javafx.application.Platform
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.scene.control.CheckBox
import javafx.scene.control.ComboBox
import javafx.scene.control.TextField
import javafx.scene.control.TitledPane
import javafx.scene.input.KeyEvent
import javafx.scene.layout.HBox
import java.util.function.Predicate

class FilterController(val onFilter: (pred: Predicate<UiAssociation>) -> Unit) {

    @FXML
    private lateinit var filterPane: TitledPane

    @FXML
    private lateinit var filterBox: HBox

    @FXML
    private lateinit var keywordField: TextField

    @FXML
    private lateinit var searchParameterField: ComboBox<String>

    @FXML
    private lateinit var isNeededField: ComboBox<String>

    @FXML
    private lateinit var shouldBeDeactivatedField: ComboBox<String>

    @FXML
    private lateinit var isDeactivatedField: ComboBox<String>

    @FXML
    private lateinit var useFilterCheckbox: CheckBox

    private val searchParameter = SimpleStringProperty("All")
    private val keyword = SimpleStringProperty("")
    private val isNeeded = SimpleStringProperty(null)
    private val shouldBeDeactivated = SimpleStringProperty(null)
    private val isDeactivated = SimpleStringProperty(null)
    private val useFilter = SimpleBooleanProperty(false)

    @FXML
    fun initialize() {
        filterPane.isCollapsible = true
        filterPane.isExpanded = false
        filterPane.isAnimated = false
        filterBox.prefWidthProperty().bind(filterPane.widthProperty().subtract(38))
        filterBox.maxWidthProperty().bind(filterPane.widthProperty().subtract(38))
        Platform.runLater {
            keyword.bind(keywordField.textProperty())
            searchParameter.bind(searchParameterField.valueProperty())
            isNeeded.bind(isNeededField.valueProperty())
            isDeactivated.bind(isDeactivatedField.valueProperty())
            shouldBeDeactivated.bind(shouldBeDeactivatedField.valueProperty())
            useFilter.bind(useFilterCheckbox.selectedProperty())

            keyword.addListener { _, _, _ -> onFilterChanged() }
            searchParameter.addListener { _, _, _ -> onFilterChanged() }
            isNeeded.addListener { _, _, _ -> onFilterChanged() }
            isDeactivated.addListener { _, _, _ -> onFilterChanged() }
            shouldBeDeactivated.addListener { _, _, _ -> onFilterChanged() }
            useFilter.addListener { _, _, _ -> onFilterChanged() }

            keywordField.addEventFilter(KeyEvent.KEY_PRESSED) { event ->
                println(keyword.isNotEmpty.value)
            }
            searchParameterField.items = FXCollections.observableArrayList(
                listOf(
                    "All",
                    "Main identifier",
                    "Secondary identifier",
                    "Category",
                    "Comment"
                )
            )
            searchParameterField.selectionModel.selectFirst()
            configureBooleanComboBox(isNeededField)
            configureBooleanComboBox(shouldBeDeactivatedField)
            configureBooleanComboBox(isDeactivatedField)
        }
    }

    private fun configureBooleanComboBox(comboBox: ComboBox<String>) {
        comboBox.items = FXCollections.observableArrayList(
            listOf("", "Yes", "No")
        )
        comboBox.cellFactory = BooleanListCellFactory()
        comboBox.buttonCell = BooleanListButtonCell()
        comboBox.selectionModel.selectFirst()
    }

    private fun entrySatisfiesBooleanFilters(it: UiAssociation): Boolean {
        return(
            ((isNeeded.get() == "Yes" && it.isNeeded.value) || (isNeeded.get() == "No" && !it.isNeeded.value) || (isNeeded.get() == "")) &&
            ((isDeactivated.get() == "Yes" && it.isDeactivated.value) || (isDeactivated.get() == "No" && !it.isDeactivated.value) || (isDeactivated.get() == "")) &&
            ((shouldBeDeactivated.get() == "Yes" && it.shouldBeDeactivated.value) || (shouldBeDeactivated.get() == "No" && !it.shouldBeDeactivated.value) || (shouldBeDeactivated.get() == ""))
        )
    }

    private fun entrySatisfiesKeywordFilter(it: UiAssociation): Boolean {
        if(keyword.value.isNullOrEmpty()) {
            return true
        } else {
            val regex = Regex(keyword.value.replace("*", ".+"))
            if(searchParameter.value == "All") {
                return (
                    regex.containsMatchIn(it.mainIdentifier.value) ||
                    it.comment.value !== null && regex.containsMatchIn(it.comment.value) ||
                    it.secondaryIdentifiers.any { regex.containsMatchIn(it) } ||
                    it.category.value !== null && regex.containsMatchIn(it.category.value)
                    )
            } else {
                return (
                    (searchParameter.value == "Main identifier" && regex.containsMatchIn(it.mainIdentifier.value)) ||
                    (searchParameter.value == "Comment" && it.comment.value !== null && regex.containsMatchIn(it.comment.value)) ||
                    (searchParameter.value == "Secondary identifier" && it.secondaryIdentifiers.any { regex.containsMatchIn(it) }) ||
                    (searchParameter.value == "Category" && it.category.value !== null && regex.containsMatchIn(it.category.value))
                )
            }
        }
    }

    private fun onFilterChanged() {
        if (useFilter.value) {
            try {
                onFilter {
                    val shouldBeShown = entrySatisfiesBooleanFilters(it) && entrySatisfiesKeywordFilter(it)
                    shouldBeShown
                }
            }
            catch(e: Exception) {
                e.printStackTrace()
            }
        } else {
            onFilter { true }
        }
    }
}


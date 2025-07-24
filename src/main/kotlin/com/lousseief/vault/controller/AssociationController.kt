package com.lousseief.vault.controller

import com.lousseief.vault.dialog.SingleStringInputDialog
import com.lousseief.vault.list.AssociationCategoryButtonCell
import com.lousseief.vault.list.AssociationCategoryListCellFactory
import com.lousseief.vault.model.ui.UiProfile
import com.lousseief.vault.model.ui.UiAssociation
import com.lousseief.vault.model.ui.UiCredential
import com.lousseief.vault.utils.Colors
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIconView
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.CheckBox
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.control.Separator
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.layout.ColumnConstraints
import javafx.scene.layout.GridPane
import javafx.scene.layout.VBox
import javafx.scene.paint.Paint
import javafx.scene.text.TextAlignment
import javafx.stage.Modality
import javafx.stage.Stage
import kotlin.text.equals

class AssociationController(val user: UiProfile, val association: UiAssociation, var onDeleteAssociation: () -> Unit) {

    // fields

    @FXML
    private lateinit var mainIdentifier: TextField

    @FXML
    private lateinit var secondaryIdentifiers: ListView<String>

    @FXML
    private lateinit var category: ComboBox<String>

    @FXML
    private lateinit var comments: TextArea

    @FXML
    private lateinit var isNeeded: CheckBox

    @FXML
    private lateinit var shouldBeDeactivated: CheckBox

    @FXML
    private lateinit var isDeactivated: CheckBox

    // ui components

    @FXML
    private lateinit var header: Label

    @FXML
    private lateinit var leftSideButtonPane: GridPane

    @FXML
    private lateinit var itemContainer: VBox

    @FXML
    private lateinit var leftSideVertical: VBox

    @FXML
    private lateinit var rightSideVertical: VBox

    @FXML
    private lateinit var verticalSeparator: Separator

    // buttons

    @FXML
    private lateinit var credentialsButton: Button

    @FXML
    private lateinit var deleteAssociationButton: Button

    @FXML
    private lateinit var addSecondaryIdentifierButton: Button

    @FXML
    private lateinit var removeSecondaryIdentifierButton: Button

    @FXML
    private lateinit var newCategoryButton: Button

    @FXML
    private lateinit var updateMainIdentifierButton: Button

    private val headerProperty = Bindings.createStringBinding(
        {
            val currentMainIdentifier = association.mainIdentifier.value
            if(currentMainIdentifier.isNotEmpty()) {
                "Association: $currentMainIdentifier"
            } else {
                "Association: (unnamed association)"
            }
        },
        association.mainIdentifier
    )

    private val categories = Bindings.createObjectBinding(
        { FXCollections.observableArrayList(listOf("") + user.settings.categories) },
        user.settings.categories
    )

    // interactions

    private val selectedSecondIdentifier = SimpleStringProperty()
    private val isSecondIdentifierSelected = Bindings.createBooleanBinding(
        { !selectedSecondIdentifier.value.isNullOrEmpty() },
        selectedSecondIdentifier
    )

    /*private val setCredentialsHandler = {
            password: String, _: ActionEvent? ->
        val credentials = user.getCredentials(originalMainIdentifier.value, password)
        credentialsController.setCredentials(credentials)
        credentialsController.setUserNames(controller.user!!.userNames)
    }*/


    private fun setupRemoveSecondIdentifierButton() {
        removeSecondaryIdentifierButton.disableProperty().bind(!isSecondIdentifierSelected)
        removeSecondaryIdentifierButton.setOnAction {
            if (selectedSecondIdentifier.value.isNotEmpty()) {
                association.secondaryIdentifiers.remove(selectedSecondIdentifier.value)
            }
        }
        removeSecondaryIdentifierButton.graphic = MaterialDesignIconView(MaterialDesignIcon.MINUS_CIRCLE).apply {
            size = "16px"
            fill = Paint.valueOf("red")
        }
    }

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
        newCategoryButton.graphic = MaterialDesignIconView(MaterialDesignIcon.CREATION).apply {
            size = "16px"
            fill = Paint.valueOf(Colors.GOLD)
        }
    }

    private fun setupCredentialsButton() {
        credentialsButton.setOnAction {
            user.passwordRequiredAction()?.let { password ->
                val vault = user.accessVault(password)
                val credentials = (vault.second[association.mainIdentifier.value]?.credentials ?: emptyList()).toMutableList()
                val uiCredentials = credentials.map(UiCredential::fromCredentials)
                val stage = Stage()
                val loader = FXMLLoader(javaClass.getResource("/Credentials.fxml"))
                loader.setController(
                    CredentialsController(
                        user,
                        association.savedAssociation.mainIdentifier,
                        association,
                        FXCollections.observableArrayList(uiCredentials),
                        credentials,
                        { stage.close() }
                    )
                )
                val credentialsView: Parent = loader.load()
                stage.scene = Scene(credentialsView)
                stage.scene.stylesheets.add("/styles/styles.css")
                stage.title = "Credentials"
                stage.isResizable = false
                stage.initModality(Modality.WINDOW_MODAL)
                stage.initOwner((it.source as Node).scene.window)
                stage.maxWidth = 330.0
                stage.minWidth = 330.0
                stage.maxHeight = 614.0
                stage.minHeight = 614.0
                stage.showAndWait()
                /*val credentialsSet = controller.passwordRequiredAction(setCredentialsHandler)
                if(credentialsSet) {
                    // credentialsController loaded and ready
                    find<CredentialsView>(mapOf(CredentialsView::mainIdentifier to originalMainIdentifier))
                        .openModal(
                            block = true,
                            resizable = false
                        )
                    if(credentialsController.credentialsSaved) // signal that saving the entire vault is now possible
                        controller.altered.set(true)
                }*/
            }
        }
        credentialsButton.graphic = FontAwesomeIconView(FontAwesomeIcon.KEY).apply { fill = Paint.valueOf(Colors.GOLD)}
    }

    private fun setupUpdateMainIdentifierButton() {
        updateMainIdentifierButton.setOnAction {
            val updatedMainIdentifier = SingleStringInputDialog(
                "Update main identifier",
                "Enter a new name for your main identifier",
                { input: String?, _: ActionEvent ->
                    if (input.isNullOrEmpty())
                        throw Exception("Empty main identifier not allowed, please try again.")
                    else if (user.associations.values.any { it.mainIdentifier.value.equals(input, true) })
                        throw Exception("Main identifier already exists, main identifiers must be unique within a profile.")
                }
            ).showAndWait()
            if (updatedMainIdentifier.isPresent) {
                association.mainIdentifier.set(updatedMainIdentifier.get())
                user.orderedAssociations.sortWith { a, b -> a.mainIdentifier.value.compareTo(b.mainIdentifier.value) }
            }
        }
        addSecondaryIdentifierButton.graphic = MaterialDesignIconView(MaterialDesignIcon.PLUS_CIRCLE).apply {
            size = "16px"
            fill = Paint.valueOf(Colors.GREEN)
        }
    }

    private fun setupAddSecondIdentifierButton() {
        addSecondaryIdentifierButton.setOnAction {
            val newId = SingleStringInputDialog(
                "Add secondary identifier",
                "Enter a name for your new secondary identifier",
                { input: String?, _: ActionEvent ->
                if (input.isNullOrEmpty())
                    throw Exception("Empty secondary identifier not allowed, please try again.")
                else if (association.secondaryIdentifiers.any { it.equals(input, true) })
                    throw Exception("Secondary identifier already exists, all secondary identifier must be unique.")
                }
            ).showAndWait()
            if (newId.isPresent) {
                association.secondaryIdentifiers.add(newId.get())
                association.secondaryIdentifiers.sortWith { a, b -> a.compareTo(b) }
            }
        }
        addSecondaryIdentifierButton.graphic = MaterialDesignIconView(MaterialDesignIcon.PLUS_CIRCLE).apply {
            size = "16px"
            fill = Paint.valueOf(Colors.GREEN)
        }
    }

    @FXML
    fun initialize() {
        header.textProperty().bind(headerProperty)

        setupAddSecondIdentifierButton()
        setupRemoveSecondIdentifierButton()
        setupNewCategoryButton()
        setupCredentialsButton()
        setupUpdateMainIdentifierButton()

        mainIdentifier.textProperty().bindBidirectional(association.mainIdentifier)
        secondaryIdentifiers.items = association.secondaryIdentifiers
        category.valueProperty().bindBidirectional(association.category)
        comments.textProperty().bindBidirectional(association.comment)
        isNeeded.selectedProperty().bindBidirectional(association.isNeeded)
        isDeactivated.selectedProperty().bindBidirectional(association.isDeactivated)
        shouldBeDeactivated.selectedProperty().bindBidirectional(association.shouldBeDeactivated)
        secondaryIdentifiers.placeholder = Label("No secondary identifiers").apply {
            textAlignment = TextAlignment.CENTER
            isWrapText = true
        }
        selectedSecondIdentifier.bind(secondaryIdentifiers.selectionModel.selectedItemProperty())
        leftSideButtonPane.columnConstraints.add(0, ColumnConstraints().apply { percentWidth = 50.0 })
        leftSideButtonPane.columnConstraints.add(1, ColumnConstraints().apply { percentWidth = 50.0 })
        category.itemsProperty().bind(categories)
        category.cellFactory = AssociationCategoryListCellFactory()
        category.buttonCell = AssociationCategoryButtonCell()
        category.placeholder = Label("No categories so far")
        //category.promptText = "Select category"
        category.selectionModel.selectFirst()

        deleteAssociationButton.graphic = FontAwesomeIconView(FontAwesomeIcon.TRASH).apply {
            size = "20px"
            fill = Paint.valueOf("white")
        }
        deleteAssociationButton.setOnAction {
            println("Delete association")
            onDeleteAssociation()

        }
    }
}

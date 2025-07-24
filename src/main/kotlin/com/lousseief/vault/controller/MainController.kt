package com.lousseief.vault.controller

import com.lousseief.vault.Router
import com.lousseief.vault.dialog.ChangeMasterPasswordDialog
import com.lousseief.vault.dialog.ChooseProfilesLocationDialog
import com.lousseief.vault.dialog.Dialogs
import com.lousseief.vault.dialog.SettingsDialog
import com.lousseief.vault.dialog.SingleStringInputDialog
import com.lousseief.vault.dialog.StringGeneratorDialog
import com.lousseief.vault.list.AssociationsListCellFactory
import com.lousseief.vault.model.ui.UiProfile
import com.lousseief.vault.model.ui.UiAssociation
import com.lousseief.vault.service.FileService
import com.lousseief.vault.utils.Colors
import com.lousseief.vault.utils.Font
import com.lousseief.vault.utils.timeToStringDateTime
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIconView
import de.jensd.fx.glyphs.materialicons.MaterialIcon
import de.jensd.fx.glyphs.materialicons.MaterialIconView
import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.collections.transformation.FilteredList
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.control.*
import javafx.scene.layout.VBox
import javafx.scene.paint.Paint
import java.util.concurrent.Callable
import kotlin.text.isNotEmpty


class MainController(private val router: Router, private val user: UiProfile) {

    @FXML
    private lateinit var logoutButton: Button

    @FXML
    private lateinit var directoryChooserButton: Button

    @FXML
    private lateinit var addAssociationButton: Button

    @FXML
    private lateinit var settingsButton: Button

    @FXML
    private lateinit var changeMasterPasswordButton: Button

    @FXML
    private lateinit var stringGeneratorButton: Button

    @FXML
    private lateinit var exportVaultButton: Button

    @FXML
    private lateinit var saveVaultButton: Button

    @FXML
    private lateinit var associationsPane: TitledPane

    @FXML
    private lateinit var associationsList: ListView<UiAssociation>

    @FXML
    private lateinit var sideBarBox: VBox

    @FXML
    private lateinit var listviewPlaceholderLabel: Label

    @FXML
    private lateinit var nextLoginTimeLabel: Label

    @FXML
    private lateinit var associationViewContainer: VBox

    @FXML
    private lateinit var productName: Label

    private val filtered = FilteredList(user.orderedAssociations)
    private val nextLoginTimeProperty = Bindings.createStringBinding(
        { "Current entered password valid for further operations until: ${user.passwordData.savedPasswordExpiry.value?.let { timeToStringDateTime(it) } ?: "expired" }" },
        user.passwordData.savedPasswordExpiry
    )

    val placeholderFn = Callable {
        println("Evaluating placeholder!")
        if(filtered.isEmpty() && user.orderedAssociations.isEmpty()) {
            "No associations in table.\n Press \"Create new associations\" to create your first association"
        } else {
            "No associations were matched by your filter.\n Please try again!"
        }
    }

    val listViewPlaceholder = Bindings.createStringBinding(
        placeholderFn,
        filtered, user.orderedAssociations
    )

    private fun loadFilterView() {
        val loader = FXMLLoader(javaClass.getResource("/Filter.fxml"))
        loader.setController(FilterController({
            val currentSelection = associationsList.selectionModel.selectedItem
            filtered.predicate = it
            // selected item loses focus when we alter the filter so we solve this below
            if(currentSelection in filtered) {
                associationsList.selectionModel.select(currentSelection)
            } else {
                associationsList.selectionModel.clearSelection()
            }

        }))
        val filterView: Parent = loader.load()
        sideBarBox.children.add(0, filterView)
    }

    private fun loadAssociationView(association: UiAssociation?) {
        if(association != null) {
            val loader = FXMLLoader(javaClass.getResource("/Association.fxml"))
            loader.setController(AssociationController(user, association, { onDeleteAssociation(association.savedAssociation.mainIdentifier) }))
            val associationView: Parent = loader.load()
            associationViewContainer.children.removeIf { true }
            associationViewContainer.children.add(associationView)
        } else {
            val label = Label(
                if(user.orderedAssociations.isEmpty())
                    "Please add associations to the vault and then select one to show its content here"
                else
                    "Select an association to view and edit it"
            )
            associationViewContainer.children.removeIf { true }
            associationViewContainer.children.add(label)
        }
    }

    private fun setupStringGeneratorButton() {
        stringGeneratorButton.setOnAction {
            StringGeneratorDialog(user.settings.passwordLength.value).showAndWait()
        }
    }

    private fun setupAddAssociationButton() {
        addAssociationButton.setOnAction {
            val newAssociation = SingleStringInputDialog(
                "Add association",
                "Enter an identifier for your new association",
                { input: String?, ev: ActionEvent ->
                    if (user.associations.value.keys.any {
                        input != "" && input !== null && it.compareTo(input, true) == 0
                    }) {
                        throw Exception("Association already exists, all associations must have unique main identifiers.")
                    } else if (input === null || input.isBlank()) {
                        throw Exception("Empty identifier not allowed, please try again.")
                    }
                }
            ).showAndWait()
            if (newAssociation.isPresent) {
                val newIdentifier = newAssociation.get()
                user.passwordRequiredAction()?.let { password ->
                    user.addAssociation(newIdentifier, password)
                }
                user.orderedAssociations.add(user.associations[newIdentifier])
                user.orderedAssociations.sortWith { a, b -> a.mainIdentifier.value.compareTo(b.mainIdentifier.value) }
                val index = filtered.indexOfFirst { it.mainIdentifier.value == newIdentifier }
                associationsList.selectionModel.select(index)
                associationsList.requestFocus()
            }
        }
        addAssociationButton.graphic = MaterialDesignIconView(MaterialDesignIcon.SHAPE_SQUARE_PLUS).apply {
            size = "16px"
            fill = Paint.valueOf("white")
        }
    }

    private fun onDeleteAssociation(savedMainIdentifier: String) {
        val delete = Dialogs.openConfirmSensitiveOperationDialog(
            "Delete association",
            null,
            "Confirm delete",
            "Are you sure you want to delete this association?"
        )
        if(delete) {
            user.passwordRequiredAction()?.let { password ->
                user.removeAssociation(savedMainIdentifier, password)
            }
            user.orderedAssociations.removeIf { it.mainIdentifier.value == savedMainIdentifier }
            associationsList.selectionModel.clearSelection()
            associationsList.requestFocus()
            Alert(Alert.AlertType.INFORMATION).apply {
                title = "Success"
                headerText = "Association successfully removed"
                contentText =
                    "The association was successfully deleted. If you don't save the vault, the delete association will reappear on next login. Upon saving the vault, the association cannot be restored."
            }.showAndWait()
        }
    }

    private fun setupChangeMasterPasswordButton() {
        changeMasterPasswordButton.setOnAction {
            val newPassword = ChangeMasterPasswordDialog { oldPassword, newPassword ->
                user.accessVault(oldPassword, null, true, newPassword)
            }.showAndWait()
            if(newPassword.isPresent) {
                Alert(Alert.AlertType.INFORMATION).apply {
                    title = "Success"
                    headerText = "Master password successfully changed"
                    contentText = "The master password was updated, the vault must be saved to disk before quitting."
                }.showAndWait()
            }
        }
    }

    private fun setupSettingsDialogButton() {
        settingsButton.setOnAction {
            SettingsDialog(user).showAndWait()
        }
        settingsButton.graphic = FontAwesomeIconView(FontAwesomeIcon.COG).apply {
            fill = Paint.valueOf(Colors.GRAY_DARK)
            size = "18px"
        }
    }

    private fun onChooseProfileLocation() {
        val directory = ChooseProfilesLocationDialog(false)
            .showAndWait()
        if(directory.isPresent && directory.get().isNotEmpty() && directory.get() != FileService.getCurrentProfilesLocation()) {
            Alert(Alert.AlertType.INFORMATION).apply {
                title = "Success"
                headerText = "Success"
                contentText = "Successfully set the profiles location to '${directory.get()}'"
            }.showAndWait()
            FileService.writeSystemSettingsFile(directory.get())
        }
    }

    private fun onTerminateSession(isCloseWindowRequest: Boolean): Boolean {
        if(user.isDirty.value) {
            return Dialogs.openConfirmSensitiveOperationDialog(
                "Close anyway",
                null,
                if(isCloseWindowRequest) "Do you really want to close this window?" else "Do you really want to end this session?",
                "You have unsaved changes in your vault which will be lost if you exit without saving, do you really want to ${if(isCloseWindowRequest) "close this window" else "end this session"} without saving first? If no, press \"Cancel\" and then press \"Save vault to disk\"."
            )
        }
        return true
    }

    @FXML
    fun initialize() {
        productName.font = Font.BitCount
        logoutButton.setOnAction {
            val proceed = onTerminateSession(false)
            if (proceed) {
                router.showLogin()
            }
        }
        logoutButton.graphic = MaterialIconView(MaterialIcon.EXIT_TO_APP).apply { fill = Paint.valueOf(Colors.GRAY_DARK)}
        associationsPane.isCollapsible = false

        // inject filter view
        loadFilterView()

        setupStringGeneratorButton()
        setupChangeMasterPasswordButton()
        setupAddAssociationButton()
        setupSettingsDialogButton()

        nextLoginTimeLabel.textProperty().bind(nextLoginTimeProperty)
        associationsList.maxWidth = Double.MAX_VALUE //associationsPane.width
        associationsList.items = filtered
        associationsList.cellFactory = AssociationsListCellFactory(associationsList)
        //textOverrun = OverrunStyle.ELLIPSIS
        //maxWidth= ti.width
        associationsList.selectionModel.selectedItemProperty().addListener { _, _, newValue ->
            loadAssociationView(newValue)
        }

        exportVaultButton.setOnAction {
            user.passwordRequiredAction(true)?.let { password ->
                val vault = user.accessVault(password)
                val exportFilename = user.export(vault)
                Alert(Alert.AlertType.INFORMATION).apply {
                    title = "Success"
                    headerText = "Vault successfully exported"
                    contentText = "The vault was successfully exported to file '$exportFilename'"
                }.showAndWait()
            }
        }
        exportVaultButton.graphic = MaterialIconView(MaterialIcon.IMPORT_EXPORT).apply {
            fill = Paint.valueOf(Colors.BLUE)
            size = "20px"
        }

        saveVaultButton.setOnAction {
            user.save()
        }
        saveVaultButton.graphic = MaterialIconView(MaterialIcon.SAVE).apply {
            fill = Paint.valueOf(Colors.BLUE)
            size = "22px"
        }
        saveVaultButton.disableProperty().bind(user.isDirty.not())

        directoryChooserButton.setOnAction { onChooseProfileLocation() }
        Platform.runLater {
            if(filtered.isEmpty()) {
                loadAssociationView(null)
            } else {
                associationsList.selectionModel.selectFirst()
            }
            associationsList.requestFocus()

            associationViewContainer.scene.window.setOnCloseRequest {
                val proceed = onTerminateSession(true)
                if(!proceed) {
                    it.consume()
                }
            }
        }
        listviewPlaceholderLabel.textProperty().bind(listViewPlaceholder)
    }
}


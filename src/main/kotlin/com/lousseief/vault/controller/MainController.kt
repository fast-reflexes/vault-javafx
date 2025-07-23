package com.lousseief.vault.controller

import com.lousseief.vault.Router
import com.lousseief.vault.dialog.ChangeMasterPasswordDialog
import com.lousseief.vault.dialog.ChooseProfilesLocationDialog
import com.lousseief.vault.dialog.SettingsDialog
import com.lousseief.vault.dialog.SingleStringInputDialog
import com.lousseief.vault.dialog.StringGeneratorDialog
import com.lousseief.vault.list.EntryListCellFactory
import com.lousseief.vault.model.UiProfile
import com.lousseief.vault.model.ui.UiAssociation
import com.lousseief.vault.service.FileService
import com.lousseief.vault.utils.Colors
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
    private lateinit var entriesPane: TitledPane

    @FXML
    private lateinit var entriesList: ListView<UiAssociation>

    @FXML
    private lateinit var sideBarBox: VBox

    @FXML
    private lateinit var listviewPlaceholderLabel: Label

    @FXML
    private lateinit var nextLoginTimeLabel: Label

    @FXML
    private lateinit var associationViewContainer: VBox

    private val filtered = FilteredList(user.mappedEntries)
    private val nextLoginTimeProperty = Bindings.createStringBinding(
        { "Current entered password valid for further operations until: ${user.passwordData.savedPasswordExpiry.value?.let { timeToStringDateTime(it) } ?: "expired" }" },
        user.passwordData.savedPasswordExpiry
    )

    val placeholderFn = Callable {
        println("Evaluating placeholder!")
        if(filtered.isEmpty() && user.mappedEntries.isEmpty()) {
            "No entries in table.\n Press \"Create new entry\" to create your first entry"
        } else {
            "No entries were matched by your filter.\n Please try again!"
        }
    }

    val listViewPlaceholder = Bindings.createStringBinding(
        placeholderFn,
        filtered, user.mappedEntries
    )

    private fun loadFilterView() {
        val loader = FXMLLoader(javaClass.getResource("/Filter.fxml"))
        loader.setController(FilterController({
            val currentSelection = entriesList.selectionModel.selectedItem
            filtered.predicate = it
            // selected item loses focus when we alter the filter so we solve this below
            if(currentSelection in filtered) {
                entriesList.selectionModel.select(currentSelection)
            } else {
                entriesList.selectionModel.clearSelection()
            }

        }))
        val filterView: Parent = loader.load()
        sideBarBox.children.add(0, filterView)
    }

    private fun loadAssociationView(association: UiAssociation?) {
        if(association != null) {
            val loader = FXMLLoader(javaClass.getResource("/Association.fxml"))
            loader.setController(AssociationController(user, association, { onDeleteEntry(association.savedAssociation.mainIdentifier) }))
            val entryView: Parent = loader.load()
            associationViewContainer.children.removeIf { true }
            associationViewContainer.children.add(entryView)
        } else {
            val label = Label(
                if(user.mappedEntries.isEmpty())
                    "Please add entries to the vault and then select one to show its content here"
                else
                    "Select an entry to view and edit it"
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
            val newEntry = SingleStringInputDialog(
                "Add entry",
                "Enter an identifier for your new entry",
                { input: String?, ev: ActionEvent ->
                    if (user.associations.value.keys.any {
                        input != "" && input !== null && it.compareTo(input, true) == 0
                    }) {
                        throw Exception("Entry already exists, all entries must have unique names.")
                    } else if (input === null || input.isBlank()) {
                        throw Exception("Empty identifier not allowed, please try again.")
                    }
                }
            ).showAndWait()
            if (newEntry.isPresent) {
                val newIdentifier = newEntry.get()
                user.passwordRequiredAction()?.let { password ->
                    user.addAssociation(newIdentifier, password)
                }
                user.mappedEntries.add(user.associations[newIdentifier])
                user.mappedEntries.sortWith { a, b -> a.mainIdentifier.value.compareTo(b.mainIdentifier.value) }
                val index = filtered.indexOfFirst { it.mainIdentifier.value == newIdentifier }
                entriesList.selectionModel.select(index)
                entriesList.requestFocus()
            }
        }
        addAssociationButton.graphic = MaterialDesignIconView(MaterialDesignIcon.SHAPE_SQUARE_PLUS).apply {
            size = "16px"
            fill = Paint.valueOf("white")
        }
    }

    private fun onDeleteEntry(savedMainIdentifier: String) {
        val deleteButtonType = ButtonType("Delete entry", ButtonBar.ButtonData.OK_DONE)
        val deleteEntryDialog = Alert(Alert.AlertType.WARNING).apply {
            headerText = "Confirm delete"
            contentText = "Are you sure you want to delete this entry?"
            dialogPane.buttonTypes.add(ButtonType.CANCEL)
            dialogPane.buttonTypes.remove(ButtonType.OK)
            dialogPane.buttonTypes.add(deleteButtonType)
        }
        val res = deleteEntryDialog.showAndWait()
        if(res.isPresent && res.get() === deleteButtonType) {
            user.passwordRequiredAction()?.let { password ->
                user.removeAssociation(savedMainIdentifier, password)
            }
            user.mappedEntries.removeIf { it.mainIdentifier.value == savedMainIdentifier }
            entriesList.selectionModel.clearSelection()
            entriesList.requestFocus()
            Alert(Alert.AlertType.INFORMATION).apply {
                headerText = "Entry successfully removed"
                contentText =
                    "The entry was successfully deleted. If you don't save the vault, the delete entry will reappear on next login. Upon saving the vault, the entry cannot be restored."
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
                headerText = "Success"
                contentText = "Successfully set the profiles location to '${directory.get()}'"
            }.showAndWait()
            FileService.writeSystemSettingsFile(directory.get())
        }
    }

    @FXML
    fun initialize() {
        logoutButton.setOnAction { router.showLogin() }
        logoutButton.graphic = MaterialIconView(MaterialIcon.EXIT_TO_APP).apply { fill = Paint.valueOf(Colors.GRAY_DARK)}
        entriesPane.isCollapsible = false

        // inject filter view
        loadFilterView()

        setupStringGeneratorButton()
        setupChangeMasterPasswordButton()
        setupAddAssociationButton()
        setupSettingsDialogButton()

        nextLoginTimeLabel.textProperty().bind(nextLoginTimeProperty)
        entriesList.maxWidth = Double.MAX_VALUE //entriesPane.width
        entriesList.items = filtered
        entriesList.cellFactory = EntryListCellFactory(entriesList)
        //textOverrun = OverrunStyle.ELLIPSIS
        //maxWidth= ti.width
        entriesList.selectionModel.selectedItemProperty().addListener { _, _, newValue ->
            loadAssociationView(newValue)
        }

        exportVaultButton.setOnAction {
            user.passwordRequiredAction(true)?.let { password ->
                val vault = user.accessVault(password)
                val exportFilename = user.export(vault)
                Alert(Alert.AlertType.INFORMATION).apply {
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
                entriesList.selectionModel.selectFirst()
            }
            entriesList.requestFocus()

            associationViewContainer.scene.window.setOnCloseRequest {
                if(user.isDirty.value) {
                    val closeAnyway = ButtonType("Close anyway", ButtonBar.ButtonData.OK_DONE)
                    val cancel = ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE)
                    val confirm = Alert(
                        Alert.AlertType.WARNING,
                        "You have unsaved changes in your vault which will be lost if you exit without saving, do you really want to close this window without saving first? If no, press \"Cancel\" and then press \"Save vault to disk\".",
                        cancel, closeAnyway
                    ).apply {
                        (dialogPane.lookupButton(closeAnyway) as Button).isDefaultButton = false
                        (dialogPane.lookupButton(cancel) as Button).isDefaultButton = true
                        headerText = "Do you really want to close this window?"
                    }.showAndWait()
                    if(confirm.isPresent && confirm.get() != closeAnyway) {
                        it.consume()
                    }
                }
            }
        }
        listviewPlaceholderLabel.textProperty().bind(listViewPlaceholder)
    }
}


package com.lousseief.vault.controller

import com.lousseief.vault.Router
import com.lousseief.vault.dialog.ChangeMasterPasswordDialog
import com.lousseief.vault.dialog.SettingsDialog
import com.lousseief.vault.dialog.SingleStringInputDialog
import com.lousseief.vault.dialog.StringGeneratorDialog
import com.lousseief.vault.list.EntryListCellFactory
import com.lousseief.vault.model.UiProfile
import com.lousseief.vault.model.ui.UiAssociation
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
import javafx.collections.FXCollections
import javafx.collections.transformation.FilteredList
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.control.*
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.layout.VBox
import javafx.scene.paint.Paint
import javafx.stage.DirectoryChooser
import javafx.stage.Stage
import java.io.File
import java.util.concurrent.Callable


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
    private lateinit var entryViewContainer: VBox

    private val mappedEntries = FXCollections.observableList<UiAssociation>(
        mutableListOf(),
        { assoc -> arrayOf(assoc.mainIdentifier) }
    ).apply {
        addAll(user.associations.value.values)
        sortWith { a, b -> a.mainIdentifier.value.compareTo(b.mainIdentifier.value) }
    }

    private val filtered = FilteredList(mappedEntries)
    private val nextLoginTimeProperty = Bindings.createStringBinding(
        { "Current entered password valid for further operations until: ${user.passwordData.savedPasswordExpiry.value?.let { timeToStringDateTime(it) } ?: "expired" }" },
        user.passwordData.savedPasswordExpiry
    )

    val placeholderFn = Callable {
        println("Evaluating placeholder!")
        if(filtered.isEmpty() && mappedEntries.isEmpty()) {
            "No entries in table.\n Press \"Create new entry\" to create your first entry"
        } else {
            "No entries were matched by your filter.\n Please try again!"
        }
    }

    val listViewPlaceholder = Bindings.createStringBinding(
        placeholderFn,
        filtered, mappedEntries
    )

    private fun loadFilterView() {
        val loader = FXMLLoader(javaClass.getResource("/Filter.fxml"))
        loader.setController(FilterController({ filtered.predicate = it }))
        val filterView: Parent = loader.load()
        sideBarBox.children.add(0, filterView)
    }

    private fun loadItemView(association: UiAssociation?) {
        if(association != null) {
            val loader = FXMLLoader(javaClass.getResource("/Entry.fxml"))
            loader.setController(EntryController(user, association, { onDeleteEntry(association.mainIdentifier.value) }))
            val entryView: Parent = loader.load()
            entryViewContainer.children.removeIf { true }
            entryViewContainer.children.add(entryView)
        } else {
            val label = Label(
                if(mappedEntries.isEmpty())
                    "Please add entries to the vault and then select one to show its content here"
                else
                    "Select an entry to view and edit it"
            )
            entryViewContainer.children.removeIf { true }
            entryViewContainer.children.add(label)
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
                    user.passwordRequiredAction()?.let { password ->
                        user.addEntry(input, password)
                    }
                }
            ).showAndWait()
            if (newEntry.isPresent) {
                mappedEntries.add(user.associations[newEntry.get()])
                mappedEntries.sortWith { a, b -> a.mainIdentifier.value.compareTo(b.mainIdentifier.value) }
                val index = filtered.indexOfFirst { it.mainIdentifier.value == newEntry.get() }
                entriesList.selectionModel.select(index)
                entriesList.requestFocus()
            }
        }
        addAssociationButton.graphic = MaterialDesignIconView(MaterialDesignIcon.SHAPE_SQUARE_PLUS).apply {
            size = "16px"
            fill = Paint.valueOf("white")
        }
    }

    private fun setupChangeMasterPasswordButton() {
        changeMasterPasswordButton.setOnAction {
            val newPassword = ChangeMasterPasswordDialog {
                    oldPassword, newPassword ->
                println(oldPassword)
                println(newPassword)
                user.accessVault(oldPassword, null, true, newPassword)
                //controller.changeMasterPassword(oldPassword, newPassword)
            }.showAndWait()
            if(newPassword.isPresent) {
                println("Showing alert")
                Alert(Alert.AlertType.INFORMATION).apply {
                    headerText = "Master password successfully changed"
                    contentText = "The master password was updated, the vault must be saved to disk before quitting."
                }.showAndWait()
                //controller.altered.set(true)
            }
        }
    }

    private fun setupSettingsDialogButton() {
        settingsButton.setOnAction {
            SettingsDialog(user) { oldPassword, newPassword ->
                user.accessVault(oldPassword, null, true, newPassword)
                //controller.changeMasterPassword(oldPassword, newPassword)
            }.showAndWait()
        }
        settingsButton.graphic = FontAwesomeIconView(FontAwesomeIcon.COG).apply {
            fill = Paint.valueOf(Colors.GRAY_DARK)
            size = "18px"
        }
    }

    private fun onDeleteEntry(mainIdentifier: String) {
        user.passwordRequiredAction()?.let { password ->
            val deleteButtonType = ButtonType("Delete entry", ButtonBar.ButtonData.OK_DONE)
            val deleteEntryDialog = Alert(Alert.AlertType.WARNING).apply {
                headerText = "Confirm delete"
                contentText = "Are you sure you want to delete this entry?"
                dialogPane.buttonTypes.add(ButtonType.CANCEL)
                dialogPane.buttonTypes.remove(ButtonType.OK)
                dialogPane.buttonTypes.add(deleteButtonType)
                dialogPane
                    .lookupButton(deleteButtonType)
                    .addEventFilter(ActionEvent.ACTION) {
                        user.accessVault(
                            password,
                            { (settings, vaultData) ->
                                val assoc = vaultData[mainIdentifier]
                                println(assoc)
                                assoc?.credentials?.forEach {
                                    it.identities.forEach {
                                        user.removeUsername(it)
                                    }
                                }
                                Pair(settings, vaultData)
                            }
                        )
                        println(user.associations[mainIdentifier])
                        user.associations.remove(mainIdentifier)
                        println("AFTER")
                        println(user.associations[mainIdentifier])
                    }
            }
            val res = deleteEntryDialog.showAndWait()
            if(res.isPresent && res.get() === deleteButtonType) {
                mappedEntries.removeIf { it.mainIdentifier.value == mainIdentifier }
                entriesList.selectionModel.select(null)
                entriesList.requestFocus()
                Alert(Alert.AlertType.INFORMATION).apply {
                    headerText = "Entry successfully removed"
                    contentText =
                        "The entry was successfully deleted. If you don't save the vault, the delete entry will reappear on next login. Upon saving the vault, the entry cannot be restored."
                }.showAndWait()
            }
        }
    }

    @FXML
    fun initialize() {
        logoutButton.setOnAction { router.showLogin() }
        logoutButton.graphic = MaterialIconView(MaterialIcon.EXIT_TO_APP).apply { fill = Paint.valueOf(Colors.GRAY_DARK)}
        entriesPane.isCollapsible = false

        val directoryChooser = DirectoryChooser()
        directoryChooser.initialDirectory = File(".")

        directoryChooserButton.setOnAction({ e ->
            val selectedDirectory = directoryChooser.showDialog(Stage())
            println(selectedDirectory.absolutePath)
        })

        /*Platform.runLater {
            println("PRINT")
            val background = logoutButton.getBackground()
            if (background != null) {
                var i = 0
                for (fill in background.getFills()) {
                    val insets: Insets? = fill.getInsets()
                    println("BackgroundFill " + i + ": " + insets)
                    i++
                }
            }
        } */

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
        entriesList.addEventFilter(MOUSE_CLICKED) { click ->
            println("In list filter")
            click.consume()
        }
        entriesList.selectionModel.selectedItemProperty().addListener { _, _, newValue ->
            println("Selected $newValue")
            loadItemView(newValue)
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
        Platform.runLater {
            if(filtered.isEmpty()) {
                loadItemView(null)
            } else {
                entriesList.selectionModel.selectFirst()
            }
            entriesList.requestFocus()
        }
        listviewPlaceholderLabel.textProperty().bind(listViewPlaceholder)
    }
}


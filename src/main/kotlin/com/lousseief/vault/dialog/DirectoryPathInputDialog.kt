package com.lousseief.vault.dialog

import com.lousseief.vault.controller.dialog.DirectoryPathInputDialogController
import javafx.fxml.FXMLLoader
import javafx.scene.control.*

class DirectoryPathInputDialog(
    header: String,
    description: String
): Dialog<String?>() {

    init {
        val loader = FXMLLoader(javaClass.getResource("/DirectoryPathInputDialog.fxml"))
        val controller = DirectoryPathInputDialogController()
        loader.setController(controller)
        val chooseProfilesLocationDialog: DialogPane = loader.load()
        dialogPane = chooseProfilesLocationDialog
        controller.finalize(this, header, description)
    }

}

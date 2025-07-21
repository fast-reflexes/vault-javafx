package com.lousseief.vault.dialog

import com.lousseief.vault.controller.dialog.ChooseProfilesLocationDialogController
import javafx.fxml.FXMLLoader
import javafx.scene.control.*

class ChooseProfilesLocationDialog(isInitialSetup: Boolean): Dialog<String?>() {

    init {
        val loader = FXMLLoader(javaClass.getResource("/ChooseProfilesLocationDialog.fxml"))
        val controller = ChooseProfilesLocationDialogController()
        loader.setController(controller)
        val chooseProfilesLocationDialog: DialogPane = loader.load()
        dialogPane = chooseProfilesLocationDialog
        controller.finalize(this, isInitialSetup)
    }

}

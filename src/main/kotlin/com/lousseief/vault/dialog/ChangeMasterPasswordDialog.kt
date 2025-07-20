package com.lousseief.vault.dialog

import com.lousseief.vault.controller.dialog.ChangeMasterPasswordDialogController
import javafx.fxml.FXMLLoader
import javafx.scene.control.*

class ChangeMasterPasswordDialog(evaluator: (String, String) -> Unit): Dialog<String?>() {

    init {
        val loader = FXMLLoader(javaClass.getResource("/ChangeMasterPasswordDialog.fxml"))
        val controller = ChangeMasterPasswordDialogController(evaluator)
        loader.setController(controller)
        val changeMasterPasswordDialog: DialogPane = loader.load()
        dialogPane = changeMasterPasswordDialog
        controller.finalize(this)
    }

}

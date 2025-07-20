package com.lousseief.vault.dialog

import com.lousseief.vault.controller.dialog.PasswordConfirmDialogController
import javafx.event.ActionEvent
import javafx.fxml.FXMLLoader
import javafx.scene.control.*

class PasswordConfirmDialog(evaluator: (String, ActionEvent) -> Unit): Dialog<String?>() {

    init {
        val loader = FXMLLoader(javaClass.getResource("/PasswordConfirmDialog.fxml"))
        val controller = PasswordConfirmDialogController(evaluator)
        loader.setController(controller)
        val passwordConfirmDialog: DialogPane = loader.load()
        dialogPane = passwordConfirmDialog
        controller.finalize(this)
    }

}

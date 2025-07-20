package com.lousseief.vault.dialog

import com.lousseief.vault.controller.dialog.AddPasswordDialogController
import javafx.event.ActionEvent
import javafx.fxml.FXMLLoader
import javafx.scene.control.*

class AddPasswordDialog(
    defaultPasswordLength: Int,
    evaluator: (String, ActionEvent) -> Unit,
    header: String
): Dialog<String?>() {

    init {
        val loader = FXMLLoader(javaClass.getResource("/AddPasswordDialog.fxml"))
        val controller = AddPasswordDialogController(
            defaultPasswordLength,
            evaluator,
            header
        )
        loader.setController(controller)
        val stringGeneratorDialog: DialogPane = loader.load()
        dialogPane = stringGeneratorDialog
        controller.finalize(this)
    }

}

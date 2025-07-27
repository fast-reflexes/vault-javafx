package com.lousseief.vault.dialog

import com.lousseief.vault.controller.dialog.SingleStringInputDialogController
import javafx.event.ActionEvent
import javafx.fxml.FXMLLoader
import javafx.scene.control.*

class SingleStringInputDialog(
    title: String,
    header: String,
    evaluator: (String?, ActionEvent) -> Unit,
    initialValue: String? = null
): Dialog<String?>() {

    init {
        val loader = FXMLLoader(javaClass.getResource("/SingleStringInputDialog.fxml"))
        val controller = SingleStringInputDialogController(evaluator, initialValue)
        loader.setController(controller)
        val singleStringInputDialog: DialogPane = loader.load()
        dialogPane = singleStringInputDialog
        controller.finalize(this, title, header)
    }

}

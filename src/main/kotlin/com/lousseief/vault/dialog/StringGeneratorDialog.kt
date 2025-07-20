package com.lousseief.vault.dialog

import com.lousseief.vault.controller.dialog.StringGeneratorDialogController
import javafx.fxml.FXMLLoader
import javafx.scene.control.*

class StringGeneratorDialog(defaultPasswordLength: Int): Dialog<String?>() {

    init {
        val loader = FXMLLoader(javaClass.getResource("/StringGeneratorDialog.fxml"))
        val controller = StringGeneratorDialogController(defaultPasswordLength)
        loader.setController(controller)
        val stringGeneratorDialog: DialogPane = loader.load()
        dialogPane = stringGeneratorDialog
        controller.finalize(this)
    }

}

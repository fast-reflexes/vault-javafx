package com.lousseief.vault.dialog

import com.lousseief.vault.controller.dialog.SettingsDialogController
import com.lousseief.vault.model.UiProfile
import javafx.fxml.FXMLLoader
import javafx.scene.control.*

class SettingsDialog(user: UiProfile, evaluator: (String, String) -> Unit): Dialog<Unit>() {

    init {
        val loader = FXMLLoader(javaClass.getResource("/SettingsDialog.fxml"))
        val controller = SettingsDialogController(user, evaluator)
        loader.setController(controller)
        val settingsDialog: DialogPane = loader.load()
        dialogPane = settingsDialog
        controller.finalize(this)
    }

}

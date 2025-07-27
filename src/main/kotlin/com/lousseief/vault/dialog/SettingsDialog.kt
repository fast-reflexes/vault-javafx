package com.lousseief.vault.dialog

import com.lousseief.vault.controller.dialog.SettingsDialogController
import com.lousseief.vault.model.ui.UiProfile
import javafx.fxml.FXMLLoader
import javafx.scene.control.*

/**
 * In this dialog we may change the settings of a profile, implying 3 properties:
 *  * default password length
 *  * time when the vault is open after having entered the password
 *  * manage categories
 */
class SettingsDialog(user: UiProfile): Dialog<Int>() {

    init {
        val loader = FXMLLoader(javaClass.getResource("/SettingsDialog.fxml"))
        val controller = SettingsDialogController(user)
        loader.setController(controller)
        val settingsDialog: DialogPane = loader.load()
        dialogPane = settingsDialog
        controller.finalize(this)
    }

}

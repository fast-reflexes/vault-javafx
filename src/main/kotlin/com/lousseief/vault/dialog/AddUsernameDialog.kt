package com.lousseief.vault.dialog

import com.lousseief.vault.controller.dialog.AddUsernameDialogController
import com.lousseief.vault.model.ui.UiProfile
import com.lousseief.vault.model.ui.UiCredential
import javafx.event.ActionEvent
import javafx.fxml.FXMLLoader
import javafx.scene.control.*

class AddUsernameDialog(
    user: UiProfile,
    credential: UiCredential,
    evaluator: (String?, ActionEvent) -> Unit
): Dialog<String?>() {

    init {
        val loader = FXMLLoader(javaClass.getResource("/AddUsernameDialog.fxml"))
        val controller = AddUsernameDialogController(user, credential, evaluator)
        loader.setController(controller)
        val addUsernameDialog: DialogPane = loader.load()
        dialogPane = addUsernameDialog
        controller.finalize(this)
    }

}

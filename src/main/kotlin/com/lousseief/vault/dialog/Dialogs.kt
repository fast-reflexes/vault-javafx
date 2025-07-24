package com.lousseief.vault.dialog

import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.ButtonBar
import javafx.scene.control.ButtonType

class Dialogs {

    companion object {
        fun openConfirmSensitiveOperationDialog(
            okButtonText: String,
            cancelButtonText: String?,
            header: String,
            content: String,
        ): Boolean {
            val ok = ButtonType(okButtonText, ButtonBar.ButtonData.OK_DONE)
            val cancel = ButtonType(cancelButtonText ?: "Cancel", ButtonBar.ButtonData.CANCEL_CLOSE)
            val confirm = Alert(
                Alert.AlertType.WARNING,
                content,
                cancel, ok
            ).apply {
                (dialogPane.lookupButton(ok) as Button).isDefaultButton = false
                (dialogPane.lookupButton(cancel) as Button).isDefaultButton = true
                title = "Are you sure?"
                headerText = header
            }.showAndWait()
            return confirm.isPresent && confirm.get() == ok
        }
    }
}

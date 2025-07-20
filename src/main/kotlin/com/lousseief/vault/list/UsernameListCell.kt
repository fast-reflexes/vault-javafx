package com.lousseief.vault.list

import com.lousseief.vault.model.ui.UiCredential
import javafx.collections.ObservableList
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.util.Callback

class UsernameListCell(val credential: UiCredential, val usernameList: ObservableList<String>) : ListCell<String?>() {

    override fun updateItem(item: String?, empty: Boolean) {
        super.updateItem(item, empty)
        if (item == "" || item in credential.identities.value) {
            if(item == "") {
                if (usernameList.size == 1)
                    text = "No previous user names found"
                else
                    text = "Select a user name"
            } else {
                text = "$item (already in use)"
            }
            isDisable = true
            style = "-fx-opacity: 0.4"
        } else {
            text = item
            isDisable = false
            style = "-fx-opacity: 1.0"
        }
    }
}

class UsernameListCellFactory(val credential: UiCredential, val usernameList: ObservableList<String>) : Callback<ListView<String?>, ListCell<String?>> {

    override
    fun call(arg0: ListView<String?>): ListCell<String?> =
        UsernameListCell(credential, usernameList)

}

package com.lousseief.vault.list

import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.util.Callback

class EntryCategoryButtonCell : ListCell<String?>() {

    override
    fun updateItem(item: String?, empty: Boolean) {
        super.updateItem(item, empty)
        if (item === "") {
            text = "Select category"
            isDisable = true
            style = "-fx-opacity: 0.4"
        } else {
            text = item
            isDisable = false
            style = "-fx-opacity: 1.0"
        }
    }
}

class EntryCategoryListCell : ListCell<String?>() {

    override
    fun updateItem(item: String?, empty: Boolean) {
        super.updateItem(item, empty)
        if (item === "") {
            text = "(no category)"
            isDisable = false
            style = "-fx-opacity: 1.0"
        } else {
            text = item
            isDisable = false
            style = "-fx-opacity: 1.0"
        }
    }
}

class EntryCategoryListCellFactory : Callback<ListView<String?>, ListCell<String?>> {

    override
    fun call(arg0: ListView<String?>): ListCell<String?> =
        EntryCategoryListCell()
}

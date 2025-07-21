package com.lousseief.vault.list

import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.util.Callback

class BooleanListButtonCell : ListCell<String?>() {

    override
    fun updateItem(item: String?, empty: Boolean) {
        super.updateItem(item, empty)
        if (item === "" || empty) {
            text = "Select value"
            isDisable = true
            style = "-fx-opacity: 0.4"
        } else {
            text = item
            isDisable = false
            style = "-fx-opacity: 1.0"
        }
    }
}

class BooleanListCell : ListCell<String?>() {

    override
    fun updateItem(item: String?, empty: Boolean) {
        super.updateItem(item, empty)
        if (item === "" || empty) {
            text = "Select value"
            isDisable = false
            style = "-fx-opacity: 0.4"
        } else {
            text = item
            isDisable = false
            style = "-fx-opacity: 1.0"
        }
    }
}

class BooleanListCellFactory : Callback<ListView<String?>, ListCell<String?>> {

    override
    fun call(arg0: ListView<String?>): ListCell<String?> =
        BooleanListCell()
}

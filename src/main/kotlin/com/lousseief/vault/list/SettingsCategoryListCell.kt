package com.lousseief.vault.list

import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.util.Callback

class SettingsCategoryListCell(val usedCategories: Map<String, Int>) : ListCell<String?>() {

    override
    fun updateItem(item: String?, empty: Boolean) {
        super.updateItem(item, empty)
        if (item !== null && item in usedCategories) {
            text = "$item (used for ${usedCategories[item]} associations)"
            isDisable = true
            style = "-fx-opacity: 0.7"
        } else {
            text = item
            isDisable = false
            style = "-fx-opacity: 1.0"
        }
    }
}

class SettingsCategoryListCellFactory(val usedCategories: Map<String, Int>) : Callback<ListView<String?>, ListCell<String?>> {

    override
    fun call(arg0: ListView<String?>): ListCell<String?> =
        SettingsCategoryListCell(usedCategories)
}

package com.lousseief.vault.list

import com.lousseief.vault.model.ui.UiAssociation
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.layout.StackPane
import javafx.util.Callback

class EntryListCell : ListCell<UiAssociation>() {

    private val label = Label()
    private val pane: StackPane

    init {
        pane = StackPane()
        pane.minWidth = 0.0
        pane.prefWidth = 1.0
        label.maxWidth = Double.Companion.MAX_VALUE
        label.ellipsisString = "..."
        pane.children.add(label)
    }

    override fun updateItem(item: UiAssociation?, empty: Boolean) {
        super.updateItem(item, empty)
        if (empty) {
            graphic = null
            isDisable = true
        } else {
            if(item !== null) {
                label.text = if (item.mainIdentifier.value === "") "(unnamed entry)" else item.mainIdentifier.value
                graphic = pane;
                isDisable = false
                style = "-fx-opacity: 1.0"
            }
        }
    }

}

class EntryListCellFactory(val lv: ListView<UiAssociation>) : Callback<ListView<UiAssociation>, ListCell<UiAssociation>> {

    override fun call(arg0: ListView<UiAssociation>): ListCell<UiAssociation> =
        EntryListCell()
}

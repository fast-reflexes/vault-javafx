package com.lousseief.vault.model.ui

import com.lousseief.vault.model.Association
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections

data class UiAssociation(
    var mainIdentifier: SimpleStringProperty,
    var secondaryIdentifiers: SimpleListProperty<String>  = SimpleListProperty<String>(),
    var isNeeded: SimpleBooleanProperty = SimpleBooleanProperty(true),
    var shouldBeDeactivated: SimpleBooleanProperty = SimpleBooleanProperty(false),
    var isDeactivated: SimpleBooleanProperty = SimpleBooleanProperty(false),
    var category: SimpleStringProperty = SimpleStringProperty(null),
    var comment: SimpleStringProperty = SimpleStringProperty(null)
) {

    companion object {

        fun fromAssociation(association: Association): UiAssociation {
            return UiAssociation(
                mainIdentifier = SimpleStringProperty(association.mainIdentifier),
                secondaryIdentifiers = SimpleListProperty(FXCollections.observableArrayList(association.secondaryIdentifiers)),
                isNeeded = SimpleBooleanProperty(association.isNeeded),
                shouldBeDeactivated = SimpleBooleanProperty(association.isDeactivated),
                isDeactivated = SimpleBooleanProperty(association.isDeactivated),
                category = SimpleStringProperty(association.category),
                comment = SimpleStringProperty(association.comment)
            )
        }

    }

    fun toAssociation(): Association {
        return Association(
            mainIdentifier = mainIdentifier.value,
            secondaryIdentifiers = secondaryIdentifiers.value,
            isNeeded = isNeeded.value,
            shouldBeDeactivated = shouldBeDeactivated.value,
            isDeactivated = isDeactivated.value,
            category = category.value,
            comment = comment.value,
        )
    }

    fun containsChange(originalAssociation: Association?): Boolean {
        if(originalAssociation == null) {
            return true // new association that didn't exist before
        }
        val aP = originalAssociation
        return aP.mainIdentifier != mainIdentifier.value
            || aP.category != category.value
            || aP.comment != comment.value
            || aP.secondaryIdentifiers.size != secondaryIdentifiers.size
            || aP.secondaryIdentifiers.zip(secondaryIdentifiers).any { (si1, si2) -> si1 != si2 }
            || aP.isNeeded != isNeeded.value
            || aP.isDeactivated != isDeactivated.value
            || aP.shouldBeDeactivated != shouldBeDeactivated.value
    }
}

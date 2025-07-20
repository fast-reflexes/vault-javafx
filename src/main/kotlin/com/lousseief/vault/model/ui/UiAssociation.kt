package com.lousseief.vault.model.ui

import com.lousseief.vault.model.Association
import com.lousseief.vault.model.Credential
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections

data class UiAssociation(
    val savedAssociation: Association,
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
                savedAssociation = association,
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
}

data class UiAssociationWithCredentials(
    val association: UiAssociation,
    var credentials: List<Credential> = emptyList()
)

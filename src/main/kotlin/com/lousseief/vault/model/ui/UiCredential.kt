package com.lousseief.vault.model.ui

import com.lousseief.vault.model.Credential
import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import java.time.Instant

data class UiCredential(
    val identities: SimpleListProperty<String>,
    val password: SimpleStringProperty,
    val created: SimpleObjectProperty<Instant>,
    val lastUpdated: SimpleObjectProperty<Instant>
) {

    companion object {

        fun fromCredentials(credentials: Credential): UiCredential {
            return UiCredential(
                identities = SimpleListProperty(FXCollections.observableArrayList(credentials.identities)),
                password = SimpleStringProperty(credentials.password),
                created = SimpleObjectProperty(credentials.created),
                lastUpdated = SimpleObjectProperty(credentials.lastUpdated)
            )
        }

    }

    fun toCredential(): Credential {
        return Credential(
            identities = this.identities.value,
            password = this.password.value,
            created = this.created.value,
            lastUpdated = this.lastUpdated.value
        )
    }

}

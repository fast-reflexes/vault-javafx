package com.lousseief.vault.model.ui

import com.lousseief.vault.model.Settings
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleListProperty
import javafx.collections.FXCollections

data class UiSettings(
    var passwordLength: SimpleIntegerProperty = SimpleIntegerProperty(20),
    var categories: SimpleListProperty<String> = SimpleListProperty<String>(),
    var savePasswordForMinutes: SimpleIntegerProperty = SimpleIntegerProperty(2)
    //var blocksEncoded: Int = 0
) {

    companion object {
        fun fromSettings(settings: Settings): UiSettings {
            return UiSettings(
                SimpleIntegerProperty(settings.passwordLength),
                SimpleListProperty(FXCollections.observableArrayList(settings.categories)),
                SimpleIntegerProperty(settings.savePasswordForMinutes)
            )
        }
    }

    fun toSettings(): Settings {
        return Settings(
            passwordLength = passwordLength.value,
            categories = categories.value,
            savePasswordForMinutes = savePasswordForMinutes.value,
        )
    }

}

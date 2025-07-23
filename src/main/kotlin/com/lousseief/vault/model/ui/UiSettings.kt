package com.lousseief.vault.model.ui

import com.lousseief.vault.model.Settings
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleListProperty
import javafx.collections.FXCollections

data class UiSettings(
    val savedSettings: Settings,
    var passwordLength: SimpleIntegerProperty = SimpleIntegerProperty(20),
    var categories: SimpleListProperty<String> = SimpleListProperty<String>(),
    var savePasswordForMinutes: SimpleIntegerProperty = SimpleIntegerProperty(2)
    //var blocksEncoded: Int = 0
) {

    companion object {
        fun fromSettings(settings: Settings): UiSettings {
            return UiSettings(
                savedSettings = settings,
                SimpleIntegerProperty(settings.passwordLength),
                SimpleListProperty(FXCollections.observableArrayList(settings.categories)),
                SimpleIntegerProperty(settings.savePasswordForMinutes)
            )
        }
    }

    val isDirty = Bindings.createBooleanBinding(
        { containsChange() },
        passwordLength, savePasswordForMinutes, categories
    )

    fun toSettings(): Settings {
        return Settings(
            passwordLength = passwordLength.value,
            categories = categories.value,
            savePasswordForMinutes = savePasswordForMinutes.value,
        )
    }

    private fun containsChange(): Boolean {
        val sS = savedSettings
        return (
            sS.passwordLength != passwordLength.value
            || sS.savePasswordForMinutes != savePasswordForMinutes.value
            || sS.categories.size != categories.size
            || sS.categories.zip(categories).any { (c1, c2) -> c1 != c2 }
        )
    }

}

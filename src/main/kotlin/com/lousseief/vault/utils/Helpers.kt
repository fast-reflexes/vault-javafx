package com.lousseief.vault.utils

import com.lousseief.vault.model.ui.UiAssociation
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.ObservableList
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.control.Spinner
import javafx.scene.control.SpinnerValueFactory
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.text.TextAlignment
import javafx.stage.Stage
import javafx.util.StringConverter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

fun copySelectionToClipboard(string: String) =
    ClipboardContent()
        .apply {
            putString(string)
            Clipboard.getSystemClipboard().setContent(this)
        }

fun initializeSpinner(property: SimpleIntegerProperty, spinner: Spinner<Int>, max: Int, min: Int) {
    val factory = spinner.valueFactory as SpinnerValueFactory.IntegerSpinnerValueFactory
    factory.max = max
    factory.min = min
    /* must set factory value BEFORE binding because otherwise the spinner will overwrite the property with its initial
    factory value */
    factory.value = property.value
    spinner.isEditable = true
    property.bind(spinner.valueProperty())
    spinner.getValueFactory().converter = object : StringConverter<Int?>() {

        override fun toString(value: Int?): String =
            value?.toString() ?: "0"

        override fun fromString(value: String?): Int =
            (value
                ?.let {
                    try {
                        Integer.parseInt(value.trim())
                    } catch(e: Exception) { null }
                }
                ?.let {
                    when {
                        it > max -> max
                        it < min -> min
                        else -> it
                    }
                }
                ?: property.value)
                .also {
                    spinner.editor.text = toString(it)
                }

    }
}

fun timeToStringDate(time: Instant?): String =
    time
        ?.atZone(ZoneId.systemDefault())
        ?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT))
        ?: "-"

fun timeToStringDateTime(time: Instant?): String =
    time
        ?.atZone(ZoneId.systemDefault())
        ?.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        ?: "-"

fun setupStage(stage: Stage, fixedMax: Boolean = false) {
    stage.minWidth = 0.0
    stage.minHeight = 0.0
    stage.maxWidth = Double.MAX_VALUE
    stage.maxHeight = Double.MAX_VALUE
    stage.sizeToScene()
    stage.centerOnScreen()
    stage.minWidth = stage.width
    stage.minHeight = stage.height
    if(fixedMax) {
        stage.maxWidth = stage.width
        stage.maxHeight = stage.height
    } else {
        stage.maxWidth = Double.MAX_VALUE
        stage.maxHeight = Double.MAX_VALUE
    }
}

fun setupErrorMessageHandling(
    errorProperty: SimpleStringProperty,
    allowedMaxWidth: Double,
    container: VBox,
    defaultNumberOfChildren: Int
) {
    errorProperty.addListener { _, _, newValue ->
        if(newValue.isNullOrEmpty()) {
            if(container.children.size > defaultNumberOfChildren) {
                (defaultNumberOfChildren..container.children.size - 1).forEach { container.children.removeAt(it) }
            }
        } else {
            if(container.children.size == defaultNumberOfChildren) {
                container.children.add(
                    Label(errorProperty.value).apply {
                        HBox.setHgrow(this, Priority.ALWAYS)
                        VBox.setVgrow(this, Priority.ALWAYS)
                        maxHeight = Double.MAX_VALUE
                        textAlignment = TextAlignment.RIGHT
                        style="-fx-text-fill: red"
                        alignment = Pos.CENTER_RIGHT
                        isWrapText = true
                        maxWidth = allowedMaxWidth
                    }
                )
            }
        }
    }

}

fun ObservableList<UiAssociation>.sortInPlaceByMainIdentifier() {
    sortWith { a, b -> a.mainIdentifier.value.compareTo(b.mainIdentifier.value) }
}

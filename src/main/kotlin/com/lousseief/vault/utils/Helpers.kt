package com.lousseief.vault.utils

import com.lousseief.vault.model.ui.UiAssociation
import javafx.application.Platform
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.ObservableList
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.control.Spinner
import javafx.scene.control.SpinnerValueFactory
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import javafx.scene.input.DataFormat
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
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.schedule

/** How long a copied secret is allowed to stay on the system clipboard. */
const val CLIPBOARD_CLEAR_SECONDS = 30L

/* Hints that ask clipboard managers and history features not to persist the entry. DataFormat
throws if the same mime type is registered twice, so look it up before constructing it. */
private fun clipboardHint(mimeType: String): DataFormat =
    DataFormat.lookupMimeType(mimeType) ?: DataFormat(mimeType)

private val CONCEALED_HINTS: List<DataFormat> by lazy {
    listOf(
        // macOS convention, honoured by most third party clipboard managers
        clipboardHint("org.nspasteboard.ConcealedType"),
        // Windows clipboard history / cloud clipboard
        clipboardHint("ExcludeClipboardContentFromMonitorProcessing"),
        clipboardHint("CanIncludeInClipboardHistory"),
        clipboardHint("CanUploadToCloudClipboard")
    )
}

private var clipboardClearTask: TimerTask? = null

/**
 * Copies a secret (password or part of one) to the system clipboard, marks it so clipboard managers
 * skip it, and clears it again after [CLIPBOARD_CLEAR_SECONDS].
 *
 * The clipboard is only cleared if it still holds the value we put there, so anything the user
 * copied in the meantime is left alone.
 */
fun copySecretToClipboard(secret: String) {
    val clipboard = Clipboard.getSystemClipboard()
    ClipboardContent()
        .apply {
            putString(secret)
            CONCEALED_HINTS.forEach { put(it, "") }
            clipboard.setContent(this)
        }

    clipboardClearTask?.cancel()
    // daemon timer, so a pending clear never keeps the application alive
    clipboardClearTask = Timer(true)
        .schedule(CLIPBOARD_CLEAR_SECONDS * 1000L) {
            Platform.runLater {
                val current = Clipboard.getSystemClipboard()
                if (current.hasString() && current.string == secret) {
                    current.clear()
                }
            }
        }
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
    stage.isResizable = true
    stage.minWidth = 0.0
    stage.minHeight = 0.0
    stage.maxWidth = Double.MAX_VALUE
    stage.maxHeight = Double.MAX_VALUE
    stage.sizeToScene()
    stage.centerOnScreen()
    Platform.runLater {
        stage.minWidth = stage.width
        stage.minHeight = stage.height
        stage.isResizable = !fixedMax
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
    sortWith { a, b -> a.mainIdentifier.value.compareTo(b.mainIdentifier.value, ignoreCase = true) }
}

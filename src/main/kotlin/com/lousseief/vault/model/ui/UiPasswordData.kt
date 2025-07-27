package com.lousseief.vault.model.ui

import com.lousseief.vault.dialog.PasswordConfirmDialog
import com.lousseief.vault.service.VerificationService
import javafx.application.Platform
import javafx.beans.property.SimpleObjectProperty
import javafx.event.ActionEvent
import java.time.Instant
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.schedule

data class UiPasswordData(
    private var savedPasswordResetter: TimerTask? = null,
    var savedPasswordExpiry: SimpleObjectProperty<Instant?> = SimpleObjectProperty(null),
    private var savedMasterPassword: String? = null
) {

    fun cancelSavedMasterPassword() {
        savedMasterPassword = null
        savedPasswordExpiry.set(null)
        savedPasswordResetter?.cancel()
        savedPasswordResetter = null
    }

    fun resetSavedMasterPassword(nextPassword: String, passwordDedupingTimeMinutes: Int) {
        synchronized(this) {
            savedMasterPassword = nextPassword
            val expirationMillis = passwordDedupingTimeMinutes * 60 * 1000L
            val expirationTime = Instant.now().plusMillis(expirationMillis)
            savedPasswordExpiry.set(expirationTime)
            savedPasswordResetter?.cancel()
            savedPasswordResetter = Timer(false)
                .schedule(expirationMillis) {
                    Platform.runLater {
                        println("reset!")
                        savedMasterPassword = null
                        savedPasswordExpiry.set(null)
                    }
                }
            if(passwordDedupingTimeMinutes == 0) {
                cancelSavedMasterPassword()
            }
        }
    }

    fun passwordRequiredAction(user: UiProfile, passwordDedupingTimeMinutes: Int, requireFreshPassword: Boolean = false): String? {
        if(!requireFreshPassword && savedMasterPassword !== null && savedPasswordExpiry.value !== null && Instant.now().isBefore(savedPasswordExpiry.value)) {
            // NOT necessarily a good thing to update / prolong this timeout every time
            resetSavedMasterPassword(savedMasterPassword!!, passwordDedupingTimeMinutes)
            return savedMasterPassword
        }
        else {
            savedMasterPassword = null
            val result = PasswordConfirmDialog { password: String, event: ActionEvent ->
                // below will throw if password is wrong
                VerificationService.authorize(password, user.keyMaterialSalt, user.verificationHash, user.verificationSalt)
                resetSavedMasterPassword(password, passwordDedupingTimeMinutes)
            }.showAndWait()
            if(result.isPresent) {
                return result.get()
            } else {
                return null
            }
        }
    }
}

package com.lousseief.vault.model.ui

import com.lousseief.vault.dialog.PasswordConfirmDialog
import com.lousseief.vault.service.VerificationService
import javafx.application.Platform
import javafx.beans.property.SimpleObjectProperty
import javafx.event.ActionEvent
import java.time.Duration
import java.time.Instant
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.schedule

data class UiPasswordData(
    private var savedPasswordResetter: TimerTask? = null,
    var savedPasswordExpiry: SimpleObjectProperty<Instant?> = SimpleObjectProperty(null),
    private var savedMasterPassword: String? = null,
    private var savedPasswordHardExpiry: Instant? = null
) {

    companion object {
        /* using the cached password prolongs its soft expiry, but never past this many minutes after
        it was actually typed - the hard cap on how long the password may stay in memory */
        const val MAX_SAVED_PASSWORD_MINUTES = 10L
    }

    fun cancelSavedMasterPassword() {
        savedMasterPassword = null
        savedPasswordExpiry.set(null)
        savedPasswordHardExpiry = null
        savedPasswordResetter?.cancel()
        savedPasswordResetter = null
    }

    fun resetSavedMasterPassword(nextPassword: String, passwordDedupingTimeMinutes: Int, isFreshEntry: Boolean = false) {
        synchronized(this) {
            savedMasterPassword = nextPassword
            /* the soft expiry slides forward on every use, the hard expiry is fixed when the password
            is typed - the effective expiry is whichever comes first */
            if (isFreshEntry || savedPasswordHardExpiry === null) {
                savedPasswordHardExpiry = Instant.now().plusSeconds(MAX_SAVED_PASSWORD_MINUTES * 60)
            }
            val softExpiry = Instant.now().plusSeconds(passwordDedupingTimeMinutes * 60L)
            val expirationTime = minOf(softExpiry, savedPasswordHardExpiry!!)
            savedPasswordExpiry.set(expirationTime)
            savedPasswordResetter?.cancel()
            // if not daemon thread, then the application will not stop until this timer has fired
            savedPasswordResetter = Timer(true)
                .schedule(Duration.between(Instant.now(), expirationTime).toMillis().coerceAtLeast(0)) {
                    Platform.runLater {
                        cancelSavedMasterPassword()
                    }
                }
            if(passwordDedupingTimeMinutes == 0) {
                cancelSavedMasterPassword()
            }
        }
    }

    fun passwordRequiredAction(user: UiProfile, passwordDedupingTimeMinutes: Int, requireFreshPassword: Boolean = false): String? {
        if(!requireFreshPassword && savedMasterPassword !== null && savedPasswordExpiry.value !== null && Instant.now().isBefore(savedPasswordExpiry.value)) {
            // prolong the soft expiry on use - the hard expiry above caps the total retention
            resetSavedMasterPassword(savedMasterPassword!!, passwordDedupingTimeMinutes)
            return savedMasterPassword
        }
        else {
            val result = PasswordConfirmDialog { password: String, event: ActionEvent ->
                // below will throw if password is wrong; the derived key is not needed here, so scrub it
                VerificationService.authorize(password, user.keyMaterialSalt, user.verificationHash, user.verificationSalt)
                    .fill(0)
                resetSavedMasterPassword(password, passwordDedupingTimeMinutes, isFreshEntry = true)
            }.showAndWait()
            if(result.isPresent) {
                return result.get()
            } else {
                return null
            }
        }
    }
}

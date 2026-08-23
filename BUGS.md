# Known bugs and platform quirks

## macOS: closing a `showAndWait()` window takes two clicks after interacting with its content

### Symptom

On macOS, any window shown with `showAndWait()` — which includes every `Dialog`, `Alert` and
the credentials modal — needs TWO clicks on the red title-bar close button once the user has
interacted with the window content (clicked a checkbox/button, selected text, ...). The first
click is silently swallowed: no `WINDOW_CLOSE_REQUEST` reaches JavaFX, no beep, no exception.
Opening a window and closing it immediately (no content interaction) works with one click.

### Root cause

The nested event loop that `showAndWait()` starts (and nothing else — ownership and modality
are irrelevant). Proven with a minimal standalone app, `bugs/macos-showandwait-close-click/CloseClickRepro.java`, which
shows the same window three ways: `showAndWait()` (bug), `show()` (no bug), and
`show()` + `Platform.enterNestedEventLoop()` (bug — the isolated cause). Windows is unaffected.
Observed on macOS 26 (Tahoe) with JavaFX 24.0.2, 25.0.4 and 26.0.2; reproduced both in this app
and in the clean-room repro, so it is a JavaFX/macOS bug, not something in this codebase.

Reported to Oracle 2026-08-23 via bugreport.java.com, internal review ID
`18ecb8f5-8a24-45a6-8bb0-b7e861f8bebc` (report text: `bugs/macos-showandwait-close-click/javafx-bug-report.md`).
Check bugs.openjdk.org for the synopsis "[macos] First click on title-bar close button ignored
in nested event loop after content interaction" to see if it has received a public JDK number
or a fix.

### Current decision

Accepted. The quirk is cosmetic (dialogs are normally closed via their buttons), and avoiding
it costs code complexity. Both workarounds below were implemented on 2026-08-22/23 and then
deliberately reverted to keep the code simple.

**Testing status at the time of the revert — important honesty note:** Workaround A and the
coroutine pattern were runtime-verified only for the credentials modal, the string generator
dialog and the settings dialog flow (one-click close confirmed on macOS). The subsequent
app-wide conversion (login, register, startup flow, add/delete association, export, save,
change master password, close-request handlers, etc.) compiled and passed the unit tests but
was reverted BEFORE being runtime-tested. When reimplementing, treat those flows as untested:
click through each one, especially the delicate conversions listed below.

### Workaround A: `show()` instead of `showAndWait()` (only when no result is needed)

`showAndWait()` blocks the *calling code*, not the user — user-blocking comes from modality,
which is unaffected. So any call site with no code after `showAndWait()` can simply use
`show()` and the bug disappears for that window. This applied to exactly one place: the
credentials modal in `AssociationController.setupCredentialsButton`, where
`stage.showAndWait()` was replaced by `stage.show()` with no other changes. Not applicable to
dialogs whose result is read on the next line.

### Workaround B: coroutines — full replacement for `showAndWait()` (implemented app-wide; see testing status above)

Keeps the sequential coding style but suspends a coroutine instead of nesting event loops.
One-click close everywhere. Requires:

**1. Dependency** in `build.gradle.kts`:

```kotlin
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-javafx:1.10.2")
```

**2. Utility file** `src/main/kotlin/com/lousseief/vault/utils/FxCoroutines.kt`:

```kotlin
package com.lousseief.vault.utils

import javafx.application.Platform
import javafx.scene.control.Dialog
import javafx.stage.Stage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Optional
import kotlin.coroutines.resume

private val fxScope = CoroutineScope(SupervisorJob() + Dispatchers.JavaFx)

/** Launches a coroutine on the JavaFX application thread. */
fun launchFx(block: suspend CoroutineScope.() -> Unit) {
    fxScope.launch(block = block)
}

/** Drop-in showAndWait() replacement: shows the dialog, suspends until hidden, returns the result. */
suspend fun <T> Dialog<T>.await(): Optional<T & Any> = suspendCancellableCoroutine { cont ->
    setOnHidden { cont.resume(Optional.ofNullable(result)) }
    cont.invokeOnCancellation { Platform.runLater { hide() } }
    show()
}

/** showAndWait() replacement for a plain Stage: suspends until the stage is hidden. */
suspend fun Stage.awaitClose(): Unit = suspendCancellableCoroutine { cont ->
    setOnHidden { cont.resume(Unit) }
    cont.invokeOnCancellation { Platform.runLater { hide() } }
    show()
}
```

**3. Conversion rules** (mechanical, applied per call site):

- Handler that shows a dialog: wrap the body in `launchFx { ... }` and change
  `.showAndWait()` to `.await()`. The body is otherwise unchanged (`await()` returns the same
  `Optional`).

  ```kotlin
  button.setOnAction {
      launchFx {
          val result = SomeDialog(...).await()
          if (result.isPresent) { ... }
      }
  }
  ```

- Function that shows a dialog internally AND returns a value (e.g.
  `UiProfile.passwordRequiredAction(): String?`, `UiProfile.save()`,
  `Dialogs.openConfirmSensitiveOperationDialog(): Boolean`): mark it `suspend fun`. Every
  caller must then be inside `launchFx` (or itself suspend). This "ripple" reached six
  functions when done for real; the fire-and-forget boundary (where `launchFx` sits) is always
  the event handler / callback.

- Callbacks crossing controllers (plain `() -> Unit` like `onDeleteCredential`): wrap at the
  callback construction site: `{ launchFx { onDeleteCredential(i) } }`.

- `setOnCloseRequest` handlers that ask "unsaved changes?": these must decide synchronously
  whether to consume the event, so they cannot await inline. Pattern: consume the event
  unconditionally (or in the branch that needs the dialog), confirm asynchronously, close
  programmatically:

  ```kotlin
  window.setOnCloseRequest {
      it.consume()
      launchFx {
          if (Dialogs.openConfirmSensitiveOperationDialog(...)) {
              window.hide()
          }
      }
  }
  ```

- The credentials `Stage` uses `stage.awaitClose()` in place of `stage.showAndWait()`.

**4. Delicate conversions — the spots that are NOT mechanical**

- `MainController` logout button: `onTerminateSession(false)` returns a Boolean that decides
  whether to call `router.showLogin()` — both the call and the branch go inside `launchFx`.
- `MainController` main-window close request: `onTerminateSession(true)` internally detaches
  the close handler (`window.onCloseRequest = null`) when the user confirms; with the
  consume-then-`hide()` pattern this still works because `hide()` bypasses close requests, but
  keep that detach in mind when restructuring.
- `Router.start()` first-run setup flow: wrapping it in `launchFx` means `start()` returns
  before the directory dialog is answered; the login window is then already visible behind the
  application-modal dialog. Verified as acceptable, but it IS a behavior change at startup.
- `LoginController`/`RegisterController`: `attemptLogin()`/`registerUser()` are invoked from
  multiple places (button action + several ENTER key filters) — every call site needs the
  `launchFx` wrap, not just the button.
- Export flow (`MainController`): three dialogs chained (password prompt → directory dialog →
  result alert) inside one handler — all live in ONE `launchFx` block; do not nest.
- `UiProfile.save()` calls `persistUpdated...()` which calls `passwordRequiredAction(true)` —
  all three become `suspend`, and `save()`'s caller (save button) wraps in `launchFx`.

**5. Gotchas learned while doing it**

- `await()`/`awaitClose()` claim the window's `onHidden` handler for the duration of the call.
- Code placed AFTER a `launchFx { }` block runs before the dialog is answered — everything
  depending on the result belongs inside the block.
- The `Optional<T & Any>` return type is needed so `await()` also works on dialogs typed like
  `Dialog<String?>`.
- `SettingsDialogController` opens a dialog from within a dialog — works fine, same pattern.
- Semantics preserved deliberately: `SettingsDialog` flow calls `result.get()` unconditionally
  (throws if closed empty), exactly like `showAndWait().get()` did.

### How it works (one paragraph)

`showAndWait()` pauses the calling code by freezing its stack frame and starting a second event
loop inside the normal one so the UI stays responsive — that inner loop is what macOS
mishandles. A coroutine pauses by packing the rest of the function and its locals into an
object, returning, and resuming from that object when the dialog's `onHidden` fires — the one
normal event loop keeps running, so there is nothing for macOS to trip over. Same sequential
code, different waiting machinery. (Analogous to JavaScript async/await; `launchFx` plays the
role of calling an async function without awaiting it.)

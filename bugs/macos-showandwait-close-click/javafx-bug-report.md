# Draft bug report for bugreport.java.com

Product: JavaFX · Subcategory: window-toolkit (glass) · OS: macOS

## Synopsis

[macos] First click on title-bar close button ignored in nested event loop after content interaction

## System / version information

- macOS 26.4.1 (Tahoe), build 25E253, Apple Silicon (aarch64)
- Reproduced with JavaFX 24.0.2, 25.0.4 and 26.0.2 (Maven artifacts, mac-aarch64)
- JDK: Amazon Corretto 25.0.4 (also seen on Corretto 23)
- Does NOT reproduce on Windows (same JavaFX versions)

## Description

When a Stage is shown with showAndWait(), the following sequence makes macOS
silently swallow a click on the red title-bar close button:

1. Show a Stage with showAndWait() (modality/ownership do not matter).
2. Click any control inside the window's content (a CheckBox, a Button, or
   simply select text — any mouse interaction with the content).
3. Click the red title-bar close button.

The first click on the close button does nothing: no WINDOW_CLOSE_REQUEST is
fired (verified with an event filter), no system beep, no exception. A second
click closes the window normally. Without step 2 (no content interaction), the
first click closes the window as expected.

The nested event loop is the cause, not showAndWait() itself: showing the same
Stage with show() followed by Platform.enterNestedEventLoop(key) (with
exitNestedEventLoop in onHidden) reproduces the issue identically, while
show() alone does not reproduce it. Modality (NONE / WINDOW_MODAL /
APPLICATION_MODAL) and initOwner have no effect on the outcome.

Because javafx.scene.control.Dialog uses showAndWait()/nested event loops,
every Dialog and Alert is affected the same way: interact with the dialog
content, and its title-bar close button needs two clicks.

## Steps to reproduce

Run the attached CloseClickRepro.java (single file, no dependencies beyond
JavaFX):

    java --module-path <javafx-mods> --add-modules javafx.controls CloseClickRepro.java

1. Click "1: showAndWait()" to open the modal window.
2. Click the checkbox inside it once.
3. Click the red title-bar close button once.

## Expected vs actual

- Expected: the window closes on the first click of the close button (a
  WINDOW_CLOSE_REQUEST is fired), as it does when the window is shown with
  show(), and as it does on Windows.
- Actual: the first click is silently ignored (no WINDOW_CLOSE_REQUEST, no
  beep); a second click closes the window. The attached repro prints
  "CLOSE_REQUEST reached JavaFX" for every click that reaches the toolkit,
  making the swallowed click visible.

Buttons 2 and 3 in the repro demonstrate the control case (show(), closes on
first click) and the isolated cause (show() + Platform.enterNestedEventLoop,
fails like showAndWait).

## Frequency

Always (100% reproducible with the steps above).

---

Attach: CloseClickRepro.java

# Vault (JavaFX)

A desktop password manager written in Kotlin + JavaFX. A user logs in with a master password,
then manages "associations" (commitments to a company/website/etc.). Each association can hold
credentials (passwords + usernames) and metadata (category, comments, flags). All vault data is
stored encrypted in a `<username>.vault` file.

## Toolchain

- JDK 25 (Corretto via SDKMAN), Gradle 9.7.1 (wrapper), Kotlin 2.3.0, JavaFX 26.0.2.
- JavaFX version is set in TWO places in `build.gradle.kts`: the `javafx {}` plugin block and the
  explicit `org.openjfx:javafx-graphics` dependency (plus a commented `:win` line for Windows
  builds). Keep them in sync.
- Build: `./gradlew build` · Run: `./gradlew run` · Production: `./gradlew fatJar`.
- macOS and Windows are supported (`OSPlatform`); anything else throws.

## File locations at runtime

- `vault.settings` (single line: the profiles directory) lives in
  `~/Library/Application Support/Vault` (mac) or `%APPDATA%\Vault` (win).
- Development mode: set env var `IS_DEVELOPMENT=true` (manually sourced, not set by Gradle) —
  then `vault.settings` is read from the current working directory (repo root has one).
- `.vault` profile files live in the directory named by `vault.settings` (`FileService`).

## Architecture

- `Main.kt` launches `Router` (the `Application`), which swaps views (Login/Register/Main) in a
  single primary stage.
- FXML files in `src/main/resources`, one controller class per view in `controller/`.
  Controllers are attached programmatically via `loader.setController(...)` — FXML files have
  NO `fx:controller` attribute. Constructor args carry state (user, association, callbacks).
- Wiring happens in `@FXML fun initialize()`; anything needing the scene/window is deferred with
  `Platform.runLater { ... }`.
- `model/` holds plain data classes (`Credential`, `Association`, ...); `model/ui/` holds
  JavaFX-property mirrors (`UiCredential`, `UiAssociation`, `UiProfile`) mapped via
  `fromX`/`toX` methods.
- Dialogs in `dialog/` extend `javafx.scene.control.Dialog` and pair with controllers in
  `controller/dialog/`. They return results via `showAndWait()` and validate input with an
  evaluator lambda that throws to signal errors.
- Crypto in `crypto/` (AES GCM/CBC, PBKDF2, HMAC). The master password is cached in memory for
  `savePasswordForMinutes` (`UiPasswordData`); `passwordRequiredAction()` returns the cached
  password or prompts.
- The credentials modal (`Credentials.fxml` + `CredentialsController`) is a separate `Stage`
  created in `AssociationController.setupCredentialsButton`, `WINDOW_MODAL` with the main window
  as owner.

## Platform gotcha: showAndWait vs the macOS close button

On macOS 26 (Tahoe), a window shown with `showAndWait()` has a native bug: after any mouse
interaction with the window's content, the next click on the red title-bar close button is
silently swallowed (it never reaches JavaFX), so closing takes two clicks. Diagnosed Aug 2026;
reproduced on JavaFX 24–26 and confirmed project-independent with a minimal standalone app
(plain Java + stock JavaFX jars); ownership/modality are NOT involved — only the nested event
loop. Reported to Oracle via bugreport.java.com on 2026-08-23, internal review ID
18ecb8f5-8a24-45a6-8bb0-b7e861f8bebc (repro and report text in `bugs/macos-showandwait-close-click/`); no public JDK number yet.

- Decision (2026-08-23): the bug is ACCEPTED and the app uses plain `showAndWait()` everywhere.
  Two verified workarounds (plain `show()`, and a coroutine-based `await()` replacement) were
  implemented app-wide and then deliberately reverted to keep the code simple. They are fully
  documented in `BUGS.md` — reimplement from there if the decision changes; do not re-derive.

## Conventions

- UI sizing for the credentials modal is hardcoded per platform (mac: 330x614, win: 346x625).
- Icons come from fontawesomefx (`FontAwesomeIconView`, `MaterialDesignIconView`), colors from
  `utils/Colors`.
- App icons live in `packaging/`: one SVG source (`vault-icon.svg`, a rounded dark tile) feeds
  both `vault.ico` (Windows) and `vault.icns` (macOS). Known cosmetic limitation, accepted after
  experimenting: macOS 26 (Tahoe) draws every legacy `.icns` on a system plate ("icon jail"),
  visible as a thin ring around the tile — NO `.icns` artwork geometry avoids it (full-bleed,
  Apple-grid squircle and glyph-on-transparency were all tried and look worse). A plate-free
  native look requires the Icon Composer format (Assets.car + CFBundleIconName), which needs
  full Xcode. Windows shows the `.ico` artwork as-is.
- Unsaved-changes protection: `CredentialsController` snapshots the initial credentials and
  compares in `credentialsAreAltered` on close; `MainController` uses `user.isDirty`.
- App version lives in `build.gradle.kts` (`version = "..."`) and is shown in the UI via the
  jar manifest `Implementation-Version`.

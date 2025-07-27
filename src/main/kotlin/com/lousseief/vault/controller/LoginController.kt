package com.lousseief.vault.controller

import com.lousseief.vault.Router
import com.lousseief.vault.exception.AuthenticationException
import com.lousseief.vault.model.ui.UiProfile
import com.lousseief.vault.service.FileService
import com.lousseief.vault.service.UserService
import javafx.application.Platform
import javafx.beans.property.SimpleStringProperty
import javafx.fxml.FXML
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.Hyperlink
import javafx.scene.control.TextField
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent

class LoginController(private val router: Router) {

    @FXML
    private lateinit var loginButton: Button

    @FXML
    private lateinit var registerLink: Hyperlink

    @FXML
    private lateinit var usernameField: TextField

    @FXML
    private lateinit var passwordField: TextField

    private val username = SimpleStringProperty(null)
    private val password = SimpleStringProperty(null)

    private fun attemptLogin() {
        if (!FileService.userExists(username.value)) {
            Alert(Alert.AlertType.ERROR).apply {
                title = "Something went wrong"
                headerText = "Username or password invalid"
                contentText = "The username or password was invalid, please try again"
            }.showAndWait()
            passwordField.requestFocus()
            passwordField.selectAll()
        } else {
            try {
                val loggedInUser = UserService.loadUser(username.value)
                val (associations, settings, userNames) = loggedInUser.initialize(password.value)
                val uiProfile = UiProfile.fromProfile(loggedInUser, associations, settings, userNames, password.value)
                router.showMain(uiProfile)
            }
            catch(e: AuthenticationException) {
                Alert(Alert.AlertType.ERROR).apply {
                    title = "Something went wrong"
                    headerText = "Login failed"
                    contentText = e.message
                }.showAndWait()
                passwordField.requestFocus()
                passwordField.selectAll()
            }
        }
    }

    @FXML
    fun initialize() {
        // save button is disabled until every field has a value
        loginButton.disableProperty().bind(username.isNull.or(password.isNull))

        Platform.runLater {
            username.bind(usernameField.textProperty())
            password.bind(passwordField.textProperty())

            registerLink.setOnAction { router.showRegister() }
            usernameField.addEventFilter(KeyEvent.KEY_PRESSED) { event ->
                if (event.code.equals(KeyCode.ENTER) && username.isNotEmpty.value)
                    passwordField.requestFocus()
            }
            passwordField.addEventFilter(KeyEvent.KEY_PRESSED) { event ->
                if (event.code.equals(KeyCode.ENTER) && password.isNotEmpty.value) {
                    attemptLogin()
                }
            }
            loginButton.addEventFilter(KeyEvent.KEY_PRESSED) {
                if (it.code === KeyCode.ENTER) {
                    attemptLogin()
                }
            }
            loginButton.setOnAction {
                attemptLogin()
            }
        }
    }
}


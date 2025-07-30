package com.lousseief.vault.controller

import com.lousseief.vault.Router
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

class RegisterController(private val router: Router) {

    @FXML
    private lateinit var registerButton: Button

    @FXML
    private lateinit var loginLink: Hyperlink

    @FXML
    private lateinit var usernameField: TextField

    @FXML
    private lateinit var passwordField: TextField

    @FXML
    private lateinit var passwordRepetitionField: TextField

    private val username = SimpleStringProperty(null)
    private val password = SimpleStringProperty(null)
    private val passwordRepetition = SimpleStringProperty(null)

    fun registerUser() {
        try {
            if (password.value != passwordRepetition.value) {
                passwordRepetitionField.requestFocus()
                passwordRepetitionField.selectAll()
                throw Exception("The password and password repetition didn't match.")
            }
            else if (password.value.isEmpty()) {
                passwordField.requestFocus()
                throw Exception("Empty password are not allowed.")
            }
            else {
                usernameField.requestFocus()
                usernameField.selectAll()
            }
            UserService.createUser(username.value, password.value)
            Alert(Alert.AlertType.INFORMATION).apply {
                title = "Success"
                headerText = "User added"
                contentText = "The user was successfully added! Please go ahead and login!"
            }.showAndWait()
            router.showLogin(false)
        } catch (e: Exception) {
            Alert(Alert.AlertType.ERROR).apply {
                title = "Something went wrong"
                headerText = "User registration failed"
                contentText = e.message
            }.showAndWait()
        }
        println("Name is: " + username.get() + " " + password.get())
    }

    @FXML
    fun initialize() {
        // save button is disabled until every field has a value
        registerButton.disableProperty().bind(username.isNull.or(password.isNull).or(passwordRepetition.isNull))

        Platform.runLater {
            username.bind(usernameField.textProperty())
            password.bind(passwordField.textProperty())
            passwordRepetition.bind(passwordRepetitionField.textProperty())

            loginLink.setOnAction { router.showLogin(false) }
            usernameField.addEventFilter(KeyEvent.KEY_PRESSED) { event ->
                if(event.code.equals(KeyCode.ENTER) && username.isNotEmpty.value)
                    passwordField.requestFocus()
            }
            passwordField.addEventFilter(KeyEvent.KEY_PRESSED) { event ->
                if(event.code.equals(KeyCode.ENTER) && password.isNotEmpty.value) {
                    registerUser()
                }
            }
            passwordRepetitionField.addEventFilter(KeyEvent.KEY_PRESSED) { event ->
                if(event.code.equals(KeyCode.ENTER) && passwordRepetition.isNotEmpty.value) {
                    registerUser()
                }
            }
            registerButton.addEventFilter(KeyEvent.KEY_PRESSED) {
                if (it.code === KeyCode.ENTER) {
                    registerUser()
                }
            }
            registerButton.setOnAction {
                registerUser()
            }
        }
    }
}


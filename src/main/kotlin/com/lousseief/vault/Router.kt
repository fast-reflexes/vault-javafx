package com.lousseief.vault

import com.lousseief.vault.controller.LoginController
import com.lousseief.vault.controller.MainController
import com.lousseief.vault.controller.RegisterController
import com.lousseief.vault.dialog.ChooseProfilesLocationDialog
import com.lousseief.vault.model.ui.UiProfile
import com.lousseief.vault.service.FileService
import com.lousseief.vault.utils.Colors
import com.lousseief.vault.utils.setupStage
import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Alert
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.stage.Stage

class Router : Application() {
    private lateinit var root: StackPane
    private lateinit var stage: Stage

    override fun start(stage: Stage) {
        root = StackPane()
        this.stage = stage
        val scene = Scene(root)
        stage.scene = scene
        stage.title = "Vault"
        scene.fill = Color.valueOf(Colors.GRAY)
        scene.stylesheets.add("/styles/styles.css")
        stage.show()
        showLogin()
        val systemIsSetup = FileService.setupSystemSettings()
        if(!systemIsSetup) {
            val directory = ChooseProfilesLocationDialog(true)
                .showAndWait()
            if(directory.isPresent && directory.get().isNotEmpty()) {
                Alert(Alert.AlertType.CONFIRMATION).apply {
                    title = "Success"
                    headerText = "Location added"
                    contentText = "Successfully set the profiles location to '${directory.get()}'"
                }.showAndWait()
                FileService.writeSystemSettingsFile(directory.get())
            } else {
                Alert(Alert.AlertType.ERROR).apply {
                    title = "Setup error"
                    headerText = "Setup error"
                    contentText = "Cannot start app without selecting directory, app will close"
                }.showAndWait()
                stage.close()
            }
        }

    }

    fun showLogin() {
        val loader = FXMLLoader(javaClass.getResource("/Login.fxml"))
        loader.setController(LoginController(this))
        val view: Parent = loader.load()
        root.children.setAll(view)
        setupStage(stage, true)

    }

    fun showRegister() {
        val loader = FXMLLoader(javaClass.getResource("/Register.fxml"))
        loader.setController(RegisterController(this))
        val view: Parent = loader.load()
        root.children.setAll(view)
        setupStage(stage, true)
    }

    fun showMain(user: UiProfile) {
        val loader = FXMLLoader(javaClass.getResource("/Main.fxml"))
        loader.setController(MainController(this, user))
        val view: Parent = loader.load()
        root.children.setAll(view)
        setupStage(stage)
    }

}

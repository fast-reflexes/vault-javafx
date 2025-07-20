package com.lousseief.vault

import com.lousseief.vault.controller.LoginController
import com.lousseief.vault.controller.MainController
import com.lousseief.vault.controller.RegisterController
import com.lousseief.vault.model.UiProfile
import com.lousseief.vault.utils.setupStage
import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
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
        scene.fill = Color.valueOf("#EDEDED")
        scene.stylesheets.add("/styles.css");
        stage.show()
        showLogin()
    }

    fun showLogin() {
        val loader = FXMLLoader(javaClass.getResource("/login.fxml"))
        loader.setController(LoginController(this))
        val view: Parent = loader.load()
        root.children.setAll(view)
        setupStage(stage, true)

    }

    fun showRegister() {
        val loader = FXMLLoader(javaClass.getResource("/register.fxml"))
        loader.setController(RegisterController(this))
        val view: Parent = loader.load()
        root.children.setAll(view)
        setupStage(stage, true)
    }

    fun showMain(user: UiProfile) {
        val loader = FXMLLoader(javaClass.getResource("/main.fxml"))
        loader.setController(MainController(this, user))
        val view: Parent = loader.load()
        root.children.setAll(view)
        setupStage(stage)
    }

}

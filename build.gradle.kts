
plugins {
    java
    kotlin("jvm") version "2.2.0"
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
}

javafx {
    // will pull in transitive modules
    modules("javafx.controls", "javafx.fxml") // replace with what you modules need

    version = "24.0.2" // or whatever version you're using
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_23)
    }
}


group = "com.lousseief"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("com.google.code.gson:gson:2.13.1")
    implementation("de.jensd:fontawesomefx-commons:9.1.2")
    implementation("de.jensd:fontawesomefx-controls:9.1.2")
    implementation("de.jensd:fontawesomefx-emojione:3.1.1-9.1.2")
    implementation("de.jensd:fontawesomefx-fontawesome:4.7.0-9.1.2")
    implementation("de.jensd:fontawesomefx-materialdesignfont:2.0.26-9.1.2")
    implementation("de.jensd:fontawesomefx-materialicons:2.2.0-9.1.2")
    implementation("de.jensd:fontawesomefx-octicons:4.3.0-9.1.2")
    implementation("de.jensd:fontawesomefx-icons525:4.2.0-9.1.2")
    implementation("de.jensd:fontawesomefx-weathericons:2.0.10-9.1.2")
    implementation("org.openjfx:javafx-graphics:24.0.2")
    implementation("org.controlsfx:controlsfx:9.0.0")
    implementation("commons-codec:commons-codec:1.18.0")

    testImplementation("junit", "junit", "4.12")
}

configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_23
}

application {
    mainClass.set("com.lousseief.vault.MainKt")

    applicationDefaultJvmArgs = listOf(
        //"--add-opens=javafx.graphics/javafx.scene=ALL-UNNAMED", // likely not needed anymore
        "--add-opens=java.base/java.time=ALL-UNNAMED"
    )
}



plugins {
    java
    kotlin("jvm") version "2.3.0"
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
}

javafx {
    // will pull in transitive modules
    modules("javafx.controls", "javafx.fxml") // replace with what you modules need

    version = "26.0.2" // or whatever version you're using
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25)
    }
}


group = "com.lousseief"
version = "1.1.1"

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

    // for running on and compiling for mac
    implementation("org.openjfx:javafx-graphics:26.0.2")

    // for compiling the windows jar
    //implementation("org.openjfx:javafx-graphics:26.0.2:win")
    implementation("org.controlsfx:controlsfx:9.0.0")
    implementation("commons-codec:commons-codec:1.18.0")

    testImplementation("junit:junit:4.13.2")
}

tasks.jar {
    manifest.attributes.apply {
        put("Implementation-Title", "Vault thin jar")
        put("Implementation-Version", version)
        put("Main-Class", "com.lousseief.vault.MainKt")
    }
}

tasks.register<Jar>("fatJar") {
    archiveClassifier.set("fat")
    manifest.attributes.apply {
        put("Implementation-Title", "Vault fat jar")
        put("Implementation-Version", version)
        put("Main-Class", "com.lousseief.vault.MainKt")
        // in the fat jar JavaFX lives on the classpath (unnamed module); this grants it native access
        // when run with java -jar, silencing the JDK 24+ restricted-method warning
        put("Enable-Native-Access", "ALL-UNNAMED")
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.endsWith("jar") }
            .map { zipTree(it) }
    })
}

configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_25
}

application {
    mainClass.set("com.lousseief.vault.MainKt")
    applicationDefaultJvmArgs = listOf(
        // JavaFX loads native libraries; JDK 24+ warns (and will later refuse) unless granted explicitly
        "--enable-native-access=javafx.graphics",
        //"--add-opens=javafx.graphics/javafx.scene=ALL-UNNAMED", // likely not needed anymore
        //"--add-opens=java.base/java.time=ALL-UNNAMED" // not needed with custom typeadapter for Instant
    )
}


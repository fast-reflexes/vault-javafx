package com.lousseief.vault.utils

object OSPlatform {

    var isWindows: Boolean
    var isMac: Boolean
    var os: String

    init {
        // Get the OS name
        val osName = System.getProperty("os.name").lowercase()
        os = osName
        isMac = osName.contains("mac")
        isWindows = osName.contains("win")
    }
}

package com.lousseief.vault.utils

/**
 * Console-only debug logging for local development.
 *
 * Output is written to stderr and requires BOTH env vars to be set:
 *
 *     IS_DEVELOPMENT=true DEBUG=true ./gradlew run
 *
 * Neither is set by Gradle, so a normal run (and any shipped build) is completely silent.
 * Messages are passed as lambdas, so nothing is evaluated while logging is off.
 *
 * NOTE: these logs are deliberately unredacted and may contain the master password and
 * vault contents. That is acceptable for local debugging only — never enable both flags
 * on a machine holding a real vault you care about, and never in a distributed build.
 */
object Log {

    val enabled: Boolean =
        System.getenv("IS_DEVELOPMENT")?.lowercase() == "true" &&
            System.getenv("DEBUG")?.lowercase() == "true"

    inline fun debug(message: () -> String) {
        if (enabled) System.err.println("[debug] ${message()}")
    }
}

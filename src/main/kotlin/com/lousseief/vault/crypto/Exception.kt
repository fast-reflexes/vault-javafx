package com.lousseief.vault.crypto

interface Cause {
    val name: String
}

class CryptoException(message: CryptoExceptionCause, val e: Throwable? = null): Exception(message.explanation) {

    enum class CryptoExceptionCause(val explanation: String): Cause {
        CHARACTER_POOL_CONTAINS_DUPLICATES("The characters to use for password generation are not unique. Please insert only a string of unique characters as material for password generation."),
        ILLEGAL_KEY_SIZE("Key size not accepted, only 256-bit keys must be used"),
        INPUT_ARRAYS_HAVE_DIFFERENT_SIZE("The inputs to compare have different sizes")
    }
}

class EncryptionException(message: EncryptionExceptionCause, val e: Throwable? = null): Exception(message.explanation) {

    enum class EncryptionExceptionCause(val explanation: String): Cause {
        UNAUTHORIZED("Bad password"),

    }
}

class DecryptionException(message: DecryptionExceptionCause, val e: Throwable? = null): Exception(message.explanation) {

    enum class DecryptionExceptionCause(val explanation: String): Cause {
        UNAUTHORIZED("Bad password")
    }
}
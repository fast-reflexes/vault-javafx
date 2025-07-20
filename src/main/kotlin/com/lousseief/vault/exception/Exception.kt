package com.lousseief.vault.exception

interface Cause {
    val name: String
}

class AuthenticationException(message: AuthenticationExceptionCause, val e: Throwable? = null): Exception(message.explanation) {

    enum class AuthenticationExceptionCause(val explanation: String): Cause {
        UNAUTHORIZED("The password was incorrect"),
        EMPTY_PASSWORDS_NOT_ALLOWED("The password must contain at least one character")
    }
}

class VerificationException(message: VerificationExceptionCause, val e: Throwable? = null): Exception(message.explanation) {

    enum class VerificationExceptionCause(val explanation: String): Cause {
        INTEGRITY_CHECK_FAILED("Checksum of input content and input checksum differed"),
    }
}

class UserException(message: UserExceptionCause, val e: Throwable? = null): Exception(message.explanation) {

    enum class UserExceptionCause(val explanation: String): Cause {
        USER_EXISTS("The user name is already taken, please pick an other one.")
    }
}

class FileException(message: FileExceptionCause, val e: Throwable? = null): Exception(message.explanation) {

    enum class FileExceptionCause(val explanation: String): Cause {
        NOT_FOUND("User doesn't exist"),
        READ_ERROR("There was an error while reading the user data from disk"),
        CORRUPT_FILE("The file doesn't seem to be a legitimate .vault file"),
        WRITE_ERROR("There was an error while saving changes to disk, please try again"),
    }
}
class InternalException(message: InternalExceptionCause, val e: Throwable? = null): Exception(message.explanation) {

    enum class InternalExceptionCause(val explanation: String): Cause {
        FILE_EXISTS("The .vault file already exists and the overwrite flag was not set to true"),
        USERNAME_TO_REMOVE_NOT_FOUND("The user tried to remove a user name that doesn't exist"),
        USERNAME_TO_REMOVE_ZERO_OR_LESS("The user tried to remove a user name that was listed as having zero or less occurrences."),
        MISSING_IDENTIFIER("The requested entry could not be found"),
        UNEXPECTED_CRYPTO_SIZE("An internal cryptographic token was not of expected size"),
        IMPLICIT_OVERWRITE("Can't write to an existing .vault file without permission to overwrite")
    }

}
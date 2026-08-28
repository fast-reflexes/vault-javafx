package com.lousseief.vault.crypto

import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import java.security.NoSuchAlgorithmException
import java.security.spec.InvalidKeySpecException

object KeyDerivation {

    private const val OUTPUT_BITS = 512
    private const val OUTPUT_BYTES = OUTPUT_BITS / 8
    private const val ITERATIONS = 210000 // should be 210000
    private const val SALT_BITS = 512
    private const val SALT_BYTES = SALT_BITS / 8

    data class PBKDF2Delivery(
        val salt: ByteArray,
        val key: ByteArray
    )

    fun deriveKey(password: String, salt: ByteArray? = null): PBKDF2Delivery {
        val saltToUse = salt ?: ByteArray(SALT_BYTES).also{ CryptoUtils.fillRandom(it) }
        val passwordChars = password.toCharArray()
        val spec = PBEKeySpec(passwordChars, saltToUse,
            ITERATIONS,
            OUTPUT_BITS
        )
        try {
            val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512")
            return PBKDF2Delivery(saltToUse, skf.generateSecret(spec).encoded)
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        } catch(e: InvalidKeySpecException) {
            throw RuntimeException(e)
        } finally {
            /* the spec and the char array each hold their own copy of the password - scrub both
            (the String passed in cannot be scrubbed, see the README on memory hygiene) */
            spec.clearPassword()
            passwordChars.fill(0.toChar())
        }
    }

}

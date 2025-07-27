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
        try {
            val saltToUse = salt ?: ByteArray(SALT_BYTES).also{ CryptoUtils.fillRandom(it) }
            val passwordChars = password.toCharArray()
            val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512")
            val spec = PBEKeySpec(passwordChars, saltToUse,
                ITERATIONS,
                OUTPUT_BITS
            )
            val key = skf.generateSecret(spec)
            val res = key.encoded
            return PBKDF2Delivery(saltToUse, res)
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        } catch(e: InvalidKeySpecException) {
            throw RuntimeException(e)
        }
    }

}

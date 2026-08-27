package com.lousseief.vault.service

import com.lousseief.vault.crypto.Conversion
import com.lousseief.vault.crypto.KeyDerivation
import com.lousseief.vault.exception.AuthenticationException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class VerificationServiceTest {

    private val password = "correct horse battery staple"

    /** Builds the salts and stored hash the way a vault file carries them. */
    private fun keyMaterialAndStoredHash(): Triple<ByteArray, String, Pair<String, String>> {
        val (keyMaterialSaltBytes, keyMaterialBytes) = KeyDerivation.deriveKey(password)
        val (verificationSaltBytes, hashBytes) =
            KeyDerivation.deriveKey(Conversion.bytesToBase64(keyMaterialBytes))
        return Triple(
            keyMaterialBytes,
            Conversion.bytesToBase64(keyMaterialSaltBytes),
            Conversion.bytesToBase64(hashBytes) to Conversion.bytesToBase64(verificationSaltBytes)
        )
    }

    @Test
    fun `the correct password authorizes and yields the key material`() {
        val (keyMaterial, keyMaterialSalt, hashAndSalt) = keyMaterialAndStoredHash()
        val (hash, salt) = hashAndSalt

        val result = VerificationService.authorize(password, keyMaterialSalt, hash, salt)

        assertArrayEquals(keyMaterial, result)
    }

    @Test(expected = AuthenticationException::class)
    fun `a wrong password is rejected`() {
        val (_, keyMaterialSalt, hashAndSalt) = keyMaterialAndStoredHash()
        val (hash, salt) = hashAndSalt
        VerificationService.authorize("not the password", keyMaterialSalt, hash, salt)
    }

    @Test
    fun `a hash produced by createKeyMaterial authorizes`() {
        val verificationData = UserService.createKeyMaterial(password)
        val result = VerificationService.authorize(
            password,
            Conversion.bytesToBase64(verificationData.keyMaterialSalt),
            Conversion.bytesToBase64(verificationData.verificationHash),
            Conversion.bytesToBase64(verificationData.verificationSalt)
        )
        assertArrayEquals(
            verificationData.encryptionKeyBytes + verificationData.hmacKeyBytes,
            result
        )
    }

    @Test
    fun `base64 survives a round trip where a UTF-8 decode does not`() {
        /* the reason the verification hash is derived from base64 of the key material rather than from a
        UTF-8 decode of it: every byte above 0x7F collapses into the replacement character */
        val (_, keyMaterialBytes) = KeyDerivation.deriveKey(password)
        assertArrayEquals(
            keyMaterialBytes,
            Conversion.Base64ToBytes(Conversion.bytesToBase64(keyMaterialBytes))
        )
        assertFalse(
            keyMaterialBytes.contentEquals(
                Conversion.UTF8ToBytes(Conversion.bytesToUTF8(keyMaterialBytes))
            )
        )
    }
}

package com.lousseief.vault.service

import com.lousseief.vault.crypto.Conversion
import com.lousseief.vault.crypto.Hmac
import com.lousseief.vault.crypto.KeyDerivation
import com.lousseief.vault.exception.InternalException
import com.lousseief.vault.exception.UserException
import com.lousseief.vault.model.AssociationWithCredentials
import com.lousseief.vault.model.Profile
import com.lousseief.vault.model.Settings

object UserService {

    fun loadUser(userName: String): Profile =
        FileService.readFile(userName)

    fun createKeyMaterial(masterPassword: String): VerificationData {
        val (saltBytes, keyMaterialBytes) = KeyDerivation.deriveKey(masterPassword)
        val (hashSaltBytes, hashBytes) = KeyDerivation.deriveKey(Conversion.bytesToUTF8(keyMaterialBytes))
        if(keyMaterialBytes.size != 64)
            throw InternalException(InternalException.InternalExceptionCause.UNEXPECTED_CRYPTO_SIZE)
        return VerificationData(
            saltBytes,
            hashBytes,
            hashSaltBytes,
            keyMaterialBytes.sliceArray(0 until 32),
            keyMaterialBytes.sliceArray(32 until 64)
        )
    }

    fun createUser(name: String, password: String): Profile {
        if (FileService.userExists(name))
            throw UserException(UserException.UserExceptionCause.USER_EXISTS)
        val (saltBytes, hashBytes, hashSaltBytes, encryptionKeyBytes, hmacKeyBytes) = createKeyMaterial(password)

        val (iv, cipherText) = VaultService.encryptVault(
            encryptionKeyBytes,
            Pair(Settings(), emptyMap<String, AssociationWithCredentials>())
        )
        val newUser = Profile(
            name,
            Conversion.bytesToBase64(saltBytes),
            Conversion.bytesToBase64(hashSaltBytes),
            Conversion.bytesToBase64(hashBytes),
            iv,
            cipherText
        )
        newUser.checkSum =
            Conversion.bytesToBase64(
                Hmac.generateMac(
                    Conversion.UTF8ToBytes(newUser.toContentString()),
                    hmacKeyBytes
                )
            )
        println("" + newUser.checkSum.length + " " + Conversion.Base64ToBytes(newUser.checkSum).size)
        FileService.writeVaultFile(newUser.name, newUser.toString(), false)
        return newUser
    }

}

data class VerificationData(
    val keyMaterialSalt: ByteArray,
    val verificationHash: ByteArray,
    val verificationSalt: ByteArray,
    val encryptionKeyBytes: ByteArray,
    val hmacKeyBytes: ByteArray
)

package com.lousseief.vault.model

import com.lousseief.vault.crypto.Conversion
import com.lousseief.vault.crypto.Hmac
import com.lousseief.vault.service.UserService
import com.lousseief.vault.service.VaultService
import com.lousseief.vault.service.VerificationService

abstract class IProfile(
    var keyMaterialSalt: String,
    var verificationSalt: String,
    var verificationHash: String,
    var iv: String,
    var encryptedData: String,
    var checkSum: String = "",
) {

    /**
     * @param password - the password to decrypt the vault
     * @param vaultManipulation - manipulation of the vault that outputs a new vault
     * @param encrypt - flag indicating if the in-memory vault needs to be reencrypted after the call
     * @param updatedPassword - if the reencryption of the vault should use a new password instead of the old
     * @param requireNewIv - whether this operation should reuse the existing iv (in-session encryption) or not
     *      (persisting to disk)
     *
     * @returns the current vault after possible update operations
     */
    fun accessVault(
        password: String,
        vaultManipulation: ((vault: MutableVault) -> Vault)? = null,
        encrypt: Boolean = false,
        updatedPassword: String = password,
        requireNewIv: Boolean = false,
        postEncryptionCallback: (() -> Unit)? = null
    ): Vault {
        // verify that the correct password is used and if so, return the derived key
        return VerificationService.authorize(
            password, keyMaterialSalt, verificationHash, verificationSalt
        )
            .let {
                Pair(it.sliceArray(0 until 32), it.sliceArray(32 until 64))
            }
            .also { (_, hMacKeyBytes) ->
                // verify integrity of the data we use
                VerificationService.verify(hMacKeyBytes, toContentString(), checkSum)
            }
            .let { (encryptionKeyBytes, hMacKeyBytes) ->
                VaultService.decryptVault(encryptedData, iv, encryptionKeyBytes)
                    .let { if (vaultManipulation !== null) vaultManipulation(it) else it }
                    .let { vault ->
                        if (encrypt) {
                            var encryptionKeyBytesToUse = encryptionKeyBytes
                            var hmacKeyBytesToUse = hMacKeyBytes
                            if (password != updatedPassword) {
                                val (
                                    updatedSaltBytes,
                                    updatedHashBytes,
                                    updatedHashSalt,
                                    updatedEncryptionKeyBytes,
                                    updatedHmacKeyBytes
                                ) = UserService.createKeyMaterial(updatedPassword)
                                keyMaterialSalt = Conversion.bytesToBase64(updatedSaltBytes)
                                verificationHash = Conversion.bytesToBase64(updatedHashBytes)
                                verificationSalt = Conversion.bytesToBase64(updatedHashSalt)
                                encryptionKeyBytesToUse = updatedEncryptionKeyBytes
                                hmacKeyBytesToUse = updatedHmacKeyBytes
                            }
                            val (nextIv, nextCipherText) = VaultService.encryptVault(
                                encryptionKeyBytesToUse,
                                vault,
                                if (requireNewIv) null else iv
                            )
                            iv = nextIv
                            encryptedData = nextCipherText
                            checkSum = Conversion.bytesToBase64(
                                Hmac.generateMac(
                                    Conversion.UTF8ToBytes(toContentString()),
                                    hmacKeyBytesToUse
                                )
                            )
                            if(postEncryptionCallback != null) {
                                postEncryptionCallback()
                            }
                        }
                        vault
                    }
            }
    }

    fun toContentString(): String =
        "${keyMaterialSalt}\n${verificationSalt}\n${verificationHash}\n${iv}\n${encryptedData}"


    override fun toString(): String =
        "${toContentString()}\n${checkSum}"
}

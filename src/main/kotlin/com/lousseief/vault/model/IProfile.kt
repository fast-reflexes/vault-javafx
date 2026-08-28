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
        val keyMaterialBytes = VerificationService.authorize(
            password, keyMaterialSalt, verificationHash, verificationSalt
        )
        val encryptionKeyBytes = keyMaterialBytes.sliceArray(0 until 32)
        val hMacKeyBytes = keyMaterialBytes.sliceArray(32 until 64)
        try {
            // verify integrity of the data we use
            VerificationService.verify(hMacKeyBytes, toContentString(), checkSum)
            val decryptedVault = VaultService.decryptVault(encryptedData, iv, encryptionKeyBytes)
            val vault = if (vaultManipulation !== null) vaultManipulation(decryptedVault) else decryptedVault
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
                try {
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
                } finally {
                    /* when the password was changed these are fresh arrays that the outer finally
                    doesn't know about; when it wasn't, they alias the outer ones and scrubbing twice
                    is harmless */
                    encryptionKeyBytesToUse.fill(0)
                    hmacKeyBytesToUse.fill(0)
                }
                if(postEncryptionCallback != null) {
                    postEncryptionCallback()
                }
            }
            return vault
        } finally {
            // best effort memory hygiene: the keys are not needed once the call is over
            keyMaterialBytes.fill(0)
            encryptionKeyBytes.fill(0)
            hMacKeyBytes.fill(0)
        }
    }

    fun toContentString(): String =
        "${keyMaterialSalt}\n${verificationSalt}\n${verificationHash}\n${iv}\n${encryptedData}"


    override fun toString(): String =
        "${toContentString()}\n${checkSum}"
}

package com.lousseief.vault.crypto

import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object CbcCrypto: Crypto {

    private const val CIPHER = "AES"
    private const val BLOCK_CIPHER_MODE = "CBC"
    private const val KEY_BITS = 256
    private const val KEY_BYTES = KEY_BITS / 8
    private const val PADDING = "PKCS5PADDING"
    private const val IV_BITS = 128 // same as block size
    private const val IV_BYTES = IV_BITS / 8

    override fun encrypt(plainTextBytes: ByteArray, keyBytes: ByteArray, ivBytes: ByteArray?): EncryptionDelivery {
        if(keyBytes.size != KEY_BYTES)
            throw CryptoException(CryptoException.CryptoExceptionCause.ILLEGAL_KEY_SIZE)
        val ivBytesToUse = ByteArray(IV_BYTES).also { CryptoUtils.fillRandom(it) }
        val ivSpec = IvParameterSpec(ivBytesToUse)
        val keySpec = SecretKeySpec(keyBytes, CIPHER)

        val cipher = Cipher.getInstance("$CIPHER/$BLOCK_CIPHER_MODE/$PADDING")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)

        val cipherBytes = cipher.doFinal(plainTextBytes)

        return EncryptionDelivery(cipherBytes, ivBytesToUse)
    }

    override fun decrypt(cipherTextBytes: ByteArray, keyBytes: ByteArray, ivBytes: ByteArray): ByteArray {
        if(keyBytes.size != KEY_BYTES)
            throw CryptoException(CryptoException.CryptoExceptionCause.ILLEGAL_KEY_SIZE)
        val ivSpec = IvParameterSpec(ivBytes)
        val keySpec = SecretKeySpec(keyBytes, CIPHER)

        val cipher = Cipher.getInstance("$CIPHER/$BLOCK_CIPHER_MODE/$PADDING")
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)

        val plainBytes = cipher.doFinal(cipherTextBytes)

        return plainBytes
    }
}
package com.lousseief.vault.crypto

import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object GcmCrypto: Crypto {

    private const val CIPHER = "AES"
    private const val BLOCK_CIPHER_MODE = "GCM"
    private const val KEY_BITS = 256
    private const val KEY_BYTES = KEY_BITS / 8
    private const val PADDING = "NoPadding"
    private const val IV_BITS = 128 // in case of random IV
    private const val IV_BYTES = IV_BITS / 8
    private const val TAG_LENGTH_BITS = 128
    private const val TAG_LENGTH_BYTES = TAG_LENGTH_BITS / 8

    override fun encrypt(plainTextBytes: ByteArray, keyBytes: ByteArray, ivBytes: ByteArray?): EncryptionDelivery {
        if(keyBytes.size != KEY_BYTES)
            throw CryptoException(CryptoException.CryptoExceptionCause.ILLEGAL_KEY_SIZE)
        val ivBytesToUse = ivBytes ?: ByteArray(IV_BYTES).also { CryptoUtils.fillRandom(it) }

        val ivSpec = GCMParameterSpec(TAG_LENGTH_BITS, ivBytesToUse)
        val keySpec = SecretKeySpec(keyBytes, CIPHER)

        val cipher = Cipher.getInstance("$CIPHER/$BLOCK_CIPHER_MODE/$PADDING")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)

        val cipherBytes = cipher.doFinal(plainTextBytes)

        return EncryptionDelivery(cipherBytes, ivBytesToUse)
    }

    override fun decrypt(cipherTextBytes: ByteArray, keyBytes: ByteArray, ivBytes: ByteArray): ByteArray {
        if(keyBytes.size != KEY_BYTES)
            throw CryptoException(CryptoException.CryptoExceptionCause.ILLEGAL_KEY_SIZE)

        val ivSpec = GCMParameterSpec(TAG_LENGTH_BITS, ivBytes)
        val keySpec = SecretKeySpec(keyBytes, CIPHER)

        val cipher = Cipher.getInstance("$CIPHER/$BLOCK_CIPHER_MODE/$PADDING")
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)

        val plainBytes = cipher.doFinal(cipherTextBytes)

        return plainBytes
    }
}
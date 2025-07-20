package com.lousseief.vault.crypto

interface Crypto {

    fun encrypt(plainTextBytes: ByteArray, keyBytes: ByteArray, ivBytes: ByteArray? = null): EncryptionDelivery
    fun decrypt(cipherTextBytes: ByteArray, keyBytes: ByteArray, ivBytes: ByteArray): ByteArray

}

data class EncryptionDelivery(
    val cipherBytes: ByteArray,
    val ivBytes: ByteArray
)


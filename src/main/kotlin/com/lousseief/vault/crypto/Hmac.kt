package com.lousseief.vault.crypto

import com.lousseief.vault.crypto.CryptoUtils.constantCompareByteArrays
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object Hmac {

    private const val HMAC_SPEC = "HmacSHA256"

    fun generateMac(dataBytes: ByteArray, keyBytes: ByteArray): ByteArray {

        val keySpec = SecretKeySpec(keyBytes, HMAC_SPEC)
        val macGenerator = Mac.getInstance(HMAC_SPEC)

        macGenerator.init(keySpec)
        return macGenerator.doFinal(dataBytes)
    }

    fun authenticateMac(dataBytes: ByteArray, keyBytes: ByteArray, macBytes: ByteArray): Boolean =
        constantCompareByteArrays(generateMac(dataBytes, keyBytes), macBytes)

}
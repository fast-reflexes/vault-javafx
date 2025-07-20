package com.lousseief.vault.crypto

import java.security.MessageDigest

object Hash {

    private const val HASHING_SPEC = "SHA-256"

    fun hash(dataBytes: ByteArray): ByteArray {
        val hasher = MessageDigest.getInstance(HASHING_SPEC)
        return hasher.digest(dataBytes)
    }
}
package com.lousseief.vault.crypto

import java.security.SecureRandom

object CryptoUtils {

    private val randomSource = SecureRandom.getInstanceStrong()

    fun fillRandom(arrToFill: ByteArray) =
        randomSource.nextBytes(arrToFill)

    fun constantTimeCompareBytes(a: Byte, b: Byte): Int {
        val aInt = a.toInt()
        val bInt = b.toInt()
        var res = 0
        (0 until 8).forEach {
            val currentA = (aInt ushr it) and 1
            val currentB = (bInt ushr it) and 1
            res += currentA xor currentB
        }
        return res
    }

    fun constantCompareByteArrays(a: ByteArray, b: ByteArray): Boolean {
        if(a.size != b.size)
            throw CryptoException(CryptoException.CryptoExceptionCause.INPUT_ARRAYS_HAVE_DIFFERENT_SIZE)
        var similarity = 0
        val len = a.size
        (0 until len).forEach {
            similarity += (10 + constantTimeCompareBytes(
                a[it],
                b[it]
            ))
        }
        return (similarity - (len * 10)) == 0
    }

    //fun padByteArray(arr: ByteArray, len: Int): ByteArray {}
    /*fun hconstantCompareByteArrays(a: ByteArray, b: ByteArray): Boolean {
        val aLen = a.size
        val bLen = b.size
        var match = aLen == bLen
        val maxLen = maxOf(aLen, bLen)
        match = match && maxLen != 0
        val aToUse = ByteArray(maxLen)
        aToUse.fill(1)
        val bToUse = ByteArray(maxLen)
        bToUse.fill(1)
        (0 until maxLen).forEach {
            match = match && aToUse[it] == bToUse[it]
        }
        return match
    }*/

    fun getCharPoolContent(
        includeLowercase: Boolean,
        includeUppercase: Boolean,
        includeNumbers: Boolean,
        includeSpecialChars: Boolean
    ): String =
        listOf(
            Pair(includeLowercase, "abcdefghijklmnopqrstuvwxyz"),
            Pair(includeUppercase, "ABCDEFGHIJKLMNOPQRSTUVWXYZ"),
            Pair(includeNumbers, "0123456789"),
            Pair(includeSpecialChars, "-_?+=)(&%#!.:,;<>@$£")
        )
            .map { (include, chars) -> if(include) chars else "" }
            .joinToString("")

    fun generateRandomString(characterPool: String, length: Int): String {
        if(characterPool.toList().size != characterPool.toSet().size)
            throw CryptoException(CryptoException.CryptoExceptionCause.CHARACTER_POOL_CONTAINS_DUPLICATES)
        return String(
            (0 until length)
                .map {
                    val e: Char = characterPool[randomSource.nextInt(Int.MAX_VALUE).rem(characterPool.length)]
                    e
                }
                .toCharArray()
        )
    }
}

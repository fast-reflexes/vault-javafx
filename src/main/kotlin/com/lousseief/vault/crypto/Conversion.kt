package com.lousseief.vault.crypto

import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.binary.Hex
import org.apache.commons.codec.binary.StringUtils

object Conversion {

    fun bytesToHex(bytes: ByteArray): String =
        Hex.encodeHexString(bytes)

    fun hexToBytes(s: String): ByteArray =
        Hex.decodeHex(s)

    fun bytesToUTF8(bytes: ByteArray): String =
        StringUtils.newStringUtf8(bytes)

    fun UTF8ToBytes(s: String): ByteArray =
        StringUtils.getBytesUtf8(s)

    fun bytesToISO88591(bytes: ByteArray): String =
        StringUtils.newStringIso8859_1(bytes)

    fun ISO88591ToBytes(s: String): ByteArray =
        StringUtils.getBytesIso8859_1(s)

    fun bytesToAscii(bytes: ByteArray): String =
        StringUtils.newStringUsAscii(bytes)

    fun AsciiToBytes(s: String): ByteArray =
        StringUtils.getBytesUsAscii(s)

    fun bytesToBase64(bytes: ByteArray): String =
        Base64.encodeBase64String(bytes)

    fun Base64ToBytes(s: String): ByteArray =
        Base64.decodeBase64(s)
}
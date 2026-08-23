package com.lousseief.vault.crypto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CryptoUtilsTest {

    @Test
    fun `generated string has the requested length`() {
        val pool = CryptoUtils.getCharPoolContent(true, true, true, true)
        assertEquals(40, CryptoUtils.generateRandomString(pool, 40).length)
    }

    @Test
    fun `generated string only contains characters from the pool`() {
        val pool = CryptoUtils.getCharPoolContent(true, false, true, false)
        val generated = CryptoUtils.generateRandomString(pool, 100)
        assertTrue(generated.all { it in pool })
    }

    @Test
    fun `char pool respects the include flags`() {
        assertEquals("0123456789", CryptoUtils.getCharPoolContent(false, false, true, false))
        assertEquals("", CryptoUtils.getCharPoolContent(false, false, false, false))
    }

    @Test(expected = CryptoException::class)
    fun `character pool with duplicates is rejected`() {
        CryptoUtils.generateRandomString("aab", 10)
    }
}

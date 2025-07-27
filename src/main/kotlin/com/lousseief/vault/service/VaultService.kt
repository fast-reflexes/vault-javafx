package com.lousseief.vault.service

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.lousseief.vault.crypto.CbcCrypto
import com.lousseief.vault.crypto.Conversion
import com.lousseief.vault.model.MutableVault
import com.lousseief.vault.model.Vault
import java.time.Instant

class InstantAdapter: TypeAdapter<Instant>() {

    override fun read(reader: JsonReader): Instant {
        return Instant.parse(reader.nextString())
    }

    override fun write(writer: JsonWriter, instant: Instant) {
        writer.value(instant.toString())
    }

}

object VaultService {

    /* starting Java 17, internal Java classes needs custom adapters for serialization, otherwise an ...does not open
    up to ... error is displayed and we must add special args for it to work */
    val jsonMapper: Gson = GsonBuilder().registerTypeAdapter(Instant::class.java, InstantAdapter()).create()

    fun encryptVault(encryptionKey: ByteArray, vault: Vault, iv: String? = null): Pair<String, String> {
        /* use sorted map to avoid the encrypted data differing when we just by accident and modifications alter the
        iteration order of the map (e.g. remove an association which is old and then readding it) */
        val vaultWithSortedAssociations = vault.first to vault.second.toSortedMap()
        val plainString = jsonMapper.toJson(vaultWithSortedAssociations)
        return CbcCrypto.encrypt(
            Conversion.UTF8ToBytes(plainString),
            encryptionKey,
            iv?.let { Conversion.Base64ToBytes(it) }
        )
            .let { (cipherBytes, ivBytes) ->
                Pair(Conversion.bytesToBase64(ivBytes), Conversion.bytesToBase64(cipherBytes))
            }
    }

    fun decryptVault(encryptedVault: String, iv: String, encryptionKey: ByteArray): MutableVault {
        val plainBytes = CbcCrypto.decrypt(
            Conversion.Base64ToBytes(encryptedVault),
            encryptionKey,
            Conversion.Base64ToBytes(iv)
        )
        val plainText = Conversion.bytesToUTF8(plainBytes)
        val itemType = object : TypeToken<MutableVault>() {}.type
        return jsonMapper.fromJson(plainText, itemType)
    }
}

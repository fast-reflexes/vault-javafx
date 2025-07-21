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

    fun encryptVault(encryptionKey: ByteArray, vault: Vault): Pair<String, String> {
        val plainString = jsonMapper.toJson(vault)
        return CbcCrypto.encrypt(
            Conversion.UTF8ToBytes(plainString),
            encryptionKey
        )
            .let { (cipherBytes, ivBytes) ->
                Pair(Conversion.bytesToBase64(ivBytes), Conversion.bytesToBase64(cipherBytes))
            }
    }
    /*if(enc !== null) {
        val (cipherBytes, ivBytes) = enc
        val updatedUser = Profile(
            user.name,
            ConversionService.bytesToBase64(saltBytes),
            ConversionService.bytesToBase64(hashSaltBytes),
            ConversionService.bytesToBase64(hashBytes),
            AES256Service.CIPHER,
            AES256Service.PADDING,
            AES256Service.BLOCK_CIPHER_MODE,

        )
        val success = FileService.writeFile(newUser, false)
        if (success) {
            alert(
                Alert.AlertType.INFORMATION,
                "User added",
                "The user was successfully added! Please go ahead and login!"
            ) {
                replaceWith<LoginView>(
                    transition = ViewTransition.Metro(500.millis, ViewTransition.Direction.RIGHT)
                )
            }
        } else {
            alert(
                Alert.AlertType.ERROR,
                "User couldn't be added",
                "something went wrong, perhaps the user already exists? Please try again!"
            )
        }
    }
}*/

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

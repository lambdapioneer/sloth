package com.lambdapioneer.sloth.impl

import android.os.Build
import androidx.annotation.RequiresApi
import com.lambdapioneer.sloth.crypto.Hkdf
import com.lambdapioneer.sloth.crypto.PwHash
import com.lambdapioneer.sloth.secureelement.KeyHandle
import com.lambdapioneer.sloth.secureelement.SecureElement
import com.lambdapioneer.sloth.storage.ReadableStorage
import com.lambdapioneer.sloth.storage.WriteableStorage
import com.lambdapioneer.sloth.utils.NoopTracer
import com.lambdapioneer.sloth.utils.Tracer


@RequiresApi(Build.VERSION_CODES.P)
class LongSlothImpl(
    private val params: LongSlothParams,
    private val secureElement: SecureElement,
    private val pwHash: PwHash,
    private val tracer: Tracer = NoopTracer(),
) {
    private val hkdf = Hkdf()

    fun keyGen(
        storage: WriteableStorage,
        pw: String,
        h: ByteArray,
        outputLengthBytes: Int,
    ): ByteArray {
        storage.put("h", h)
        storage.put("salt", pwHash.createSalt())

        secureElement.hmacGenKey(keyHandle = KeyHandle(h))

        return derive(storage, pw, outputLengthBytes)
    }

    fun derive(storage: ReadableStorage, pw: String, outputLengthBytes: Int): ByteArray {
        tracer.addKeyValue("l", params.l.toString())
        tracer.addKeyValue("argon", pwHash.toString())

        tracer.start()

        val omegaPre = pwHash.deriveHash(
            salt = storage.get("salt"),
            password = pw,
            outputLengthInBytes = params.l,
        )

        tracer.step("afterPwHash")

        val omegaPost = secureElement.hmacDerive(
            keyHandle = KeyHandle(storage.get("h")),
            data = omegaPre
        )

        tracer.step("afterSeHmac")

        val k = hkdf.derive(
            salt = storage.get("salt"),
            ikm = omegaPost,
            info = HKDF_INFO_CONSTANT.encodeToByteArray(),
            l = outputLengthBytes
        )

        tracer.finish()

        return k
    }

    fun hasKey(storage: ReadableStorage): Boolean {
        return try {
            storage.get("h").isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        internal const val HKDF_INFO_CONSTANT = "LongSlothHkdfInfoConstant"
    }
}

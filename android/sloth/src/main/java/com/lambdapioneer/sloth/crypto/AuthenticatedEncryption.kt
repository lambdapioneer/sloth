package com.lambdapioneer.sloth.crypto

import com.lambdapioneer.sloth.crypto.AuthenticatedEncryption.Companion.decryptUnauthenticated
import com.lambdapioneer.sloth.crypto.AuthenticatedEncryption.Companion.inplaceDecryptEncrypt
import com.lambdapioneer.sloth.utils.secureRandomBytes
import java.lang.Integer.min
import java.math.BigInteger
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * We use CTR mode with an appended HMAC-SHA-256 tag for authenticated encryption. We avoid using
 * GCM to make the implementation of the [decryptUnauthenticated] method easier. It also allows us
 * to re-encrypt the ciphertext in-place in the [inplaceDecryptEncrypt] method.
 */
internal class AuthenticatedEncryption {

    companion object {
        private const val KEY_LEN = 32
        const val TAG_LEN = 16
        internal const val IV_LEN = 16
        internal const val BLOCK_LEN = 16

        private const val ALGORITHM_AES_CTR = "AES/CTR/NoPadding"
        private const val ALGORITHM_HMAC_SHA256 = "HmacSHA256"

        fun keyGen() = secureRandomBytes(KEY_LEN)

        fun ivGen() = secureRandomBytes(IV_LEN)

        /**
         * Slightly different from the paper notation: we return `blob` and `tag` as one consecutive
         * [ByteArray] for a simple implementation.
         */
        fun encrypt(k: ByteArray, iv: ByteArray, data: ByteArray): ByteArray {
            val ciphertextAndTag = ByteArray(data.size + TAG_LEN)
            val tagOffset = data.size

            Cipher.getInstance(ALGORITHM_AES_CTR).run {
                val key = SecretKeySpec(k, "AES")
                init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))
                doFinal(data, 0, data.size, ciphertextAndTag, 0)
            }

            val tag = computeTag(k, ciphertextAndTag, 0, data.size)
            System.arraycopy(tag, 0, ciphertextAndTag, tagOffset, tag.size)

            return ciphertextAndTag
        }

        fun decrypt(k: ByteArray, iv: ByteArray, ciphertextAndTag: ByteArray): ByteArray {
            val offsetTag = ciphertextAndTag.size - TAG_LEN
            verifyTag(k, ciphertextAndTag)

            return Cipher.getInstance(ALGORITHM_AES_CTR).run {
                val key = SecretKeySpec(k, "AES")
                init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))
                doFinal(ciphertextAndTag, 0, offsetTag)
            }
        }

        /**
         * Authenticates the tag only. Throws an [AEADBadTagException] if the tag does not match.
         * Note that the tag does not require the IV value.
         */
        fun authenticate(k: ByteArray, dataAndTag: ByteArray) {
            verifyTag(k, dataAndTag)
        }

        /**
         * Decrypts the given range specified by [offset] and [length] of the [dataAndTag] byte
         * array. This method does not authenticate the decrypted data. This should be done before
         * using this method using [verifyTag].
         *
         * The offset must be aligned at AES block boundaries (16 bytes, see[BLOCK_LEN]).
         */
        fun decryptUnauthenticated(
            k: ByteArray,
            iv: ByteArray,
            dataAndTag: ByteArray,
            offset: Int,
            length: Int,
        ): ByteArray {
            // sanity checks
            val ciphertextLengthWithoutTag = dataAndTag.size - TAG_LEN
            require(offset >= 0)
            require(offset % BLOCK_LEN == 0)
            require(length >= 0)
            require(offset + length <= ciphertextLengthWithoutTag)

            val key = SecretKeySpec(k, "AES")
            val ivSpec = computeAesCtrIvForOffset(iv, offset)
            return Cipher.getInstance(ALGORITHM_AES_CTR).run {
                init(Cipher.DECRYPT_MODE, key, ivSpec)
                doFinal(dataAndTag.copyOfRange(offset, offset + length))
            }
        }

        /**
         * Re-encrypts the given [dataAndTag] byte array in-place
         */
        fun inplaceDecryptEncrypt(
            dataAndTag: ByteArray,
            decryptK: ByteArray,
            decryptIv: ByteArray,
            encryptK: ByteArray,
            encryptIv: ByteArray,
        ) {
            // verify tag of the input
            val offsetTag = dataAndTag.size - TAG_LEN
            verifyTag(decryptK, dataAndTag)

            // prepare ciphers
            val decryptCipher = Cipher.getInstance("AES/CTR/NoPadding")
            decryptCipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(decryptK, "AES"),
                IvParameterSpec(decryptIv)
            )

            val encryptCipher = Cipher.getInstance("AES/CTR/NoPadding")
            encryptCipher.init(
                Cipher.ENCRYPT_MODE,
                SecretKeySpec(encryptK, "AES"),
                IvParameterSpec(encryptIv)
            )

            // perform the re-encryption in a streaming fashion
            val blockSize = 1024
            var inputPos = 0
            var outputPos = 0

            while (inputPos < offsetTag) {
                // read a block of input ciphertext
                val inputBlockEnd = min(offsetTag, inputPos + blockSize)
                val inputBlock = dataAndTag.copyOfRange(inputPos, inputBlockEnd)
                inputPos = inputBlockEnd

                // update the decryption cipher which might produce a block of plaintext
                val perhapsDecryptedBlock = decryptCipher.update(inputBlock) ?: continue

                // in case the decryption has produced output, we update the encryption cipher;
                // in turn this might produce a block of ciphertext
                val perhapsOutputBlock = encryptCipher.update(perhapsDecryptedBlock) ?: continue

                // if the encryption has produced output, we copy it to the output buffer ensuring
                // that we don't overwrite the input buffer
                val l = perhapsOutputBlock.size
                System.arraycopy(perhapsOutputBlock, 0, dataAndTag, outputPos, l)
                check(outputPos <= inputPos)
                outputPos += l
            }

            // we call doFinal on the decryption cipher to ensure that the last block of plaintext
            // is produced
            val finalBlock = encryptCipher.doFinal(decryptCipher.doFinal())
            System.arraycopy(finalBlock, 0, dataAndTag, outputPos, finalBlock.size)
            outputPos += finalBlock.size

            // At this point we should be at the end of the input and output ciphertexts
            check(outputPos == inputPos)
            check(outputPos == dataAndTag.size - TAG_LEN)

            // Replace the tag with a new one
            val newTag = computeTag(encryptK, dataAndTag, 0, outputPos)
            System.arraycopy(newTag, 0, dataAndTag, outputPos, newTag.size)
        }

        private fun computeAesCtrIvForOffset(iv: ByteArray, offset: Int): IvParameterSpec {
            val ivBigInt = BigInteger(iv)
            val blockOffset = offset / BLOCK_LEN
            val offsetIvBigInt = ivBigInt + BigInteger.valueOf(blockOffset.toLong())
            val candidateBytes = offsetIvBigInt.toByteArray()

            val resultIv = ByteArray(IV_LEN)

            if (candidateBytes.size < resultIv.size) {
                // The computed number is smaller than the IV size, so we pad it with zeros
                System.arraycopy(
                    /* src = */ candidateBytes,
                    /* srcPos = */ 0,
                    /* dest = */ resultIv,
                    /* destPos = */ resultIv.size - candidateBytes.size,
                    /* length = */ candidateBytes.size
                )
            } else {
                // The computed number is larger than (or equal to) the IV size, so we take the
                // last AE_IV_LEN bytes only
                System.arraycopy(
                    /* src = */ candidateBytes,
                    /* srcPos = */ candidateBytes.size - resultIv.size,
                    /* dest = */ resultIv,
                    /* destPos = */ 0,
                    /* length = */ resultIv.size
                )
            }

            return IvParameterSpec(resultIv)
        }

        /**
         * Verifies the tag of the given [ciphertextAndTag] byte array.
         * Throws an [AEADBadTagException] if the tag does not match.
         */
        private fun verifyTag(k: ByteArray, ciphertextAndTag: ByteArray) {
            val offsetTag = ciphertextAndTag.size - TAG_LEN
            val tag = ciphertextAndTag.copyOfRange(offsetTag, ciphertextAndTag.size)

            val expectedTag = computeTag(
                k = k,
                input = ciphertextAndTag,
                offset = 0,
                length = offsetTag
            )
            if (!expectedTag.contentEquals(tag)) {
                throw AEADBadTagException("Tag mismatch")
            }
        }

        private fun computeTag(
            k: ByteArray,
            input: ByteArray,
            offset: Int? = null,
            length: Int? = null,
        ): ByteArray {
            return Mac.getInstance(ALGORITHM_HMAC_SHA256).run {
                init(SecretKeySpec(k, ALGORITHM_HMAC_SHA256))
                update(input, offset ?: 0, length ?: input.size)
                doFinal().copyOfRange(0, TAG_LEN)
            }
        }
    }
}


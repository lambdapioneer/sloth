import Foundation
import CryptoKit
import Sodium

public struct AuthenticatedCiphertext {
    public var iv: Bytes
    public var blobAndTag: Bytes

    public init(iv: Bytes, blobAndTag: Bytes) {
        self.iv = iv
        self.blobAndTag = blobAndTag
    }
}

let AuthenticatedEncryptionNonceLength = 12 // 128 bit
let AuthenticatedEncryptionKeyLength = 32 // 256 bit
let AuthenticatedEncryptionTagLength = 16 // 196 bit

/// A simple AEAD wrapper around CryptoKit's `AES.GCM` cipher. This turned out to be the fastest, easily-accessible AEAD implementation after comparision
/// with both CryptoKit's `ChaChaPoly` and LibSodium's `SealedBox` which both were 2-3x slower.
enum AuthenticatedEncryption {

    internal static func encrypt(k: Bytes, iv: Bytes, data: Bytes) throws -> AuthenticatedCiphertext {
        let result = try AES.GCM.seal(data, using: SymmetricKey(data: k), nonce: try AES.GCM.Nonce(data: iv))
        let blobAndTag = Bytes(result.ciphertext) + Bytes(result.tag)
        return AuthenticatedCiphertext(iv: iv, blobAndTag: blobAndTag)
    }

    internal static func decrypt(k: Bytes, ciphertext: AuthenticatedCiphertext) throws -> Bytes {
        let tagOffset = Int(ciphertext.blobAndTag.count - AuthenticatedEncryptionTagLength)
        let blob = ciphertext.blobAndTag[0..<tagOffset]
        let tag = ciphertext.blobAndTag[tagOffset..<ciphertext.blobAndTag.count]

        let sealedBox = try AES.GCM.SealedBox(nonce: try AES.GCM.Nonce(data: ciphertext.iv), ciphertext: blob, tag: tag)
        guard let result = try? AES.GCM.open(sealedBox, using: SymmetricKey(data: k)) else {
            throw SlothError.failedToDecryptAuthenticatedCiphertext
        }
        return Bytes(result)
    }

    /// Returns a random 12 byte nonce.
    internal static func generateRandomIV() throws -> Bytes {
        guard let nonceBytes = Sodium().randomBytes.buf(length: AuthenticatedEncryptionNonceLength) else {
            throw SlothError.failedToGenerateRandomBytes
        }
        return nonceBytes
    }

    /// Returns a random 32 byte key.
    internal static func generateRandomKey() throws -> Bytes {
        guard let nonceBytes = Sodium().randomBytes.buf(length: AuthenticatedEncryptionKeyLength) else {
            throw SlothError.failedToGenerateRandomBytes
        }
        return nonceBytes
    }
}

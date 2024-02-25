@testable import LibSloth
import XCTest

/// These tests must live in a target of an app to get the right entitlements to access the key chain / SlothSecureEnclave.
final class SlothSecureEnclaveTests: XCTestCase {

    func testKeyGenAndEcdh() throws {
        // generate a new private key
        try SlothSecureEnclave.resetSecretSeKey(handle: "test")
        let secretKey = try SlothSecureEnclave.loadSecretSeKey(handle: "test")

        // using the same public key should result in the same output
        let publicKey1 = try SlothSecureEnclave.generateRandomPublicKey()
        let a1 = try SlothSecureEnclave.runECDH(secretKey: secretKey, pubKey: publicKey1)
        let a2 = try SlothSecureEnclave.runECDH(secretKey: secretKey, pubKey: publicKey1)
        XCTAssertEqual(a1, a2)

        // using different public keys should result in different outputs
        let publicKey2 = try SlothSecureEnclave.generateRandomPublicKey()
        let b1 = try SlothSecureEnclave.runECDH(secretKey: secretKey, pubKey: publicKey1)
        let b2 = try SlothSecureEnclave.runECDH(secretKey: secretKey, pubKey: publicKey2)
        XCTAssertNotEqual(b1, b2)

        // resetting the secret key should result in a different output
        try SlothSecureEnclave.resetSecretSeKey(handle: "test")
        let secretKey2 = try SlothSecureEnclave.loadSecretSeKey(handle: "test")
        XCTAssertNotEqual(secretKey, secretKey2)
        let c1 = try SlothSecureEnclave.runECDH(secretKey: secretKey2, pubKey: publicKey1)
        XCTAssertNotEqual(a1, c1)
    }

    func testAesGcmEncryptDecryptHappyPath() throws {
        let message = Bytes("message".utf8)

        // encrypt and decrypt using the same key handle should result in the same text
        let ciphertext = try SlothSecureEnclave.aesGcmEncrypt(handle: "test", data: message)
        let actual = try SlothSecureEnclave.aesGcmDecrypt(handle: "test", ciphertext: ciphertext)
        XCTAssertEqual(message, actual)
    }

    func testAesGcmEncryptDecryptFailsIfCiphertextChanged() throws {
        let message = Bytes("message".utf8)

        var ciphertext = try SlothSecureEnclave.aesGcmEncrypt(handle: "test", data: message)

        // decrypting of a modified ciphertext should fail
        ciphertext[0] ^= 0x01

        XCTAssertThrowsError(try SlothSecureEnclave.aesGcmDecrypt(handle: "test", ciphertext: ciphertext)) { (error) in
            XCTAssertEqual(error as? SlothError, SlothError.failedToDecryptAuthenticatedCiphertext)
        }
    }

    func testAesGcmEncryptDecryptFailsIfSecretKeyReset() throws {
        let message = Bytes("message".utf8)

        let ciphertext = try SlothSecureEnclave.aesGcmEncrypt(handle: "test", data: message)

        try SlothSecureEnclave.resetSecretSeKey(handle: "test")
        _ = try SlothSecureEnclave.aesGcmEncrypt(handle: "test", data: message)

        // as a result the previous ciphertext should no longer decrypt
        XCTAssertThrowsError(try SlothSecureEnclave.aesGcmDecrypt(handle: "test", ciphertext: ciphertext)) { (error) in
            XCTAssertEqual(error as? SlothError, SlothError.failedToDecryptAuthenticatedCiphertext)
        }
    }

    func testExportAndImportPublicKey() throws {
        for _ in 0..<10 {
            let pubKey = try SlothSecureEnclave.generateRandomPublicKey()
            let bytes = try SlothSecureEnclave.exportPublicKey(pubKey: pubKey)
            let reimportedKey = try SlothSecureEnclave.importPublicKey(bytes: bytes)

            XCTAssertEqual(pubKey, reimportedKey)
        }
    }
}

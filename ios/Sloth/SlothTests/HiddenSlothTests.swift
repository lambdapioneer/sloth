@testable import LibSloth
import XCTest

/// These tests must live in a target of an app to get the right entitlements to access the key chain / SecureEnclave.
final class HiddenSlothTests: XCTestCase {

    func testInitReturnsValidStorageState() throws {
        let hiddenSloth = HiddenSloth(withRainbowSloth: RainbowSloth(withN: 10), maxSize: 1024)

        let storageState = try hiddenSloth.initializeStorage(handle: "test")
        XCTAssertFalse(storageState.ciphertext.isEmpty)
        XCTAssertFalse(storageState.handle.isEmpty)
        XCTAssertFalse(storageState.wrappedData.wrappedIv.isEmpty)
        XCTAssertFalse(storageState.wrappedData.wrappedTk.isEmpty)
        XCTAssertFalse(storageState.wrappedData.wrappedTiv.isEmpty)
    }

    func testEncryptDecryptHappyPath() throws {
        let hiddenSloth = HiddenSloth(withRainbowSloth: RainbowSloth(withN: 10), maxSize: 1024)
        var storage = try hiddenSloth.initializeStorage(handle: "test")

        let data = Bytes("message".utf8)
        let pw = "password"

        storage = try hiddenSloth.encrypt(state: storage, pw: pw, data: data)

        let actual = try hiddenSloth.decrypt(state: storage, pw: pw)
        XCTAssertEqual(actual, data)
    }

    func testEncryptDecryptFailsWhenDifferentPasswort() throws {
        let hiddenSloth = HiddenSloth(withRainbowSloth: RainbowSloth(withN: 10), maxSize: 1024)
        var storage = try hiddenSloth.initializeStorage(handle: "test")

        storage = try hiddenSloth.encrypt(state: storage, pw: "password", data: Bytes("message".utf8))

        // decrypting with the wrong passphrase should throw
        XCTAssertThrowsError(try hiddenSloth.decrypt(state: storage, pw: "different password")) { (error) in
            XCTAssertEqual(error as? SlothError, SlothError.failedToDecryptAuthenticatedCiphertext)
        }
    }

    func testEncryptDecryptFailsWhenCiphertextChanged() throws {
        let hiddenSloth = HiddenSloth(withRainbowSloth: RainbowSloth(withN: 10), maxSize: 1024)
        var storage = try hiddenSloth.initializeStorage(handle: "test")

        let pw = "password"
        storage = try hiddenSloth.encrypt(state: storage, pw: pw, data: Bytes("message".utf8))

        // we change a bit in the ciphertext which should cause the outer decryption to fail
        storage.ciphertext[0] ^= 0x01

        XCTAssertThrowsError(try hiddenSloth.decrypt(state: storage, pw: pw)) { (error) in
            XCTAssertEqual(error as? SlothError, SlothError.failedToDecryptAuthenticatedCiphertext)
        }
    }

    func testRatchetHappyPath() throws {
        let hiddenSloth = HiddenSloth(withRainbowSloth: RainbowSloth(withN: 10), maxSize: 1024)
        let initialStorage = try hiddenSloth.initializeStorage(handle: "test")

        let data = Bytes("message".utf8)
        let pw = "password"

        // performing our encryption should change all cryptographic fields
        let encryptedStorage = try hiddenSloth.encrypt(state: initialStorage, pw: pw, data: data)
        XCTAssertNotEqual(initialStorage.ciphertext, encryptedStorage.ciphertext)
        XCTAssertNotEqual(initialStorage.wrappedData.wrappedIv, encryptedStorage.wrappedData.wrappedIv)
        XCTAssertNotEqual(initialStorage.wrappedData.wrappedTk, encryptedStorage.wrappedData.wrappedTk)
        XCTAssertNotEqual(initialStorage.wrappedData.wrappedTiv, encryptedStorage.wrappedData.wrappedTiv)

        // likewise, ratcheting should change all cryptographic fields
        let rachetedStorage = try hiddenSloth.ratchet(state: encryptedStorage)
        XCTAssertNotEqual(encryptedStorage.ciphertext, rachetedStorage.ciphertext)
        XCTAssertNotEqual(encryptedStorage.wrappedData.wrappedIv, rachetedStorage.wrappedData.wrappedIv)
        XCTAssertNotEqual(encryptedStorage.wrappedData.wrappedTk, rachetedStorage.wrappedData.wrappedTk)
        XCTAssertNotEqual(encryptedStorage.wrappedData.wrappedTiv, rachetedStorage.wrappedData.wrappedTiv)

        let actual = try hiddenSloth.decrypt(state: rachetedStorage, pw: pw)
        XCTAssertEqual(actual, data)
    }

    func testRatchetFailsWhenCiphertextChanged() throws {
        let hiddenSloth = HiddenSloth(withRainbowSloth: RainbowSloth(withN: 10), maxSize: 1024)
        var storage = try hiddenSloth.initializeStorage(handle: "test")

        // ratcheting an initial storage should run without problems
        storage = try hiddenSloth.ratchet(state: storage)
        storage = try hiddenSloth.ratchet(state: storage)

        // ratcheting after the ciphertext has been mangled with should throw, as the AES-GCM tag is no longer valid
        storage.ciphertext[0] ^= 0x01

        XCTAssertThrowsError(try hiddenSloth.ratchet(state: storage)) { (error) in
            XCTAssertEqual(error as? SlothError, SlothError.failedToDecryptAuthenticatedCiphertext)
        }
    }

    func testInnerEncryptInnerDecryptHappyPath() throws {
        let hiddenSloth = HiddenSloth(withRainbowSloth: RainbowSloth(withN: 10), maxSize: 1024)

        let k = try AuthenticatedEncryption.generateRandomKey()
        let data = Bytes("test".utf8)
        let innerCiphertext = try hiddenSloth.innerEncrypt(k: k, data: data)

        let actual = try hiddenSloth.innerDecrypt(k: k, ciphertext: innerCiphertext)
        XCTAssertEqual(actual, data)
    }

    // we test this one separately, as changing the outer ciphertext fails the wrapping AES-GCM before
    // actually reaching the inner decryption
    func testInnerEncryptInnerDecryptFailsWhenCiphertextChanged() throws {
        let hiddenSloth = HiddenSloth(withRainbowSloth: RainbowSloth(withN: 10), maxSize: 1024)

        let k = try AuthenticatedEncryption.generateRandomKey()
        let data = Bytes("test".utf8)
        var innerCiphertext = try hiddenSloth.innerEncrypt(k: k, data: data)

        // we change a bit in the ciphertext which should cause the inner decryption to fail
        innerCiphertext.blobAndTag[0] ^= 0x01

        XCTAssertThrowsError(try AuthenticatedEncryption.decrypt(k: k, ciphertext: innerCiphertext)) { (error) in
            XCTAssertEqual(error as? SlothError, SlothError.failedToDecryptAuthenticatedCiphertext)
        }
    }

    func testGenerateRandomPassword() throws {
        let pw1 = try HiddenSloth.generateRandomPassword(outputLength: 32)
        XCTAssertEqual(pw1.count, 32)

        let pw2 = try HiddenSloth.generateRandomPassword(outputLength: 32)
        XCTAssertNotEqual(pw1, pw2)
    }
}

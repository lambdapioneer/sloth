@testable import LibSloth
import XCTest

final class AuthenticatedEncryptionTests: XCTestCase {

    func testKeyGenerationMethods() throws {
        let k1 = try AuthenticatedEncryption.generateRandomKey()
        XCTAssertEqual(k1.count, 32)
        let k2 = try AuthenticatedEncryption.generateRandomKey()
        XCTAssertNotEqual(k1, k2)
    }

    func testIVGenerationMethods() throws {
        let n1 = try AuthenticatedEncryption.generateRandomIV()
        XCTAssertEqual(n1.count, 12)
        let n2 = try AuthenticatedEncryption.generateRandomIV()
        XCTAssertNotEqual(n1, n2)
    }

    func testEncryptDecryptHappyPath() throws {
        let k = try AuthenticatedEncryption.generateRandomKey()
        let iv = try AuthenticatedEncryption.generateRandomIV()
        let data = try Bytes(hex: "42")

        // encrypt then decrypt
        let ciphertext = try AuthenticatedEncryption.encrypt(k: k, iv: iv, data: data)
        let actual = try AuthenticatedEncryption.decrypt(k: k, ciphertext: ciphertext)

        XCTAssertEqual(data, actual)
    }

    func testEncryptDecryptFailsWhenByteFlipped() throws {
        let k = try AuthenticatedEncryption.generateRandomKey()
        let iv = try AuthenticatedEncryption.generateRandomIV()
        let data = try Bytes(hex: "42")

        // encrypt
        var ciphertext = try AuthenticatedEncryption.encrypt(k: k, iv: iv, data: data)

        // flip a bit in the first byte of the ciphertext
        ciphertext.blobAndTag[0] ^= 0x01

        // trying to decrypt bad ciphertext will throw
        XCTAssertThrowsError(try AuthenticatedEncryption.decrypt(k: k, ciphertext: ciphertext)) { (error) in
            XCTAssertEqual(error as? SlothError, SlothError.failedToDecryptAuthenticatedCiphertext)
        }
    }
}

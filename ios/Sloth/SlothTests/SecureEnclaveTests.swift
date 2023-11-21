@testable import RainbowSloth
import XCTest

/// These tests must live in a target of an app to get the right entitlements to access the key chain / SecureEnclave.
final class SecureEnclaveTests: XCTestCase {

    func testKeyGenAndEcdh() throws {
        // generate a new private key
        try! SecureEnclave.resetSecretSeKey()
        let secretKey = try! SecureEnclave.loadSecretSeKey()

        // using the same public key should result in the same output
        let publicKey1 = try! SecureEnclave.genRandomPublicKey()
        let a1 = try! SecureEnclave.runECDH(secretKey: secretKey, pubKey: publicKey1)
        let a2 = try! SecureEnclave.runECDH(secretKey: secretKey, pubKey: publicKey1)
        XCTAssertEqual(a1, a2)

        // using different public keys should result in different outputs
        let publicKey2 = try! SecureEnclave.genRandomPublicKey()
        let b1 = try! SecureEnclave.runECDH(secretKey: secretKey, pubKey: publicKey1)
        let b2 = try! SecureEnclave.runECDH(secretKey: secretKey, pubKey: publicKey2)
        XCTAssertNotEqual(b1, b2)

        // resetting the secret key should result in a different output
        try! SecureEnclave.resetSecretSeKey()
        let secretKey2 = try! SecureEnclave.loadSecretSeKey()
        XCTAssertNotEqual(secretKey, secretKey2)
        let c1 = try! SecureEnclave.runECDH(secretKey: secretKey2, pubKey: publicKey1)
        XCTAssertNotEqual(a1, c1)
    }

    func testExportAndImportPublicKey() throws {
        for _ in 0..<10 {
            let pubKey = try! SecureEnclave.genRandomPublicKey()
            let data = try! SecureEnclave.exportPublicKey(pubKey: pubKey) as Data
            let reimportedKey = try! SecureEnclave.importPublicKey(keyData: data as CFData)

            XCTAssertEqual(pubKey, reimportedKey)
        }
    }
}

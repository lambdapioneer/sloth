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

    func testExportAndImportPublicKey() throws {
        for _ in 0..<10 {
            let pubKey = try SlothSecureEnclave.generateRandomPublicKey()
            let bytes = try SlothSecureEnclave.exportPublicKey(pubKey: pubKey)
            let reimportedKey = try SlothSecureEnclave.importPublicKey(bytes: bytes)

            XCTAssertEqual(pubKey, reimportedKey)
        }
    }
}

@testable import LibSloth
import XCTest

final class HkdfTests: XCTestCase {

    func testHexConversion() throws {
        let s = "abcdef0123456789"
        let d = try Bytes(hex: s)
        let s2 = try d.toHex()
        XCTAssertEqual(s, s2)
    }

    /// "Basic test case with SHA-256"
    func testRfc5869TestCase1() throws {
        let ikm = try Bytes(hex: "0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b")
        let salt = try Bytes(hex: "000102030405060708090a0b0c")
        let info = try Bytes(hex: "f0f1f2f3f4f5f6f7f8f9")
        let l = 42

        let actual = HkdfSha256.derive(salt: salt, ikm: ikm, info: info, outputLength: l)
        let expected = try Bytes(hex: "3cb25f25faacd57a90434f64d0362f2a2d2d0a90cf1a5a4c5db02d56ecc4c5bf34007208d5b887185865")

        XCTAssertEqual(actual, expected)
    }

    /// "Test with SHA-256 and longer inputs/outputs"
    func testRfc5869TestCase2() throws {
        let ikm = try Bytes(hex: "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f202122232425262728292a2b2c2d2e2f303132333435363738393a3b3c3d3e3f404142434445464748494a4b4c4d4e4f")
        let salt = try Bytes(hex: "606162636465666768696a6b6c6d6e6f707172737475767778797a7b7c7d7e7f808182838485868788898a8b8c8d8e8f909192939495969798999a9b9c9d9e9fa0a1a2a3a4a5a6a7a8a9aaabacadaeaf")
        let info = try Bytes(hex: "b0b1b2b3b4b5b6b7b8b9babbbcbdbebfc0c1c2c3c4c5c6c7c8c9cacbcccdcecfd0d1d2d3d4d5d6d7d8d9dadbdcdddedfe0e1e2e3e4e5e6e7e8e9eaebecedeeeff0f1f2f3f4f5f6f7f8f9fafbfcfdfeff")
        let l = 82

        let actual = HkdfSha256.derive(salt: salt, ikm: ikm, info: info, outputLength: l)
        let expected = try Bytes(hex: "b11e398dc80327a1c8e7f78c596a49344f012eda2d4efad8a050cc4c19afa97c59045a99cac7827271cb41c65e590e09da3275600c2f09b8367793a9aca3db71cc30c58179ec3e87c14c01d5c1f3434f1d87")

        XCTAssertEqual(actual, expected)
    }

    /// "Test with SHA-256 and zero-length salt/info"
    func testRfc5869TestCase3() throws {
        let ikm = try Bytes(hex: "0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b")
        let salt = try Bytes(hex: "")
        let info = try Bytes(hex: "")
        let l = 42

        let actual = HkdfSha256.derive(salt: salt, ikm: ikm, info: info, outputLength: l)
        let expected = try Bytes(hex: "8da4e775a563c18f715f802a063c5a31b8a11f5c5ee1879ec3454e5f3c738d2d9d201395faa4b61a96c8")

        XCTAssertEqual(actual, expected)
    }
}

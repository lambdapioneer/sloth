import XCTest

final class HkdfTests: XCTestCase {

    func testHexConversion() throws {
        let s = "abcdef0123456789"
        let d = Data(hex: s)
        let s2 = d.toHex
        XCTAssertEqual(s, s2)
    }

    func testRfc5869TestCase1() throws {
        let ikm = Data(hex: "0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b")
        let salt = Data(hex: "000102030405060708090a0b0c")
        let info = Data(hex: "f0f1f2f3f4f5f6f7f8f9")
        let l = 42

        let actual = HkdfSha256.derive(salt: salt, ikm: ikm, info: info, l: l)
        let expected = Data(hex: "3cb25f25faacd57a90434f64d0362f2a2d2d0a90cf1a5a4c5db02d56ecc4c5bf34007208d5b887185865")

        XCTAssertEqual(actual, expected)
    }

}

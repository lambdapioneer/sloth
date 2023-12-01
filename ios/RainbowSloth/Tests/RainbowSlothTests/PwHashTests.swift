@testable import RainbowSloth
import XCTest

final class PwHashTests: XCTestCase {
    func testPwHashOutputLengthMatches() throws {
        // Its less bad to have force unwraps in tests,
        // but if these were running in CI and failed, your test suite would hang
        let pw = Bytes("test".utf8)
        let salt = try Bytes(hex: "0123456789ABCDEF0123456789ABCDEF")

        let res = try PwHash.derive(salt: salt, pw: pw, outputLength: 42)

        XCTAssertEqual(res.count, 42)
    }

    func testPwHashDifferentPasswordsDifferentOutput() throws {
        let pw1 = Bytes("test".utf8)
        let pw2 = Bytes("test2".utf8)
        let salt = try Bytes(hex: "0123456789ABCDEF0123456789ABCDEF")

        let res1 = try PwHash.derive(salt: salt, pw: pw1, outputLength: 32)
        let res2 = try PwHash.derive(salt: salt, pw: pw2, outputLength: 32)

        XCTAssertNotEqual(res1, res2)
    }

    func testPwHashDifferentSaltsDifferentOutput() throws {
        let pw = Bytes("test".utf8)
        let salt1 = try Bytes(hex: "0123456789ABCDEF0123456789ABCDEF")
        let salt2 = try Bytes(hex: "F123456789ABCDEF0123456789ABCDEF")

        let res1 = try PwHash.derive(salt: salt1, pw: pw, outputLength: 32)
        let res2 = try PwHash.derive(salt: salt2, pw: pw, outputLength: 32)

        XCTAssertNotEqual(res1, res2)
    }

    func testPwHashSameParametersSameOutput() throws {
        let pw = Bytes("test".utf8)
        let salt = try Bytes(hex: "0123456789ABCDEF0123456789ABCDEF")

        let res1 = try PwHash.derive(salt: salt, pw: pw, outputLength: 32)
        let res2 = try PwHash.derive(salt: salt, pw: pw, outputLength: 32)

        XCTAssertEqual(res1, res2)
    }
}

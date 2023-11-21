@testable import RainbowSloth
import XCTest

final class PwHashTests: XCTestCase {
    
    func testPwHashOutputLengthMatches() throws {
        let pw = "test".data(using: .utf8)!
        let salt = Data(hex: "0123456789ABCDEF0123456789ABCDEF")

        let res = PwHash.derive(salt: salt, pw: pw, outputLength: 42)

        XCTAssertEqual(res.count, 42)
    }
    
    func testPwHashDifferentPasswordsDifferentOutput() throws {
        let pw1 = "test".data(using: .utf8)!
        let pw2 = "test2".data(using: .utf8)!
        let salt = Data(hex: "0123456789ABCDEF0123456789ABCDEF")

        let res1 = PwHash.derive(salt: salt, pw: pw1, outputLength: 32)
        let res2 = PwHash.derive(salt: salt, pw: pw2, outputLength: 32)

        XCTAssertNotEqual(res1, res2)
    }
    
    func testPwHashDifferentSaltsDifferentOutput() throws {
        let pw = "test".data(using: .utf8)!
        let salt1 = Data(hex: "0123456789ABCDEF0123456789ABCDEF")
        let salt2 = Data(hex: "F123456789ABCDEF0123456789ABCDEF")

        let res1 = PwHash.derive(salt: salt1, pw: pw, outputLength: 32)
        let res2 = PwHash.derive(salt: salt2, pw: pw, outputLength: 32)

        XCTAssertNotEqual(res1, res2)
    }
    
    func testPwHashSameParametersSameOutput() throws {
        let pw = "test".data(using: .utf8)!
        let salt = Data(hex: "0123456789ABCDEF0123456789ABCDEF")

        let res1 = PwHash.derive(salt: salt, pw: pw, outputLength: 32)
        let res2 = PwHash.derive(salt: salt, pw: pw, outputLength: 32)

        XCTAssertEqual(res1, res2)
    }
}

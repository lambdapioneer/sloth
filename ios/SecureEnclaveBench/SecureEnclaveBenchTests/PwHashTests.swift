import XCTest

final class PwHashTests: XCTestCase {

    func testArgon2OutputLengthMatches() throws {
        let pw = "test"
        let salt = Data(hex: "0123456789ABCDEF0123456789ABCDEF")

        let res = PwHash.derive(salt: salt, pw: pw, l: 42)

        XCTAssertEqual(res.count, 42)
    }
}

import RainbowSloth
import XCTest

/// These tests must live in a target of an app to get the right entitlements to access the key chain / SecureEnclave.
final class RainbowSlothTests: XCTestCase {

    func testWorksWithLargeN() throws {
        let sloth = RainbowSloth(withN: 10)

        let (_, kOriginal) = try sloth.keygen(
            pw: "test",
            handle: "default",
            outputLength: 32
        )
        XCTAssertEqual(kOriginal.count, 32)
    }

    func testDeriveGeneratesSameKeyAsKeyGen() throws {
        let sloth = RainbowSloth(withN: 1)

        let (storageState, kOriginal) = try sloth.keygen(
            pw: "test",
            handle: "default",
            outputLength: 32
        )
        XCTAssertEqual(kOriginal.count, 32)

        let kDerived = try sloth.derive(
            storageState: storageState,
            pw: "test",
            outputLength: 32
        )
        XCTAssertEqual(kOriginal, kDerived)
    }

    func testDifferentPasswortResultsInDifferentKey() throws {
        let sloth = RainbowSloth(withN: 1)

        let (storageState, kOriginal) = try sloth.keygen(
            pw: "test",
            handle: "default",
            outputLength: 32
        )
        XCTAssertEqual(kOriginal.count, 32)

        let kDerived = try sloth.derive(
            storageState: storageState,
            pw: "differentPassword",
            outputLength: 32
        )
        XCTAssertNotEqual(kOriginal, kDerived)
    }

    func testAnotherKeyGenResultsInNewKey() throws {
        let sloth = RainbowSloth(withN: 1)

        let (storageState, kOriginal) = try sloth.keygen(
            pw: "test",
            handle: "default",
            outputLength: 32
        )
        XCTAssertEqual(kOriginal.count, 32)

        let _ = try sloth.keygen(
            pw: "test",
            handle: "default",
            outputLength: 32
        )

        let kDerived = try sloth.derive(
            storageState: storageState,
            pw: "test",
            outputLength: 32
        )
        XCTAssertNotEqual(kOriginal, kDerived)
    }

    func testAnotherKeyGenWithDifferentHandleIsIndependent() throws {
        let sloth = RainbowSloth(withN: 1)

        let (storageState, kOriginal) = try sloth.keygen(
            pw: "test",
            handle: "default",
            outputLength: 32
        )
        XCTAssertEqual(kOriginal.count, 32)

        let (_, kOther) = try sloth.keygen(
            pw: "test",
            handle: "differentHandle",
            outputLength: 32
        )
        XCTAssertNotEqual(kOriginal, kOther)

        let kDerived = try sloth.derive(
            storageState: storageState,
            pw: "test",
            outputLength: 32
        )
        XCTAssertEqual(kOriginal, kDerived)
    }
}

import RainbowSloth
import XCTest

/// These tests must live in a target of an app to get the right entitlements to access the key chain / SecureEnclave.
final class RainbowSlothTests: XCTestCase {

    func testDeriveGeneratesSameKeyAsKeyGen() throws {
        let sloth = RainbowSloth(withN: 10)

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
        let sloth = RainbowSloth(withN: 10)

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

    func testAnotherKeyGenUnderSameHandleResultsInKeyChange() throws {
        let sloth = RainbowSloth(withN: 10)

        let (storageState, kOriginal) = try sloth.keygen(
            pw: "test",
            handle: "default",
            outputLength: 32
        )
        XCTAssertEqual(kOriginal.count, 32)

        // note that we do not update the storage, but
        // using the same handle will change the key inside the SE
        _ = try sloth.keygen(
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
        let sloth = RainbowSloth(withN: 10)

        let (storageStateOriginal, kOriginal) = try sloth.keygen(
            pw: "sloth",
            handle: "original",
            outputLength: 32
        )
        XCTAssertEqual(kOriginal.count, 32)

        let (storageStateOther, kOther) = try sloth.keygen(
            pw: "penguin",
            handle: "other",
            outputLength: 32
        )
        XCTAssertNotEqual(kOriginal, kOther)

        let kDerivedOriginal = try sloth.derive(
            storageState: storageStateOriginal,
            pw: "sloth",
            outputLength: 32
        )
        XCTAssertEqual(kOriginal, kDerivedOriginal)

        let kDerivedOther = try sloth.derive(
            storageState: storageStateOther,
            pw: "penguin",
            outputLength: 32
        )
        XCTAssertEqual(kOther, kDerivedOther)
    }

    func testWorksWithVeryLargeN() throws {
        let sloth = RainbowSloth(withN: 1000)

        let (_, kOriginal) = try sloth.keygen(
            pw: "test",
            handle: "default",
            outputLength: 32
        )
        XCTAssertEqual(kOriginal.count, 32)
    }

    func testUsingLargerParameterResultsInLongerRuntime() throws {
        let slothFast = RainbowSloth(withN: 10)
        let slothSlow = RainbowSloth(withN: 100)
        let iterations = 10

        let fastTimings = try RainbowSlothEvaluationWrapper.runEval(sloth: slothFast, iterations: iterations)
        let slowTimings = try RainbowSlothEvaluationWrapper.runEval(sloth: slothSlow, iterations: iterations)

        let fastAverage = fastTimings.reduce(0.0, +) / 5
        let slowAverage = slowTimings.reduce(0.0, +) / 5
        XCTAssertGreaterThan(slowAverage, fastAverage)

        // from observations the slowDown factor is higher and more consistent on real devices
        // compared to simulators
        let slowDown = slowAverage / fastAverage
        XCTAssertGreaterThan(slowDown, 5)
        XCTAssertLessThan(slowDown, 15)
    }
}

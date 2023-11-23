@testable import RainbowSloth
import XCTest

final class ReHashToEcTests: XCTestCase {

    func testRehashSameSeedSameOutput() throws {
        let seed = "sloth"

        let a1 = ReHashToEc.rehashToP256(seed: seed.data(using: .ascii)!)
        let a2 = ReHashToEc.rehashToP256(seed: seed.data(using: .ascii)!)

        XCTAssertEqual(a1, a2)
    }

    func testRehashDifferentSeedDifferentOutput() throws {
        let seed = "sloth"
        let differentSeed = "penguin"

        let b1 = ReHashToEc.rehashToP256(seed: seed.data(using: .ascii)!)
        let b2 = ReHashToEc.rehashToP256(seed: differentSeed.data(using: .ascii)!)

        XCTAssertNotEqual(b1, b2)
    }
}

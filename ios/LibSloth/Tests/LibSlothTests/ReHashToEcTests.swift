@testable import LibSloth
import XCTest

final class ReHashToEcTests: XCTestCase {

    func testRehashSameSeedSameOutput() throws {
        let seed = "sloth"

        let a1 = ReHashToEc.rehashToP256(seed: Bytes(seed.utf8))
        let a2 = ReHashToEc.rehashToP256(seed: Bytes(seed.utf8))

        XCTAssertEqual(a1, a2)
    }

    func testRehashDifferentSeedDifferentOutput() throws {
        let seed = "sloth"
        let differentSeed = "penguin"

        let b1 = ReHashToEc.rehashToP256(seed: Bytes(seed.utf8))
        let b2 = ReHashToEc.rehashToP256(seed: Bytes(differentSeed.utf8))

        XCTAssertNotEqual(b1, b2)
    }
}

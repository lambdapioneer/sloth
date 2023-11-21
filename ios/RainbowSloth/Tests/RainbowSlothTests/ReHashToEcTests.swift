@testable import RainbowSloth
import XCTest

final class ReHashToEcTests: XCTestCase {

    func testRehash() throws {
        let seed = "1337"

        // from same seed results in same public keys
        let a1 = ReHashToEc.rehashToP256(seed: seed.data(using: .ascii)!)
        let a2 = ReHashToEc.rehashToP256(seed: seed.data(using: .ascii)!)
        XCTAssertEqual(a1, a2)

        // from different seed results in different seed
        let differentSeed = "penguin"
        let b1 = ReHashToEc.rehashToP256(seed: seed.data(using: .ascii)!)
        let b2 = ReHashToEc.rehashToP256(seed: differentSeed.data(using: .ascii)!)
        XCTAssertNotEqual(b1, b2)
    }
}

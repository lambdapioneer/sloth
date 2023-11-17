import Argon2Swift
import Foundation

public struct PwHash {
    public static func derive(salt: Data, pw: String, l: Int) -> Data {
        // OWASP: "Use Argon2id with a minimum configuration of 19 MiB of memory, an iteration count of 2, and 1 degree of parallelism."
        let res = try! Argon2Swift.hashPasswordString(
            password: pw,
            salt: Salt(bytes: salt),
            iterations: 2,
            memory: 19*1024, // 19 MiB
            parallelism: 1,
            length: l,
            type: Argon2Type.id
        )

        return res.hashData()
    }
}

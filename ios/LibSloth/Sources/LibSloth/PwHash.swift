import Foundation
import Sodium

/// Wrapper around the Argon2id password hashing algorithm as provided by `Sodium`.
enum PwHash {

    /// Derives a key from the given `salt` and password `pw`. The output will be `outputLength` bytes long.
    static func derive(salt: Bytes, pw: Bytes, outputLength: Int) throws -> Bytes {
        // OWASP: "Use Argon2id with a minimum configuration of 19 MiB of memory, an iteration count of 2, and 1 degree of parallelism."
        guard let res = Sodium().pwHash.hash(
            outputLength: outputLength,
            passwd: Array(pw),
            salt: Array(salt),
            opsLimit: 2,
            memLimit: 19*1024*1024 // 19 MiB
        ) else {
            throw SlothError.failedToDeriveArgon2Hash
        }
        return res
    }

    /// Creates a new random `salt` byte array that can be used with the `derive` function.
    static func generateRandomSalt(outputLength: Int = 16) throws -> Bytes {
        guard let saltBytes = Sodium().randomBytes.buf(length: outputLength) else {
            throw SlothError.failedToGenerateRandomBytes
        }
        return saltBytes
    }
}

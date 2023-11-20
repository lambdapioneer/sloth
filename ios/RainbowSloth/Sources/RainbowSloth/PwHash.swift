import Sodium
import Foundation

/// Wrapper around the Argon2id password hashing algorithm as provided by `Sodium`.
public struct PwHash {
    
    /// Derives a key from the given `salt` and password `pw`. The output will be `outputLength` bytes long.
    public static func derive(salt: Data, pw: Data, outputLength: Int) -> Data {
        // OWASP: "Use Argon2id with a minimum configuration of 19 MiB of memory, an iteration count of 2, and 1 degree of parallelism."
        let sodium = Sodium.init()
        let sodiumPwHash = sodium.pwHash
        let res =  sodiumPwHash.hash(
            outputLength: outputLength,
            passwd: Array(pw),
            salt: Array(salt),
            opsLimit: 2,
            memLimit: 19*1024*1024 // 19 MiB
        )
        return Data(res!)
    }
    
    /// Creates a new random `salt` byte array that can be used with the `derive` function.
    public static func randomSalt(outputLength: Int = 16) -> Data {
        var bytes = [UInt8](repeating: 0, count: outputLength)
        let result = SecRandomCopyBytes(kSecRandomDefault, bytes.count, &bytes)
        assert(result == errSecSuccess, "Failed to generate random bytes")
        return Data(bytes)
    }
}

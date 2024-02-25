import Foundation

private let P256_PUBLIC_KEY_SIZE = 32

/// The required data to be persisted for later re-deriving the same key using `RainbowSloth`.
public struct RainbowSlothStorageState {
    public var handle: String
    public var salt: Bytes

    public init(handle: String, salt: Bytes) {
        self.handle = handle
        self.salt = salt
    }
}

/// Implementation of the RainbowSloth algorithm for tuneable password-based key stretching on iOS using the SecureEnclave (SE).
/// The parameter `n` determines the number of required SE operations and can be increased for higher security levels.
/// Refer to the paper and documentation for more details.
public struct RainbowSloth {
    var n: Int

    public init(withN n: Int) {
        self.n = n
    }

    /// Generates a new key under the handle `handle` using the password `pw`. The output will be `outputLength` bytes long.
    /// Every call to this function will result in a new key and any existing key under the same handle will be overwritten.
    /// Calls to `derive` using the returned storage state will result in the same key.
    public func keygen(pw: String, handle: String, outputLength: Int) throws -> (RainbowSlothStorageState, Bytes) {
        let storageState = try RainbowSlothStorageState(
            handle: handle,
            salt: PwHash.generateRandomSalt()
        )

        try SlothSecureEnclave.resetSecretSeKey(handle: storageState.handle)

        let k = try derive(storageState: storageState, pw: pw, outputLength: outputLength)

        return (storageState, k)
    }

    /// Re-derives a key from the given storage state. The result will be `outputLength` bytes long.
    public func derive(storageState: RainbowSlothStorageState, pw: String, outputLength: Int) throws -> Bytes {
        let pres = try preambleDerive(storageState: storageState, pw: pw)
        let k = try innerDerive(storageState: storageState, pres: pres, outputLength: outputLength)
        return k
    }

    /// An evaluation method of the internal `innerDerive` method to measure the effective time guarantees. The returned array contains
    /// `iterations` many measurements of this operation in seconds.
    func eval(storageState: RainbowSlothStorageState, pw: String, outputLength: Int, iterations: Int) throws -> [Double] {
        let pres = try preambleDerive(storageState: storageState, pw: pw)

        var durations = [Double]()
        for _ in 0 ..< iterations {
            let tStart = Date()

            // abort on first throw
            _ = try innerDerive(storageState: storageState, pres: pres, outputLength: outputLength)

            let tDelta = Date().timeIntervalSince(tStart)
            durations.append(tDelta)
        }

        return durations
    }

    private func preambleDerive(storageState: RainbowSlothStorageState, pw: String) throws -> [Bytes] {
        let l = n * P256_PUBLIC_KEY_SIZE
        let pwBytes = Bytes(pw.utf8)

        let pres_combined = try PwHash.derive(salt: storageState.salt, pw: pwBytes, outputLength: l)
        let pres = try pres_combined.chunkify(length: P256_PUBLIC_KEY_SIZE)

        return pres
    }

    private func innerDerive(storageState: RainbowSlothStorageState, pres: [Bytes], outputLength: Int) throws -> Bytes {
        let secretKey = try SlothSecureEnclave.loadSecretSeKey(handle: storageState.handle)

        var posts = [Bytes]()
        for pre in pres {
            let x = ReHashToEc.rehashToP256(seed: pre)

            // abort on first throw
            let post = try SlothSecureEnclave.runECDH(secretKey: secretKey, pubKey: x)

            posts.append(post)
        }
        let posts_combined = Bytes.concat(chunks: posts)

        let k = HkdfSha256.derive(
            salt: storageState.salt,
            ikm: posts_combined,
            info: Bytes(),
            outputLength: outputLength
        )

        return k
    }
}

/// Wrapper for evaluation through the demo app
public enum RainbowSlothEvaluationWrapper {
    public static func runEval(sloth: RainbowSloth, iterations: Int) throws -> [Double] {
        let (storage, _) = try sloth.keygen(pw: "test", handle: "eval", outputLength: 32)
        return try sloth.eval(storageState: storage, pw: "test", outputLength: 32, iterations: iterations)
    }
}

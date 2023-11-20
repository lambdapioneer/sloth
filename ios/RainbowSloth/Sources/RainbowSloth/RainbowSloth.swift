import Foundation


private let P256_PUBLIC_KEY_SIZE = 32

/// Implementation of the RainbowSloth algorithm for tuneable password-based key stretching on iOS using the SecureEnclave (SE).
/// The parameter `n` determines the number of required SE operations and can be increased for higher security levels.
/// Refer to the paper and documentation for more details.
public struct RainbowSloth {
    var n: Int

    public init(withN n: Int) {
        self.n = n;
    }

    /// Generates a new key under the handle `handle` using the password `pw`. The output will be `outputLength` bytes long.
    /// Every call to this function will result in a new key and any existing key under the same handle will be overwritten.
    /// Calls to `derive` using the returned storage state will result in the same key.
    public func keygen(pw: String, handle: String, outputLength: Int) throws -> (RainbowSlothStorageState, Data) {
        let storageState = RainbowSlothStorageState(
            handle: handle,
            salt: PwHash.randomSalt()
        )

        try SecureEnclave.resetSecretSeKey(handle: storageState.handle)

        let k = try derive(storageState: storageState, pw: pw, outputLength: outputLength)

        return (storageState, k)
    }

    /// Re-derives a key from the given storage state. The result will be `outputLength` bytes long.
    public func derive(storageState: RainbowSlothStorageState, pw: String, outputLength: Int) throws -> Data {
        let pres = try preambleDerive(storageState: storageState, pw: pw)
        let k = try innerDerive(storageState: storageState, pres: pres, outputLength: outputLength)
        return k
    }

    /// An evaluation method of the internal derive method to measure the effective time guarantees. The returned array contains
    /// `iterations` many measurements of this operation in seconds.
    public func eval(storageState: RainbowSlothStorageState, pw: String, outputLength: Int, iterations: Int) throws -> [Double] {
        let pres = try preambleDerive(storageState: storageState, pw: pw)

        var durations = [Double]()
        for _ in 0..<iterations {
            let tStart = Date()
            let _ = try innerDerive(storageState: storageState, pres: pres, outputLength: outputLength)

            let tDelta = Date().timeIntervalSince(tStart)
            durations.append(tDelta)
        }

        return durations
    }

    private func preambleDerive(storageState: RainbowSlothStorageState, pw: String) throws -> [Data] {
        let l = n * P256_PUBLIC_KEY_SIZE
        let pwBytes = pw.data(using: .utf8)!
        let pres_combined = PwHash.derive(salt: storageState.salt, pw: pwBytes, outputLength: l)
        return pres_combined.chunkify(length: P256_PUBLIC_KEY_SIZE)
    }

    private func innerDerive(storageState: RainbowSlothStorageState, pres: [Data], outputLength: Int) throws -> Data {
        let secretKey = try SecureEnclave.loadSecretSeKey(handle: storageState.handle)

        var posts = [Data]()
        for pre in pres {
            let x = ReHashToEc.rehashToP256(seed: pre)
            let post = try SecureEnclave.runECDH(secretKey: secretKey, pubKey: x)
            posts.append(post as Data)
        }
        let posts_combined = Data.combine(chunks: posts)

        let k = HkdfSha256.derive(
            salt: storageState.salt,
            ikm: posts_combined,
            info: Data(),
            outputLength: outputLength
        )

        return k
    }
}

/// The required data to be persisted for later re-deriving the same key using `RainbowSloth`.
public struct RainbowSlothStorageState {
    var handle: String
    var salt: Data
}

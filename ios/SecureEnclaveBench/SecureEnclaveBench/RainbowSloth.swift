import Foundation


let DEFAULT_HANDLE = Data()
let P256_PUBLIC_KEY_SIZE = 32

public struct RainbowSloth {
    var n: Int

    init(withN n: Int) {
        self.n = n;
    }

    func keygen(pw: String, handle: String, outputLength: Int) throws -> (RainbowSlothStorageState, Data) {
        let storageState = RainbowSlothStorageState(
            handle: handle,
            salt: randomSalt()
        )

        try SecureEnclave.resetSecretSeKey(handle: storageState.handle)

        let k = try derive(storageState: storageState, pw: pw, outputLength: outputLength)

        return (storageState, k)
    }

    func derive(storageState: RainbowSlothStorageState, pw: String, outputLength: Int) throws -> Data {
        let pres = try preambleDerive(storageState: storageState, pw: pw)
        let k = try innerDerive(storageState: storageState, pres: pres, outputLength: outputLength)
        return k
    }

    func eval(storageState: RainbowSlothStorageState, pw: String, outputLength: Int, iterations: Int) throws -> [Double] {
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
        let pres_combined = PwHash.derive(salt: storageState.salt, pw: pw, l: l)
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
            l: outputLength
        )

        return k
    }
}

public struct RainbowSlothStorageState {
    var handle: String
    var salt: Data
}

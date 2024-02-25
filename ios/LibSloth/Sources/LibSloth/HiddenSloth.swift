import Foundation
import Sodium

/// The `HiddenSlothStorageState` wraps all data required by `HiddenSloth` algorithm to be persisted. It's the implementing
/// app's responsibility to save and load the included items as it seems fit.
public struct HiddenSlothStorageState {
    public var rainbowSlothStorageState: RainbowSlothStorageState
    public var wrappedData: HiddenSlothWrappedData
    public var ciphertext: Bytes
    public var handle: String

    public init(rainbowSlothStorageState: RainbowSlothStorageState, wrappedData: HiddenSlothWrappedData, ciphertext: Bytes, handle: String) {
        self.rainbowSlothStorageState = rainbowSlothStorageState
        self.wrappedData = wrappedData
        self.ciphertext = ciphertext
        self.handle = handle
    }
}

/// All elements of the `HiddenSlothStorageState` that have been wrapped by the SE
public struct HiddenSlothWrappedData {
    public var wrappedIv: Bytes
    public var wrappedTk: Bytes
    public var wrappedTiv: Bytes

    public init(wrappedIv: Bytes, wrappedTk: Bytes, wrappedTiv: Bytes) {
        self.wrappedIv = wrappedIv
        self.wrappedTk = wrappedTk
        self.wrappedTiv = wrappedTiv
    }
}

/// The `HiddenSloth` plausibly-deniable encryption scheme allows to store up-to `maxSize` data. See the paper for more details.
/// This struct is not yet part of the official API and might change in the future.
///
/// Note: while the functions may look pure (i.e. not modifying their input arguments), the `encrypt` and `ratchet` methods interact with
/// the secret key of the SecureEnclave and thus have side-effects.
public struct HiddenSloth {
    var rainbowSloth: RainbowSloth
    var maxSize: Int

    /// The required key length for the `AuthenticatedEncryption` implementation.
    let kLengthBytes = AuthenticatedEncryptionKeyLength

    public init(withRainbowSloth rainbowSloth: RainbowSloth, maxSize: Int) {
        self.rainbowSloth = rainbowSloth
        self.maxSize = maxSize
    }

    /// Creates a new `HiddenSlothStorageState`. This method should be called once when the app is started for the first time
    /// and there is no storage yet.
    public func initializeStorage(handle: String) throws -> HiddenSlothStorageState {
        // Derive initial key from random passphrase
        let pw = try HiddenSloth.generateRandomPassword()
        let hDess = handle + "_dess"
        let (rainbowSlothStorageState, k) = try rainbowSloth.keygen(pw: pw, handle: hDess, outputLength: kLengthBytes)

        // Encrypt an empty byte array
        let inner = try innerEncrypt(k: k, data: Bytes())
        let (ciphertext, wrappedData) = try outerEncrypt(handle: handle, innerCiphertext: inner)

        return HiddenSlothStorageState(rainbowSlothStorageState: rainbowSlothStorageState, wrappedData: wrappedData, ciphertext: ciphertext, handle: handle)
    }

    /// Encrypts the given data using the `pw` passphrase. The provided `state` is superseded by the returned `HiddenSlothStorageState`.
    public func encrypt(state: HiddenSlothStorageState, pw: String, data: Bytes) throws -> HiddenSlothStorageState {
        let k = try rainbowSloth.derive(storageState: state.rainbowSlothStorageState, pw: pw, outputLength: kLengthBytes)

        let inner = try innerEncrypt(k: k, data: data)
        let (ciphertext, wrappedData) = try outerEncrypt(handle: state.handle, innerCiphertext: inner)

        return HiddenSlothStorageState(rainbowSlothStorageState: state.rainbowSlothStorageState, wrappedData: wrappedData, ciphertext: ciphertext, handle: state.handle)
    }

    /// Tries to decrypt the given data using `pw`. If decryption fails (e.g. a wrong passphrase is provided or no data has been stored), then
    /// the method will throw `SlothError.failedToDecryptAuthenticatedCiphertext`.
    public func decrypt(state: HiddenSlothStorageState, pw: String) throws -> Bytes {
        let ciphertext = try outerDecrypt(state: state)

        let k = try rainbowSloth.derive(storageState: state.rainbowSlothStorageState, pw: pw, outputLength: kLengthBytes)
        let inner = try innerDecrypt(k: k, ciphertext: ciphertext)

        return inner
    }

    /// Ratchets the `HiddenSlothStorageState` which subsequently is superseded by the returned new state. This method should ideally
    /// be called on every app start (see paper for details).
    public func ratchet(state: HiddenSlothStorageState) throws -> HiddenSlothStorageState {
        let ciphertext = try outerDecrypt(state: state)

        let hDems = state.handle + "_dems"
        try SlothSecureEnclave.resetSecretSeKey(handle: hDems)

        let (newCiphertext, wrappedData) = try outerEncrypt(handle: state.handle, innerCiphertext: ciphertext)

        return HiddenSlothStorageState(rainbowSlothStorageState: state.rainbowSlothStorageState, wrappedData: wrappedData, ciphertext: newCiphertext, handle: state.handle)
    }

    func innerEncrypt(k: Bytes, data: Bytes) throws -> AuthenticatedCiphertext {
        let size = UInt32(data.count)
        let sizeBytes = withUnsafeBytes(of: size.bigEndian, Array.init)

        // pad to configured size
        let paddingBytes = Bytes(repeating: 0x00, count: self.maxSize - data.count - sizeBytes.count)
        let content = sizeBytes + data + paddingBytes
        assert(content.count == self.maxSize)

        // encrypt with our derived key
        let iv = try AuthenticatedEncryption.generateRandomIV()
        return try AuthenticatedEncryption.encrypt(k: k, iv: iv, data: content)
    }

     func innerDecrypt(k: Bytes, ciphertext: AuthenticatedCiphertext) throws -> Bytes {
        let content = try AuthenticatedEncryption.decrypt(k: k, ciphertext: ciphertext)

        let sizeBytes = Data(content[..<4])
        let size = UInt32(bigEndian: sizeBytes.withUnsafeBytes {$0.load(as: UInt32.self)})

        let contentStart = 4
        let contentEnd = Int(4 + size)
        return Bytes(content[contentStart..<contentEnd])
    }

     func outerEncrypt(handle: String, innerCiphertext: AuthenticatedCiphertext) throws -> (Bytes, HiddenSlothWrappedData) {
        // encrypt the inner ciphertext using fresh keys "in software"
        let tk = try AuthenticatedEncryption.generateRandomKey()
        let tiv = try AuthenticatedEncryption.generateRandomIV()
        let ciphertext = try AuthenticatedEncryption.encrypt(k: tk, iv: tiv, data: innerCiphertext.blobAndTag)

        // then encrypt all keys and IVs using a fresh handle "in the SE"; when comparing this with the Algorithms in
        // the paper notice that here the tag of the authenticated encryption is part of the ciphertext and therefore
        // does not need to be handled separately
        let hDems = handle + "_dems"
        try SlothSecureEnclave.resetSecretSeKey(handle: hDems)

        let wrappedIv = try SlothSecureEnclave.aesGcmEncrypt(handle: hDems, data: innerCiphertext.iv)
        let wrappedTk = try SlothSecureEnclave.aesGcmEncrypt(handle: hDems, data: tk)
        let wrappedTiv = try SlothSecureEnclave.aesGcmEncrypt(handle: hDems, data: tiv)

        let wrappedData = HiddenSlothWrappedData(wrappedIv: wrappedIv, wrappedTk: wrappedTk, wrappedTiv: wrappedTiv)
        return (ciphertext.blobAndTag, wrappedData)
    }

    func outerDecrypt(state: HiddenSlothStorageState) throws -> AuthenticatedCiphertext {
        let hDems = state.handle + "_dems"
        let tiv = try SlothSecureEnclave.aesGcmDecrypt(handle: hDems, ciphertext: state.wrappedData.wrappedTiv)
        let tk = try SlothSecureEnclave.aesGcmDecrypt(handle: hDems, ciphertext: state.wrappedData.wrappedTk)
        let iv = try SlothSecureEnclave.aesGcmDecrypt(handle: hDems, ciphertext: state.wrappedData.wrappedIv)

        let ciphertext = try AuthenticatedEncryption.decrypt(k: tk, ciphertext: AuthenticatedCiphertext(iv: tiv, blobAndTag: state.ciphertext))

        return AuthenticatedCiphertext(iv: iv, blobAndTag: ciphertext)
    }

    /// A performance evaluation method of the `ratchet` method that returns an array of measurements. The array will have `iterations` indepedentent
    /// measuresments that record the time for each `ratchet` execution in seconds.
    func eval(state: HiddenSlothStorageState, iterations: Int) throws -> [Double] {
        var state = state

        var durations = [Double]()
        for _ in 0 ..< iterations {
            let tStart = Date()

            // abort on first throw
            state = try ratchet(state: state)

            let tDelta = Date().timeIntervalSince(tStart)
            durations.append(tDelta)
        }

        return durations
    }

    /// Generates a randon alphanumerical password of the given length. The entropy per character is `log2(62) = 5.954...` .
    /// Hence, the default length of 32 characters results in an entropy of `32 * log2(62) = 190.534...` which is larger than our
    /// default security margin of 128 bit.
    static func generateRandomPassword(outputLength: Int = 32) throws -> String {
        let chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

        var result = ""
        for _ in 0..<outputLength {
            let idx = Int(Sodium().randomBytes.uniform(upperBound: UInt32(chars.count)))
            result += String(chars[chars.index(chars.startIndex, offsetBy: idx)])
        }

        return result
    }

}

/// Wrapper for evaluation through the demo app
public enum HiddenSlothEvaluationWrapper {
    public static func runEval(sloth: HiddenSloth, iterations: Int) throws -> [Double] {
        let storage = try sloth.initializeStorage(handle: "eval")
        return try sloth.eval(state: storage, iterations: iterations)
    }
}

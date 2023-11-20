import CryptoKit
import Foundation

/// A simple wrapper around `HDKF` with `SHA256` using `Data` types.
public struct HkdfSha256 {

    /// Derives a key from the initial key material `ikm` using the given `salt` and `info`. The output will be `outputLength` bytes long.
    public static func derive(salt: Data, ikm: Data, info: Data, outputLength: Int) -> Data {
        let output = HKDF<SHA256>.deriveKey(
            inputKeyMaterial: SymmetricKey(data: ikm),
            salt: salt,
            info: info,
            outputByteCount: outputLength
        )
        return output.withUnsafeBytes { body in
            Data(body)
        }
    }
}

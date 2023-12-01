import CryptoKit
import Foundation

/// A simple wrapper around `HDKF` with `SHA256`
enum HkdfSha256 {

    /// Derives a key from the initial key material `ikm` using the given `salt` and `info`. The output will be `outputLength` bytes long.
    internal static func derive(salt: Bytes, ikm: Bytes, info: Bytes, outputLength: Int) -> Bytes {
        let output = HKDF<SHA256>.deriveKey(
            inputKeyMaterial: SymmetricKey(data: ikm),
            salt: salt,
            info: info,
            outputByteCount: outputLength
        )
        return output.withUnsafeBytes { body in
            Bytes(body)
        }
    }
}

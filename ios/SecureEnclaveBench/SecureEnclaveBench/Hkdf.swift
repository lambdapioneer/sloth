import CryptoKit
import Foundation

public struct HkdfSha256 {

    public static func derive(salt: Data, ikm: Data, info: Data, l: Int) -> Data {
        let output = HKDF<SHA256>.deriveKey(
            inputKeyMaterial: SymmetricKey(data: ikm),
            salt: salt,
            info: info,
            outputByteCount: l
        )
        return output.withUnsafeBytes { body in
            Data(body)
        }
    }
}

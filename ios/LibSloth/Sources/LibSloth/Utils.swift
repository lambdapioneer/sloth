import Foundation
import Sodium

public typealias Bytes = [UInt8]

extension Bytes {
    /// Creates a new `Data` object initialized with bytes decoded from the provided hexadecimal-encoded String.
    init(hex: String) throws {
        guard let bytes = Sodium().utils.hex2bin(hex) else {
            throw SlothError.failedToDecodeHexString
        }
        self = bytes
    }

    /// Returns a hexadecimal-encoded version of this `Data` object.
    func toHex() throws -> String {
        guard let string = Sodium().utils.bin2hex(self) else {
            throw SlothError.failedToEncodeHexString
        }
        return string
    }

    /// Splits this array into arrays that each have `length` bytes. Throws if the `.count` of this object cannot be evenly divided.
    func chunkify(length: Int) throws -> [Bytes] {
        if self.count % length != 0 {
            throw SlothError.failedToChunkifyArray
        }
        let count = self.count / length
        var result = [Bytes]()
        for i in 0..<count {
            let start = i * length
            let end = start + length
            let chunk = Bytes(self[start..<end])
            result.append(chunk)
        }
        return result
    }

    /// Concatenates all given `chunks` to create a new `Bytes` object.
    static func concat(chunks: [Bytes]) -> Bytes {
        var result = Bytes()
        for chunk in chunks {
            result.append(contentsOf: chunk)
        }
        return result
    }
}

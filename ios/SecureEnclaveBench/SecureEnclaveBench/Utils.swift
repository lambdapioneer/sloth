import Foundation


public func randomSalt(l: Int = 32) -> Data {
    var bytes = [UInt8](repeating: 0, count: l)
    let result = SecRandomCopyBytes(kSecRandomDefault, bytes.count, &bytes)
    assert(result == errSecSuccess, "Failed to generate random bytes")
    return Data(bytes)
}

public extension Data {
    init(hex: String) {
        let l = hex.count / 2
        var data = Data(capacity: l)
        var begin = hex.startIndex
        for _ in 0..<l {
            let end = hex.index(begin, offsetBy: 2)
            let bytes = hex[begin..<end]
            begin = end
            var b = UInt8(bytes, radix: 16)!
            data.append( &b, count: 1)
        }
        self = data
    }

    var toHex: String {
        return map { String(format: "%02x", $0) }
            .joined()
    }

    func chunkify(length: Int) -> [Data] {
        precondition(self.count % length == 0, "Data is not evenly divisible by chunk size.")
        let count = self.count / length
        var result = [Data]()
        for i in 0..<count {
            let start = i * length
            let end = start + length
            let chunk = self[start..<end]
            result.append(chunk)
        }
        return result
    }

    static func combine(chunks: [Data]) -> Data {
        var result = Data()
        for chunk in chunks {
            result.append(chunk)
        }
        return result
    }
}

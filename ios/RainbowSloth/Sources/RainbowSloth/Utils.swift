import Foundation


internal extension Data {

    /// Creates a new `Data` object initialized with bytes decoded from the provided hexadecimal-encoded String.
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

    /// Returns a hexadecimal-encoded version of this `Data` object.
    var toHex: String {
        return map { String(format: "%02x", $0) }
            .joined()
    }

    /// Splits this array into arrays that each have `length` bytes. Throws if the `.count` of this object cannot be evenly divided.
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

    /// Concatenates all given `chunks` to create a new `Data` object.
    static func combine(chunks: [Data]) -> Data {
        var result = Data()
        for chunk in chunks {
            result.append(chunk)
        }
        return result
    }
}

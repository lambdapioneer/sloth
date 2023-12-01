import SystemConfiguration
import UIKit

// See: https://stackoverflow.com/a/46380596
public extension UIDevice {
    static let modelName: String = {
        #if targetEnvironment(simulator)
        let identifier = "simulator," + ProcessInfo().environment["SIMULATOR_MODEL_IDENTIFIER"]!
        #else
        var systemInfo = utsname()
        uname(&systemInfo)
        let machineMirror = Mirror(reflecting: systemInfo.machine)
        let identifier = machineMirror.children.reduce("") { identifier, element in
            guard let value = element.value as? Int8, value != 0 else { return identifier }
            return identifier + String(UnicodeScalar(UInt8(value)))
        }
        #endif
        return identifier
    }()
}

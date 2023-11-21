import Foundation

internal enum SlothSecureEnclaveError: Error {
    case loadFailed(String)
    case genFailed(String)
    case ecdhFailed(String)
}

internal struct SecureEnclave {

    internal static func runECDH(secretKey: SecKey, pubKey: SecKey) throws -> CFData {
        let keyPairAttr:[String : Any] = [
             kSecAttrKeySizeInBits as String: 256,
             kSecAttrKeyType as String: kSecAttrKeyTypeEC,
             kSecPrivateKeyAttrs as String: [kSecAttrIsPermanent as String: false],
             kSecPublicKeyAttrs as String:[kSecAttrIsPermanent as String: false],
             SecKeyKeyExchangeParameter.requestedSize.rawValue as String: 256
         ]

        var cfError: Unmanaged<CFError>?
        guard let sharedKey: CFData = SecKeyCopyKeyExchangeResult(
            secretKey,
            SecKeyAlgorithm.ecdhKeyExchangeCofactorX963SHA256,
            pubKey,
            keyPairAttr as CFDictionary,
            &cfError
        ) else {
            throw cfError!.takeRetainedValue() as Error
        }
        return sharedKey
    }

    internal static func genRandomPublicKey() throws -> SecKey {
        let pairAttributes: NSDictionary = [
            kSecAttrKeyType: kSecAttrKeyTypeEC,
            kSecAttrKeySizeInBits: 256,
            kSecPublicKeyAttrs:  [
                kSecAttrIsPermanent: false,
            ],
            kSecPrivateKeyAttrs: [
                kSecAttrIsPermanent: false,
            ]
        ]
        var pubKey, secretKey: SecKey?
        let status = SecKeyGeneratePair(pairAttributes as CFDictionary, &pubKey, &secretKey)
        guard status == errSecSuccess else {
            throw SlothSecureEnclaveError.genFailed("generating random public key failed")
        }
        return pubKey!

    }

    internal static func loadSecretSeKey(handle: String = "sloth_default") throws -> SecKey {
        let query: NSDictionary = [
            kSecClass: kSecClassKey,
            kSecAttrApplicationTag: handle,
            kSecAttrKeyType: kSecAttrKeyTypeEC,
            kSecReturnRef: true
        ]

        var item: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &item)
        guard status == errSecSuccess else {
            throw SlothSecureEnclaveError.loadFailed("load failed")
        }
        return item as! SecKey
    }

    internal static func resetSecretSeKey(handle: String = "sloth_default") throws {
        // (1) delete existing key (if any) under the given handle, as create does not
        // overwrite existing keys
        let query: NSDictionary = [
            kSecClass: kSecClassKey,
            kSecAttrApplicationTag: handle,
            kSecAttrKeyType: kSecAttrKeyTypeEC,
            kSecReturnRef: true
        ]
        let _ = SecItemDelete(query as CFDictionary) // ignore if we fail to

        // (2) create a new key
        let attributes: NSDictionary = [
            kSecAttrKeyType: kSecAttrKeyTypeEC,
            kSecAttrKeySizeInBits: 256,
            kSecAttrTokenID: kSecAttrTokenIDSecureEnclave,
            kSecPrivateKeyAttrs: [
                kSecAttrIsPermanent: true,
                kSecAttrApplicationTag: handle,
            ]
        ]

        var cfError: Unmanaged<CFError>?
        guard let _: SecKey = SecKeyCreateRandomKey(attributes, &cfError) else {
            throw cfError!.takeRetainedValue() as Error
        }
    }

    internal static func exportPublicKey(pubKey: SecKey) throws -> CFData {
        var cfError: Unmanaged<CFError>?
        guard let keyData = SecKeyCopyExternalRepresentation(pubKey, &cfError) else {
            throw cfError!.takeRetainedValue() as Error
        }
        return keyData
    }

    internal static func importPublicKey(keyData: CFData) throws -> SecKey {
        let importAttributes: NSDictionary = [
            kSecAttrKeyType: kSecAttrKeyTypeEC,
            kSecAttrKeySizeInBits: 256,
            kSecAttrKeyClass: kSecAttrKeyClassPublic,
        ]
        var cfError: Unmanaged<CFError>?
        guard let reimportedPublicKey = SecKeyCreateWithData(keyData, importAttributes, &cfError) else {
            throw cfError!.takeRetainedValue() as Error
        }
        return reimportedPublicKey
    }

    internal static func runEval(iterations: Int) throws -> [Double] {
        // create keys to play with
        try! resetSecretSeKey()
        let secretSeKey = try! loadSecretSeKey()
        debugPrint("secretSeKey", secretSeKey)

        let randomPublicKey = try! genRandomPublicKey()
        debugPrint("randomPublicKey", randomPublicKey)

        // making sure all hardware syncs can happen
        sleep(1)

        var times: [Double] = []

        for _ in 1...iterations {
            let tStart = Date()

            try runEvalIteration(secretKey: secretSeKey, pubKey: randomPublicKey)

            times.append(Date().timeIntervalSince(tStart))
        }

        return times
    }

    private static func runEvalIteration(secretKey: SecKey, pubKey: SecKey) throws {
        let _ = try! runECDH(secretKey: secretKey, pubKey: pubKey) // run one ecdh as baseline
    }
}

/// Wrapper for evaluation through the demo app
public struct SecureEnclaveEvaluationWrapper {
    public static func runEval(iterations: Int) throws -> [Double] {
        return try SecureEnclave.runEval(iterations: iterations)
    }
}

import CryptoKit
import Foundation

enum SlothSecureEnclave {

    /// Performs a Diffie-Hellmann with the `secretKey` stored inside the Secure Enclave and the
    /// provided `pubKey` stored in regular memory.
    static func runECDH(secretKey: SecKey, pubKey: SecKey) throws -> Bytes {
        guard SecKeyIsAlgorithmSupported(secretKey, .keyExchange, .ecdhKeyExchangeCofactorX963SHA256) else {
            throw SlothError.unsupportedEcdhAlgorithm
        }

        let keyPairAttr: NSDictionary = [
            kSecAttrKeySizeInBits: 256,
            kSecAttrKeyType: kSecAttrKeyTypeECSECPrimeRandom,
            kSecPrivateKeyAttrs: false,
            kSecPublicKeyAttrs: false,
            SecKeyKeyExchangeParameter.requestedSize.rawValue: 256
        ]

        var cfError: Unmanaged<CFError>?
        guard let sharedKey: CFData = SecKeyCopyKeyExchangeResult(
            secretKey,
            SecKeyAlgorithm.ecdhKeyExchangeCofactorX963SHA256,
            pubKey,
            keyPairAttr as CFDictionary,
            &cfError
        ) else {
            throw SlothError.failedToPerformEcdh
        }

        return Bytes(sharedKey as Data)
    }

    /// Generates a random public key outside the Secure Enclave
    static func generateRandomPublicKey() throws -> SecKey {
        let pairAttributes: NSDictionary = [
            kSecAttrKeyType: kSecAttrKeyTypeECSECPrimeRandom,
            kSecAttrKeySizeInBits: 256,
            kSecPublicKeyAttrs: [
                kSecAttrIsPermanent: false
            ],
            kSecPrivateKeyAttrs: [
                kSecAttrIsPermanent: false
            ]
        ]

        var pubKey, secretKey: SecKey?
        let status = SecKeyGeneratePair(pairAttributes as CFDictionary, &pubKey, &secretKey)
        guard status == errSecSuccess else {
            throw SlothError.failedToGeneratePublicKey
        }

        guard let res = pubKey else {
            throw SlothError.failedToGeneratePublicKey
        }
        return res
    }

    /// Loads an existing secret key that has been stored inside the Secure Enclave under `handle`.
    static func loadSecretSeKey(handle: String) throws -> SecKey {
        let query = getQueryForSecretSeKey(handle: handle)

        var secItem: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &secItem)
        guard status == errSecSuccess else {
            throw SlothError.failedToLoadSecretKey
        }
        return secItem as! SecKey
    }

    /// Resets the secret key under `handle` (if any) inside the Secure Enclave.
    static func resetSecretSeKey(handle: String) throws {
        // (1) delete existing key (if any) under the given handle, as create does not
        // overwrite existing keys
        let query = getQueryForSecretSeKey(handle: handle)
        _ = SecItemDelete(query as CFDictionary) // ignore if we fail to

        // (2) setup propertoes of new key
        guard let accessControl = SecAccessControlCreateWithFlags(
            kCFAllocatorDefault,
            kSecAttrAccessibleWhenUnlockedThisDeviceOnly,
            .privateKeyUsage,
            nil
        ) else {
            throw SlothError.failedToSetAccessControl
        }
        let attributes: NSDictionary = [
            kSecAttrKeyType: kSecAttrKeyTypeECSECPrimeRandom,
            kSecAttrKeySizeInBits: 256,
            kSecAttrTokenID: kSecAttrTokenIDSecureEnclave,
            kSecPrivateKeyAttrs: [
                kSecAttrIsPermanent: true,
                kSecAttrApplicationTag: handle,
                kSecAttrAccessControl: accessControl
            ]
        ]

        // (3) generate new key
        var cfError: Unmanaged<CFError>?
        guard SecKeyCreateRandomKey(attributes, &cfError) != nil else {
            throw SlothError.failedToGenerateSecretKey
        }
    }

    private static func getQueryForSecretSeKey(handle: String) -> NSDictionary {
        return [
            kSecClass: kSecClassKey,
            kSecAttrApplicationTag: handle,
            kSecAttrKeyType: kSecAttrKeyTypeECSECPrimeRandom,
            kSecReturnRef: true
        ]
    }

    /// Exports the given public key as a `Bytes` array.
    static func exportPublicKey(pubKey: SecKey) throws -> Bytes {
        var cfError: Unmanaged<CFError>?
        guard let keyData = SecKeyCopyExternalRepresentation(pubKey, &cfError) else {
            throw SlothError.failedToExportPublicKey
        }
        return Bytes(keyData as Data)
    }

    static func importPublicKey(bytes: Bytes) throws -> SecKey {
        let importAttributes: NSDictionary = [
            kSecAttrKeyType: kSecAttrKeyTypeECSECPrimeRandom,
            kSecAttrKeySizeInBits: 256,
            kSecAttrKeyClass: kSecAttrKeyClassPublic
        ]
        var cfError: Unmanaged<CFError>?
        guard let importedPublicKey = SecKeyCreateWithData(Data(bytes) as CFData, importAttributes, &cfError) else {
            throw SlothError.failedToImportPublicKey
        }
        return importedPublicKey
    }

    static func runEval(iterations: Int) throws -> [Double] {
        // create keys for evaluation
        try resetSecretSeKey(handle: "evaluation_handle")
        let secretSeKey = try loadSecretSeKey(handle: "evaluation_handle")
        let randomPublicKey = try generateRandomPublicKey()

        // this sleep is not strictly necessary, but let here as an extra opportunity
        // for the device to return to a low activity before our measurements
        sleep(1)

        var times: [Double] = []
        for _ in 1 ... iterations {
            let tStart = Date()

            // we measure one ECDH execution
            _ = try runECDH(secretKey: secretSeKey, pubKey: randomPublicKey)

            times.append(Date().timeIntervalSince(tStart))
        }

        return times
    }
}

/// Wrapper for evaluation through the demo app
public enum SecureEnclaveEvaluationWrapper {
    public static func runEval(iterations: Int) throws -> [Double] {
        return try SlothSecureEnclave.runEval(iterations: iterations)
    }
}

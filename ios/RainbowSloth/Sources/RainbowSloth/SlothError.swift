import Foundation

enum SlothError: Error {
    //
    // Errors related to CryptoKit and Secure Enclave
    //

    /// The device does not support the ECDH algorithm for our key type
    case unsupportedEcdhAlgorithm

    /// CyrptoKit failed to generate a new random public key
    case failedToGeneratePublicKey

    /// CryptoKit failed to load a key inside the Secure Enclave under our handle
    case failedToLoadSecretKey

    /// CryptoKit failed to create access control parameters
    case failedToSetAccessControl

    /// CryptoKit failed to perform ECDH with the Secure Enclave
    case failedToPerformEcdh

    /// CryptoKit failed to generate a secret key inside the Secure Enclave
    case failedToGenerateSecretKey

    /// CryptoKit failed to export a public key
    case failedToExportPublicKey

    /// CryptoKit failed to import a public key
    case failedToImportPublicKey

    //
    // Errors related to LibSodium
    //

    /// A call to derive an Argon2 hash using LibSodium failed
    case failedToDeriveArgon2Hash

    /// A call to generate random bytes using LibSodium failed
    case failedToGenerateRandomBytes

    /// A call to decode a hex string using LibSodium failed
    case failedToDecodeHexString

    /// A call to encode a byte array into a hex string using LibSodium failed
    case failedToEncodeHexString

    //
    // Errors related to the RainbowSloth algorithms and utils
    //

    /// The derivation of EC keys failed to calculate a seed value for one of its rounds
    case failedToConvertRehashSeed

    /// Trying to split an array into chunks of the same size failed. Most likely the total length was not evenly divisible.
    case failedToChunkifyArray

    /// Retrieving the UTF-8 decodeing of a string failed
    case failedToDecodePasswordUtf8
}

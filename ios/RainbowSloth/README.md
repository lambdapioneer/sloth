# Sloth: iOS

We have implemented the SE-backed key stretching scheme RainbowSloth for iOS 15+.
This folder gets synced into a designated repository to allow inclusion as a Swift package dependency.

For changes refer to the main repository here: https://github.com/lambdapioneer/sloth


## Setting up

Add this repository as a dependency to your `Package.swift` file like so:

```swift
    dependencies: [
        .package(url: "https://github.com/lambdapioneer/sloth-ios.git", from: "0.0.1"),
    ],
    
    // ...
    
            dependencies: [
                .product(name: "RainbowSloth", package: "sloth-ios")
            ]
```


## Using RainbowSloth

After adding the dependency you can import the library in the respective `.swift` files and use it:

```swift
import RainbowSloth

// create a new Sloth instance
let sloth = RainbowSloth(withN: 100) // see paper on how to choose `n`

// create a new key
let (storageState, key) = try sloth.keygen(
    pw: "user-passphrase",
    handle: "your-identifier",
    outputLength: 32
)

// re-derive the same key later
let key = try sloth.derive(
    storageState: storageState,
    pw: "user-passphrase",
    outputLength: 32
)
```

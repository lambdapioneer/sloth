// swift-tools-version: 5.9

import PackageDescription

let package = Package(
    name: "RainbowSloth",
    platforms: [.iOS(.v14)],
    products: [
        .library(
            name: "RainbowSloth",
            targets: ["RainbowSloth"]),
    ],
    dependencies: [
        .package(url: "https://github.com/jedisct1/swift-sodium.git", from: "0.9.1"),
    ],
    targets: [
        .target(
            name: "RainbowSloth",
            dependencies: [
                .product(name: "Sodium", package: "swift-sodium")
            ]
        ),
        .testTarget(
            name: "RainbowSlothTests",
            dependencies: ["RainbowSloth"]),
    ]
)

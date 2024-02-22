// swift-tools-version: 5.7

import PackageDescription

let package = Package(
    name: "LibSloth",
    platforms: [.iOS(.v14)],
    products: [
        .library(
            name: "LibSloth",
            targets: ["LibSloth"])
    ],
    dependencies: [
        .package(url: "https://github.com/jedisct1/swift-sodium.git", from: "0.9.1")
    ],
    targets: [
        .target(
            name: "LibSloth",
            dependencies: [
                .product(name: "Sodium", package: "swift-sodium")
            ]
        ),
        .testTarget(
            name: "LibSlothTests",
            dependencies: ["LibSloth"])
    ]
)

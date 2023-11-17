# Sloth: iOS

We have implemented **RainbowSloth** for iOS as a demo project included in this folder
This code is developed as an academic prototype and not intended for production use yet.

## Getting started

For install the Argon2 dependency using CocoaPods.
If you do  not have CocoaPods installed, follow the instructions on their website: https://cocoapods.org/

Then enter the iOS folder and execute the following:

```bash
pod install
```

Then open the `Sloth.xcworkspace` file in Xcode.
It is important to open the workspace file and not the project file.


### Adding Sloth to your iOS project

> [!IMPORTANT]
> We have not _yet_ published library artifacts. For now, you need to build the project locally.


## The benchmark app

The project comes with a benchmark app that is also used to generate the performance numbers that are reported in the paper.
You can install it on a real device or a simulator using Xcode.

For our evaluation we created .IPA files that we then run online using AWS DeviceFarm.
During these runs we manually interacted with the app through the web interface of DeviceFarm.
The app reports the results to a custom server that we run on one of our servers.
The server and how to run it is described inside the [`server` folder](server/).

To replicate these steps first start the server on a server and update the IP address in the [`ContentView.swift`](SecureEnclaveBench/SecureEnclaveBench/ContentView.swift) file.
Build the artifacts by running `build_artifact.sh`.
Start the simulator (or use real devices) and install the IPA file.
Execute the scenarios under test using the UI and observe that results are collected on the server as log files.


## Tests

The project bundles tests for the individual components and you can execute them as usual through the IDE on either the simulators or a real device.

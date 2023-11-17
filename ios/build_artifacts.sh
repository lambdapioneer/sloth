#!/bin/bash
set -e

mkdir -p Artifacts;
rm -rf Artifacts/*;

cd SecureEnclaveBench;

xcodebuild clean -workspace SecureEnclaveBench.xcworkspace -scheme SecureEnclaveBench;
xcodebuild build -workspace SecureEnclaveBench.xcworkspace -scheme SecureEnclaveBench;
xcodebuild archive -workspace SecureEnclaveBench.xcworkspace -scheme SecureEnclaveBench -archivePath ../Artifacts/SecureEnclaveBench.xcarchive;
xcodebuild -exportArchive -archivePath ../Artifacts/SecureEnclaveBench.xcarchive -exportOptionsPlist ../ExportOptions.plist -exportPath ../Artifacts;

# Artifact Appendix

Paper title: **Sloth: Key Stretching and Deniable Encryption using Secure Elements on Smartphones**

Requested Badge: **Reproducible**

Artifacts HotCRP ID: https://artifact.petsymposium.org/artifact2024.4/paper.php/7

Artifact URL: https://github.com/lambdapioneer/sloth/tree/aec

Note: This file is also included in the submitted repository as reference for future users. We suggest that the artifact is assigned to reviewers who are familiar with Android/iOS development.


## Description

Our paper introduces a family of protocols for key stretching and plausibly deniable storage that we have implemented on Android and iOS.
This repository contains libraries for the respective platforms that can be easily integrated by other developers.
In addition, we provide scripts that we used for testing and evaluation purposes.
For example, on Android, you can use our scripts to run the full evaluation automatically on many real devices in parallel using AWS Device Farm.

We suggest that the artifact evaluation focuses on the Android artifacts and evaluation scripts, as those do not require any specialized equipment and developer accounts.
Nevertheless, we also describe how to run the iOS evaluation using either a local device or a remote session on AWS Device Farm.

Good luck!


### Security/Privacy Issues and Ethical Concerns

⚠️ **Important:** Running experiments using AWS Device Farm results in costs. 
Please ensure you are familiar with the [AWS Device Farm pricing](https://aws.amazon.com/device-farm/pricing/) before continuing.
The total costs for the proposed experiments should not exceed $100 and every new AWS account comes with free minutes.
Those free minutes should be sufficient to run all described experiments.

Testing the iPhone artifacts might require an Apple Developer account which incurs a yearly subscription fee.
This is required to sign the app in order to run it on physical devices (such as those available on AWS Device Farm).


## Basic Requirements

For the 🤖 Android artifacts, the reviewer requires access to:
- A local computer running Ubuntu 24.04 (or similar). MacOS might also work, but is not tested.
- Android Studio and Python 3
- Either:
  - An AWS account with access to Device Farm (recommended)
  - A local phone with SE support (helpful for debugging)

For the 🍏 iOS artifacts, the reviewer requires access to:
- An up-to-date Mac machine with XCode installed.
- An Apple Developer account (required for AWS Device Farm)
- And either:
  - A local iPhone device (recommended)
  - An AWS account with access to Device Farm _and_ an Apple Developer account. 
- For collecting the results a publicly reachable server is required.

For the evaluation notebooks, the reviewer requires access to:
- A Linux/Mac machine with Python3 installed
- The results generated from the Android/iOS tests


### Hardware Requirements

For Android only a general-purpose computer with Linux is required.
MacOS is might also work, but is not tested.
A physical Android device with a SE can be helpful--Table 4 in the Appendix lists common devices and their compatibility.
For the evaluation, we recommend to use AWS Device Farm instead which provides remote access to physical devices managed by AWS.
In particular, you will have access to the same exact models we used for our evaluation.

For iOS a Mac machine that can run a recent XCode version is required.
The artifacts can then tested using a physical iPhone device or AWS Device Farm.
Note that for the latter the app needs to be signed with requires an Apple Developer account subscription.

As the iOS benchmarks requires uploading the evaluation results to a simple Linux web service, you will need access to a simple publicly-reachable server. A simple DigitalOcean droplet or similar is sufficient.


### Software Requirements

For 🤖 Android:
- Ubuntu (test with 24.04)
- Android Studio (tested with 2023.3.1)
- AWS CLI (tested with 2.15.58)
- Python 3
- OpenJDK 17

For 🍏 iOS:
- MacOS (most recent)
- XCode (most recent)
- Python 3

We discuss how to set up the environment in the next section. So, for now do not download or install anything yet.


### Estimated Time and Storage Consumption

The overall evaluation should require about 4 hours of interactive work and about 1 hour waiting for results.
Most time will be spent setting up the environment and might be higher if you are not familiar with Android or iOS development.
The overall disk space requirements are less than 50 GiB.


## Environment

### Accessibility

The repository is available here: https://github.com/lambdapioneer/sloth/tree/aec

Since the repository is actively used by other projects already, we use the **branch `aec`** during artifact evaluation.
We will merge that branch into `main` with all updates that resulted from reviewer feedback when the artifact evaluation concludes.


### Set up the environment

Mobile development requires a few steps to set up the environment.
We describe the Android and iOS procedures separately, as they are likely executed on different machines.


#### 🤖 Android

We first describe how to set up the Android environment.

1. Clone the repository and checkout the `aec` branch:
```bash
$ git clone https://github.com/lambdapioneer/sloth.git
$ cd sloth
$ git checkout aec
```

2. Download and install Android Studio from https://developer.android.com/studio

3. Open the project in Android Studio and build the project. In particular, you should be able to install and run the app on an emulator or physical device. However, on the emulator the SE will not be available and therefore the app will show an error when trying to use it.

4. Also check that you can build the project from the command line. This is important for the automated testing with AWS Device Farm later. If it is complaining about a missing JDK (or you run into any other problems), ensure that you have installed OpenJDK 17.
```bash
# Optionally: ensure only OpenJDK 17 is installed
$ sudo apt remove openjdk*
$ sudo apt install openjdk-17-jdk

# Build the Android project via CLI
$ cd android
$ ./gradlew build
```


#### AWS Device Farm (for 🤖 Android)

1. Create an AWS account if you do not have one yet.

2. Go to the AWS Device Farm console and create a new project. We name it `sloth-aec` for this example.

3. Note the **project ARN** as you will need it later. It will look like this: `arn:aws:devicefarm:us-west-2:344656001885:project:d9e2bbda-80f4-4e37-ad4e-02093a2015f5`

4. From the project page, create a new device pool by clicking "Project Settings" -> "Device Pools" -> "Create a new device pool".

5. Choose "Create a static device" and name it `aec-single`. Select "Galaxy S22 5G" with OS version 13 as the only device. We use this minimal device pool to verify our setup cheaply without spinning up too many devices.

6. Repeat this step again for our main evaluation device pool that we call `aec-small`. In particular, we choose the following devices: "Samsung Galaxy S22 5G" with OS version 13, "Samsung Galaxy S21" with OS version 12, "Google Pixel 7" with OS version 13, and "Google Pixel 3" with OS version 10

7. Now we install the AWS CLI on your local machine. For this, follow the instructions at https://docs.aws.amazon.com/cli/latest/userguide/install-cliv2.html

8. Configure the AWS CLI with your credentials by running `aws configure` and entering your access key and secret key. Set the region to match the region of your Device Farm project (likely `us-west-2`).

9. In the Android folder create a new Python virtual environment and install the required packages:
```bash
$ cd android
$ python3 -m venv env
$ source env/bin/activate
(env) $ pip install -r requirements.txt
```

10. Now we use the project ARN to identify the ARNs for the device pool. Make sure to change the ARN to **your** project ARN.

```bash
# You can also list your Device Farm project ARNs using `aws devicefarm list-projects`
aws devicefarm list-device-pools --arn arn:aws:devicefarm:us-west-2:344656001885:project:d9e2bbda-80f4-4e37-ad4e-02093a2015f5
```

11. Open the `run_on_aws_devicefarm.py` script in your favorite editor, and update the configuration variables at the top of the file. In particular, set the `PROJECT_ARN` to your project ARN and the `DEVICE_POOL_ALIASES` dictionary to the ARNs of the device pool you want to use. Do not rename the keys (`single` and `small`).

12. Now we can run the script to upload the app to Device Farm and start a test run using our smallest pool. This will take a few minutes to complete. While it runs you can observe the progress via the AWS web console.

```bash
$ source env/bin/activate
(env) $ python3 run_on_aws_devicefarm.py --device-pool single --rebuild-apks
2024-05-24 15:19:29,510 INFO   botocore.credentials Found credentials in shared credentials file: ~/.aws/credentials
2024-05-24 15:19:29,543 INFO   main                 client=<botocore.client.DeviceFarm object at 0x78eb27f8f8c0>
2024-05-24 15:19:30,043 INFO   main                 Device pool found and verified. It has 1 devices.

BUILD SUCCESSFUL in 6s
262 actionable tasks: 97 executed, 165 up-to-date
2024-05-24 15:19:37,281 INFO   main                 Android ./gradlew build successful
2024-05-24 15:19:37,281 INFO   main                 Found app APK file: ./app/build/outputs/apk/debug/app-debug.apk
2024-05-24 15:19:37,281 INFO   main                 Found tests APK file: ./bench/build/outputs/apk/androidTest/debug/bench-debug-androidTest.apk
2024-05-24 15:19:37,281 INFO   main                 run_id='single-hw-support-2024-05-24-151937'
2024-05-24 15:19:37,324 INFO   main                 Uploading ./app/build/outputs/apk/debug/app-debug.apk to Device Farm as single-hw-support-2024-05-24-151937-app-debug.apk
2024-05-24 15:19:42,558 INFO   main                 SUCCEEDED: app_upload_arn='arn:aws:devicefarm:us-west-2:344656001885:upload:d9e2bbda-80f4-4e37-ad4e-02093a2015f5/320e9606-feee-4539-aee8-d3a9cc5cad84'
2024-05-24 15:19:42,600 INFO   main                 Uploading ./bench/build/outputs/apk/androidTest/debug/bench-debug-androidTest.apk to Device Farm as single-hw-support-2024-05-24-151937-bench-debug-androidTest.apk
2024-05-24 15:19:46,498 INFO   main                 SUCCEEDED: tests_upload_arn='arn:aws:devicefarm:us-west-2:344656001885:upload:d9e2bbda-80f4-4e37-ad4e-02093a2015f5/8e8a0770-5ad1-4a9a-8835-fddd9dcc2b6c'
2024-05-24 15:19:46,938 INFO   main                 Scheduled as run_arn='arn:aws:devicefarm:us-west-2:344656001885:run:d9e2bbda-80f4-4e37-ad4e-02093a2015f5/d08f90dc-699f-4f5f-9bd6-2e0097552f75'
2024-05-24 15:19:46,985 INFO   main                 Run is in state SCHEDULING... waiting
2024-05-24 15:19:57,032 INFO   main                 Run is in state PENDING... waiting
2024-05-24 15:20:07,085 INFO   main                 Run is in state PENDING... waiting
2024-05-24 15:20:17,135 INFO   main                 Run is in state RUNNING... waiting
[...]
2024-05-24 15:26:19,120 INFO   main                 Run is in state RUNNING... waiting
2024-05-24 15:26:29,174 INFO   main                 Run reached state COMPLETED
2024-05-24 15:26:29,230 INFO   main                 Handling job 1/1: SamsungGalaxyS225G
2024-05-24 15:26:31,174 INFO   main                 -> stored: ../results/single-hw-support-2024-05-24-151937/SamsungGalaxyS225G/testIsDefaultKeyGenerationHardwareBackedHmacShort.json (393 KiB)
2024-05-24 15:26:31,180 INFO   main                 -> stored: ../results/single-hw-support-2024-05-24-151937/SamsungGalaxyS225G/testCanCreateStrongBoxKeyCtr.json (453 KiB)
[...]
2024-05-24 15:26:40,182 INFO   main                 -> stored: ../results/single-hw-support-2024-05-24-151937/SamsungGalaxyS225G/testHiddenSloth_withSmall_withOpRatchet.json (11940 KiB)
```

13. Phew! Well done following along so far 🥳.


#### 🍏 iOS

We now describe how to set up the iOS environment.

1. Clone the repository and checkout the `aec` branch:
```bash
$ git clone https://github.com/lambdapioneer/sloth.git
$ cd sloth
$ git checkout aec
```

2. Install XCode from the App Store.

3. Open the project in XCode and build the project. In particular, you should be able to start and interact with the app on the simulator. The iOS simulator emulates the SE, so you can test all features without a physical device. However, the performance numbers will be different.

4. If you have an Apple developer account, sign into it using the preferences and update the project configuration to use you "team" for signing the app.

5. If you are using a local iPhone for testing, [enable developer mode on the device](https://developer.apple.com/documentation/xcode/enabling-developer-mode-on-a-device) and connect it to your Mac. Then select the device as the target in XCode and run the app.

**Note:** you will need to be signed in with the same Apple ID in both XCode and your iPhone.


#### AWS Device Farm (for 🍏 iOS)

1. Create an AWS account if you do not have one yet.

2. Go to the AWS Device Farm console and create a new project. We name it `sloth-aec` for this example.

That is all for now and we will create on-demand interactive sessions later during the evaluation.


#### Web service (for 🍏 iOS)

1. Create a new DigitalOcean droplet or similar. We recommend picking the cheapest x86 instance available. For local testing, you can also use a local machine that is one the same network as the iPhone target.

2. Clone the repository and checkout the `aec` branch:
```bash
$ git clone https://github.com/lambdapioneer/sloth.git
$ cd sloth
$ git checkout aec
```

3. Install Python 3 and the required packages:
```bash
$ sudo apt update
$ sudo apt install python3 python3-pip python3-venv
$ cd ios/server
$ python3 -mvenv env
$ source env/bin/activate
(env) $ python3 -mpip install -r requirements.txt
```

4. Start the server:
```bash
(env) $ mkdir data/
(env) $ python3 -mflask --app main run
```

5. Note the public **server IP**. You later use this IP address to upload the evaluation results from the interactive iOS benchmark app.

6. Verify that the server is indeed reachable by opening a browser and navigating to `http://<server-ip>:5000/ios-report`. If simply accessing with your browsers, you should see an error page.

⚠️ **Warning:** the provided server code is just a simple solution for evaluation and not suitable for production use. In particular, it is likely vulnerable to even simple attacks. We recommend to run it on a dedicated machine and to shut it down after the experiments finished.

#### Python notebooks

1. Navigate to your already checked out `aec` branch of the repository (see Android or iOS steps).

2. Install the required packages:
```bash
$ cd notebooks
$ python3 -mvenv env
$ source env/bin/activate
(env) $ python3 -mpip install -r requirements.txt
```

3. Start Jupyter to verify everything is working:
```bash
$ source env/bin/activate
(env) $ jupyter notebook
```


### Testing the Environment

We included basic testing as part of the setup instructions above.
So, there are no additional steps for this section.


## Artifact Evaluation

During the artifact evaluation we will reproduce the most important figures of the papers.
In particular, we will show the following important claims:
- The performance of the key stretching algorithms LongSloth and RainbowSloth scales with its parameters
- The performance of HiddenSloth is comparable to the numbers shown in the paper

Note that the experiments that you will execute are run against a newer version of the library with slightly different performance characteristics.
However, the numbers should be comparable for all practical concerns.

### Main Results and Claims

We will reproduce all main results from the paper by recreating all plots shown in the evaluation section.
Since the device survey (Table 4 in the Appendix) is very costly to create (requires many AWS Device Farm minutes), we do not include it here.
In addition, both the Android and iOS projects come with extensive unit and integration tests that can be executed locally to convince oneself of the correctness of the implementation.

#### Main Result 1: Duration of HMAC Operations on 🤖 Android

We reproduce the results from **Figure 4** which shows the duration of HMAC operations on Android for different devices.
Importantly, we show that the duration scales roughly linearly with the chosen size parameter.

#### Main Result 2: Duration of ECDH Operations on 🍏 iOS

We reproduce the results from **Figure 3** which shows the duration of ECDH operations on iOS for different devices.
Importantly, we show that the duration of individual operations is predictable for each device.

#### Main Result 3: Duration of the LongSloth.Derive Operations on 🤖 Android

We reproduce the results from **Figure 5 (top)** which shows the duration of the LongSloth.Derive operations on Android for different devices.
Importantly, we show that we can parameterize it so that it meet a minimum time requirements.

#### Main Result 4: Duration of the LongSloth.Derive Operations on 🍏 iOS

We reproduce the results from **Figure 5 (bottom)** which shows the duration of the LongSloth.Derive operations on iOS for different devices.
Importantly, we show that we can parameterize it so that it meet a minimum time requirements.

#### Main Result 5: Duration of the HiddenSloth Operations on 🤖 Android

We reproduce the results from **Figure 6 (top)** and **Figure 7** which shows the duration of the HiddenSloth operations on Android for different devices.

#### Main Result 6: Duration of the HiddenSloth Operations on 🍏 iOS

We reproduce the results from **Figure 6 (bottom)** which shows the duration of the HiddenSloth operations on iOS for different devices.


### Experiments

The experiments are grouped based on the platforms and each covers multiple main results.

#### Experiment 1: 🤖 Android

We will run the experiments in an automated manner using our AWS Device Farm script and then analyze the data using the Jupyter notebooks.

1. Run the full benchmark suite using our "small" device pool.
```bash
$ cd android
$ source env/bin/activate
(env) $ python3 run_on_aws_devicefarm.py --device-pool small --rebuild-apks
```

2. This might take about 10 minutes to complete. However, it should not take much longer than the "single" device pool example from the setup instructions, as the devices are run in parallel. You can check the progress in the AWS Device Farm web console.

3. Note the **result folder** that is being created. E.g. `results/small-hw-support-2024-05-24-161053`.

4. Open the Jupyter notebooks.
```bash
(env) $ deactivate
$ cd ../evaluation
$ source env/bin/activate
(env) $ jupyter notebook
```

5. Open `01-analysis-android-op-perf.ipynb` and update the `RESULTS_DIR` variable in the second cell. Then run the entire notebook. This reproduces **Main Result 1: Duration of HMAC Operations on 🤖 Android**.

6. Open `03-analysis-android-long-sloth.ipynb` and update the `RESULTS_DIR` variable in the second cell. Then run the entire notebook. This reproduces **Main Result 3: Duration of the LongSloth.Derive Operations on 🤖 Android**. Note: the similarly named file `03-analysis-android-and-ios-sloth-perf` includes also the iOS code to create a combined plot.

7. Open `04-analysis-android-hidden-sloth.ipynb` and update the `RESULTS_DIR` variable in the second cell. Then run the entire notebook. This reproduces **Main Result 5: Duration of the HiddenSloth Operations on 🤖 Android**.

8. All done! 🎉


#### Experiment 2: 🍏 iOS

We will run the experiments using remote iOS devices on AWS Device Farm and manually executing them via the app. The results will be uploaded to our web service from which we then download them to analyze them using the Jupyter notebooks.

1. Ensure the web service is running as per the setup instructions above.

2. Prepare a _signed_ IPA archive in XCode to use with the AWS Device Farm. The resulting file should end in `.ipa` and contain a `Payload` directory which then contains a folder named `Sloth.app`. If this is not the case, check the build settings in XCode or manually unzip/rezip as needed to match the structure.

3. Create a new "Remote Session" within our `aec-sloth` project using the AWS web console. Select the iPhone device you want to use (see paper for the models we used). Then confirm and start.

⚠️ **Important:** make sure to end the session as soon as possible when done, as otherwise AWS will continue to charge!

4. Drag and drop the IPA file into the AWS Device Farm web console. This installs the IPA file and you can control it using the web interface.

If you get stuck, the [AWS Device Farm FAQ section](https://docs.aws.amazon.com/devicefarm/latest/developerguide/troubleshooting-ios-applications.html) contains helpful details for debugging the iOS layout.

5. Run the following tests with `iterations=10` using the UI controls:
  - Experiment: "SE-OP"
  - Experiment: "RainbowSloth.innerDerive" once for each Rainbow parameter available
  - Experiment: "HiddenSloth.ratchet" once for each maxSize parameter available

6. After each test completes, you should see respective log messages at the web service indicating that data has been uploaded.

7. **Repeat:** the steps 3-6 for each device you want to test.

8. Download the results from the web service to your local machine. Store them in the `results` folder under a name of your choosing, e.g. `/results/ios-2023-03-19`.

9. Open the Jupyter notebooks.
```bash
(env) $ deactivate
$ cd ../evaluation
$ source env/bin/activate
(env) $ jupyter notebook
```

10. Open `01-analysis-ios-op-perf.ipynb` and update the `RESULTS_DIR` variable in the second cell. Then run the entire notebook. This reproduces **Main Result 2: Duration of ECDH Operations on 🍏 iOS**.

11. Open `03-analysis-ios-rainbow-sloth` and update the `RESULTS_DIR` variable in the second cell. Then run the entire notebook. This reproduces **Main Result 4: Duration of the LongSloth.Derive Operations on 🍏 iOS**.

12. Open `04-analysis-ios-hidden-sloth.ipynb` and update the `RESULTS_DIR` variable in the second cell. Then run the entire notebook. This reproduces **Main Result 6: Duration of the HiddenSloth Operations on 🤖 iOS**.

13. All done! 🎉

## Limitations

The evaluation likely yields slightly different results than the paper as we have since updated the library.
However, they should be very similar for all practical concerns.
There are many additional tests for both Android and iOS that we do not cover here and which are mostly used for verifying correctness.
If interested, you can run them via the Android Studio or XCode IDEs using a physical device.

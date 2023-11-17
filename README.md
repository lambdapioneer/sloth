# 🦥 Sloth: Key Stretching and Deniable Encryption using Secure Elements on Smartphones

This repository contains the code and analysis scripts for the [Sloth paper]() (not yet published).
Sloth describes a set of cryptographic protocols that leverage the Secure Element (SE) of smartphones for key stretching and deniable encryption.

In particular, this repository provides the following:

- An Android library implementing:
  - The **LongSloth** key stretching scheme using StrongBox.
  - The **HiddenSloth** multi-snapshot deniable encryption scheme using StrongBox.
  - Benchmarking tests that can be run against emulators and real devices.
  - A Python script to automatically run these tests on AWS DeviceFarm against many real devices.
- An iOS demo project implementing:
  - The **RainbowSloth** key stretching scheme using the Secure Enclave.
  - A simple Python server to collect the results,
- The evaluation scripts used to generate the plots and tables in the paper.


## Abstract 📄

Traditional key stretching lacks a strict time guarantee due to the ease of parallelized password guessing by attackers.
This paper introduces Sloth, a key stretching method leveraging the Secure Element (SE) commonly found in modern smartphones to provide a strict rate limit on password guessing.
While this would be straightforward with full access to the SE, Android and iOS only provide a very limited API. 
Sloth utilizes the existing developer SE API and novel cryptographic constructions to build an effective rate-limit for password guessing on recent Android and iOS devices.
Our approach ensures robust security even for short, randomly-generated, six-character alpha-numeric passwords against adversaries with _virtually unlimited_ computing resources.
Our solution is compatible with approximately 96% of iPhones and 45% of Android phones and Sloth seamlessly integrates without device or OS modifications, making it immediately usable by app developers today.
We formally define the security of Sloth and evaluate its performance on various devices.
Finally, we present HiddenSloth, a deniable encryption scheme, leveraging Sloth and the SE to withstand multi-snapshot adversaries.


## This repository

[![Android](https://github.com/lambdapioneer/sloth/actions/workflows/android.yaml/badge.svg?branch=main)](https://github.com/lambdapioneer/sloth/actions/workflows/android.yaml)

This repository is organized into the following folders:

- [`android`](android/): The Android implementation of LongSloth and HiddenSloth.
- [`evaluation`](evaluation/): The analysis scripts used to generate the plots and tables in the paper.
- [`ios`](ios/): The iOS implementation of RainbowSloth. This is a demo project and not production-ready.
- [`results`](results/): An empty folder to store the collected results locally.

See the individual `README.md` files in the respective folders for more information.


## Bibtex 📚

TBD once published.
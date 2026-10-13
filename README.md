# Bafang M420 Android diagnostic prototype

Native Kotlin Android project. **Not an APK**. Does not communicate with the bicycle or diagnose motor behaviour yet.

## What works
- Enumerates Android USB host devices and endpoints
- Recognizes the documented Bafang BESST interface by VID:PID `0323:0627`
- Shares a USB inventory report
- Never transmits to the motor or changes controller settings

## What is missing
- Build and signing of an APK (requires an Android SDK build environment)
- Android USB HID permissions and transport driver for BESST
- Verified Bafang CAN protocol implementation and diagnostic parameter decoding
- Hardware validation with a genuine compatible BESST interface

## Build
Open this project with a compatible Android Gradle development environment, sync dependencies, and build the debug APK (`assembleDebug`). This cannot be installed by opening the ZIP file.

## Safety
USB-C-to-USB-A is only a connector adapter; do not wire it directly to the bicycle. Do not ride a bicycle whose motor continues to drive unexpectedly.

Protocol research: https://github.com/andrey-pr/OpenBafangTool


## USB permission update
The app now has a **Request USB device permission** button. Android will show a system prompt for the first enumerated USB device. After the user grants or denies permission, the app refreshes the USB inventory. It does not send data to the motor.

## Build on Android phone (no Windows)
AndroidIDE can open the extracted project folder and build a debug APK with its Quick Run command after installing its Android build toolchain. AndroidIDE upstream is archived and may not work on current Android versions. The project has **not** been compiled or device-tested here.

## Limitations
This version identifies USB devices and requests access only. No BESST transport, CAN decoding, error-code retrieval, or motor diagnosis has been implemented. A USB adapter alone cannot communicate with CAN.

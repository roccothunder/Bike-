# Build APK using GitHub cloud, with only an Android phone

This project is an Android USB interface **test**, NOT a working M420 motor fault-code reader.

1. On your phone, sign into github.com and create a new **private** repository (e.g. `bafang-m420-usb-test`).
2. Upload the **contents** of this project's folder to the repository root, including the hidden `.github/workflows/build-apk.yml` file. GitHub's browser upload may require selecting files individually and retaining paths; a Git client app can upload the folder as a complete repository instead.
3. Open the repository's **Actions** tab, select **Build Android APK**, and tap **Run workflow**. The workflow also starts when you push to `main` or `master`.
4. Wait for the build to finish. Open its run, scroll to **Artifacts**, and download `BafangM420-USB-test-APK`.
5. Unzip the downloaded artifact. Tap `app-debug.apk` on the phone to install.

No Windows PC is involved. The APK can enumerate connected USB devices and request USB permission; it cannot diagnose the motor without an implemented Bafang protocol driver and suitable physical interface.

Note: Android app installation requires user approval for the source of the APK.

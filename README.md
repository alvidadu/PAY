# MFS SopnoPay Android Source

- Package: `com.sms.sopnopay`
- Language: Kotlin
- Network: Volley
- Firebase: not used

## Build on GitHub Actions
1. Upload the contents of this project to your repository root (the `MFS_SMS_Gateway` folder contents, not an extra nested folder).
2. Ensure `.github/workflows/android.yml` is included.
3. Open **Actions** and run **Android APK Build** (or push to `main`/`master`).
4. When successful, download the artifact named `MFS-SopnoPay-debug-apk` from the workflow run.

This workflow installs Gradle directly, so it does not call `chmod +x gradlew` and does not require a `gradlew` file.

## Open in Android Studio
Open the `MFS_SMS_Gateway` directory and let Android Studio sync dependencies.

## Important
This is a starter source project and has not been compiled or tested in this environment. API response formats and server-side authentication must match your PHP backend. Android background execution can be restricted by device settings; SMS is processed only after Android delivers it. Never treat SMS text alone as definitive proof of payment—verify transactions with the payment provider/server.

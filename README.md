# Jarvis Android App

Automated continuous microphone ear & mechanical hand automation service.

## 🚀 How to build the APK

### Option A: 1-Click via GitHub Actions (No Android Studio required!)
1. Create a new repository on GitHub (private or public).
2. Push all the files in this zip to your GitHub repository.
3. Open GitHub in your browser, go to the **Actions** tab.
4. The **Build Jarvis APK** workflow will automatically run and compile the APK!
5. Download the `jarvis-debug-apk.zip` artifact containing your ready-to-install `app-debug.apk`.

### Option B: Using Android Studio
1. Open Android Studio -> **Open an Existing Project**.
2. Select this project root folder.
3. Wait for Gradle Sync to finish.
4. Click **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
5. Locate the APK in `app/build/outputs/apk/debug/app-debug.apk`.

### 📱 Enabling the Services on your Phone
1. Install the APK on your device.
2. Grant Microphone permission when prompted.
3. Go to **Settings -> Accessibility -> Installed Services / Downloaded Apps**.
4. Enable **Jarvis Automation Service**.
5. Start your AI server on your computer (`ws://<your-pc-ip>:8080`).
6. Tap **Start Jarvis Ear** in the app!

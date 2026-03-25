# Basic Android App

A simple Android application written in Kotlin that displays **"Hello World!"** when opened.

## Prerequisites

Before you can run this app, make sure you have the following installed:

- **[Android Studio](https://developer.android.com/studio)** (Hedgehog 2023.1.1 or newer recommended)
- **Java Development Kit (JDK) 8** or higher (bundled with Android Studio)
- **Android SDK** with API level 24 or higher (installed via Android Studio's SDK Manager)

## How to Open and Run the App

### 1. Clone the repository

```bash
git clone https://github.com/S1mse4/Basic-AndriodApp.git
cd Basic-AndriodApp
```

### 2. Open in Android Studio

1. Launch **Android Studio**.
2. On the welcome screen, click **"Open"** (or go to **File → Open…**).
3. Navigate to the cloned `Basic-AndriodApp` folder and click **OK**.
4. Wait for Gradle to sync — Android Studio will download all required dependencies automatically.

### 3. Run on an Emulator

1. In the toolbar, open the **Device Manager** (the phone icon, or **View → Tool Windows → Device Manager**).
2. Click **"Create Device"**, choose a hardware profile (e.g. *Pixel 6*), and select a system image with API level 24 or higher.
3. Click **Finish** to create the virtual device.
4. Select the emulator from the run target dropdown in the toolbar.
5. Click the green **▶ Run** button (or press `Shift + F10`).

### 4. Run on a Physical Device

1. On your Android phone, enable **Developer Options** (tap *Build number* 7 times in *Settings → About phone*).
2. Enable **USB Debugging** inside Developer Options.
3. Connect your phone to your computer via USB and accept the authorization prompt on the phone.
4. Select your device from the run target dropdown in Android Studio.
5. Click the green **▶ Run** button (or press `Shift + F10`).

## What You Will See

When the app launches, you will see a white screen with **"Hello World!"** displayed in the center.

## Project Structure

```
app/src/main/
├── AndroidManifest.xml               # App manifest (entry point declaration)
├── java/com/example/basicandroidapp/
│   └── MainActivity.kt               # Main screen logic
└── res/
    ├── layout/activity_main.xml      # UI layout (Hello World TextView)
    └── values/
        ├── strings.xml               # String resources
        ├── colors.xml                # Color resources
        └── themes.xml                # App theme
```
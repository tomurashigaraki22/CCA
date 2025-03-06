# CCA: Camera Capture Application

Capture synchronized images from multiple cameras on your mobile device. 📸

## Description

CCA is a React Native application leveraging native modules to capture images simultaneously from multiple cameras. It provides a seamless way to access and control camera hardware directly from JavaScript, enhancing performance and flexibility.

## Installation

Get started by following these steps:

1.  **Clone the repository:**

    ```bash
    git clone https://github.com/your-username/CCA.git
    cd CCA
    ```

2.  **Install dependencies:**

    ```bash
    npm install
    ```

3.  **Install CocoaPods dependencies for iOS:**

    ```bash
    cd ios && pod install && cd ..
    ```

4.  **Configure Android local properties:**

    Create or update `android/local.properties` with your Android SDK location:

    ```properties
    sdk.dir=/path/to/your/android/sdk
    ```

## Usage

### Running the application

1.  **Start the Metro bundler:**

    ```bash
    npm start
    ```

2.  **Run on Android:**

    ```bash
    npm run android
    ```

    or

3.  **Run on iOS:**

    ```bash
    npm run ios
    ```

### Taking Pictures

1.  Grant camera permissions when prompted by the application.
2.  The application displays previews from available back cameras.
3.  Tap the capture button to take simultaneous pictures from all cameras.
4.  Captured images are saved to the device's external storage and the paths displayed.

## Features

*   📸 **Simultaneous Capture**: Capture images from multiple back cameras at the same time.
*   📱 **Native Performance**: Utilizes native modules for efficient camera control.
*   🛠️ **Configurable**: Easily customizable through React Native components and native code.
*   🖼️ **Camera Preview**: Provides real-time camera previews.
*   ✔️ **Permissions Handling**: Handles camera permissions gracefully on both Android and iOS.

## Technologies Used

| Technology          | Description                                                                                                 |
| :------------------ | :---------------------------------------------------------------------------------------------------------- |
| React Native        | Cross-platform mobile application framework.                                                              |
| Ruby                | Language for CocoaPods                                                                                     |
| Native Modules      | Access native device features and APIs.                                                                    |
| Camera2 API (Android) | Advanced camera control for Android devices.                                                            |
| Swift (iOS)         | Native language for iOS camera implementations                                                           |
| Gradle              | Build automation system for Android                                                                       |
| CocoaPods (iOS)    | Dependency manager for Swift and Objective-C Cocoa projects                                                  |
| JavaScript
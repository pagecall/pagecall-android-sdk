# pagecall-android-sdk

This SDK provides an easy-to-integrate solution for embedding the Pagecall webapp into your Android applications using the PagecallWebView class, which extends the native WebView class. With the added bridge between WebView and the native environment, we enable seamless audio functionality for an enhanced user experience.

For other platforms, please refer to https://docs.pagecall.com

## Requirements

- Android API level 21 or higher
- AndroidX compatibility

## Installation

1. Add the following to your project's build.gradle file:
```gradle
allprojects {
    repositories {
        ...
        maven {
            url 'https://maven.pkg.github.com/pagecall/pagecall-android-sdk'
            credentials {
                username = project.findProperty("GITHUB_USERNAME") ?: System.getenv("GITHUB_USERNAME")
                password = project.findProperty("GITHUB_TOKEN") ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
```

Github authentication is required, as this SDK is currently uploaded on Github Packages.
The `credentials` field is necessary for this purpose. The required permission in this case is `read:packages`.

2. Add the dependency to your app's build.gradle file:
```gradle
dependencies {
    implementation 'com.pagecall:pagecall-android-sdk:0.0.13' // Recommended to use the latest
}
```
3. Sync your project with the Gradle files.

## Usage

1. In your Android project, import the PagecallWebView class:
```java
import com.pagecall.PagecallWebView;
```
2. Replace your WebView instances with PagecallWebView instances:
```java
PagecallWebView webView = new PagecallWebView(this);
```
3. Load the Pagecall webapp URL:
```java
webView.loadUrl("https://app.pagecall.com/meet?room_id={room_id}&access_token={access_token}");
```
4. Don't forget to add the required permissions in your `AndroidManifest.xml`:
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
```

The current version of this SDK does not detect when a user denies permissions. Please ensure that permissions are granted before entering a room.

## Support

For any issues, bug reports, or feature requests, please open an issue in this repository or contact our support team at support@pagecall.com.

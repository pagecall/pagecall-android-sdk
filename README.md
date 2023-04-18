# pagecall-android-sdk

This SDK provides an easy-to-integrate solution for embedding the Pagecall webapp into your Android applications using the PagecallWebView class, which extends the native WebView class. With the added bridge between WebView and the native environment, we enable seamless audio functionality for an enhanced user experience.

## Requirements

- Android API level 21 or higher
- AndroidX compatibility

## Installation

1. Add the following to your project's build.gradle file:
```gradle
allprojects {
    repositories {
        ...
        maven { url 'https://a.b' }
    }
}
```
2. Add the dependency to your app's build.gradle file:
```gradle
dependencies {
    implementation 'com.github.pagecall:pagecall-android-sdk:0.0.1'
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

## Support

For any issues, bug reports, or feature requests, please open an issue in this repository or contact our support team at support@pagecall.com.

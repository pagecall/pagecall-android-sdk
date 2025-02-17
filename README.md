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
    implementation 'com.pagecall:pagecall-android-sdk:0.0.47' // Recommended to use the latest
}
```
3. Sync your project with the Gradle files.

## Usage

#### Activity (Java)

1. In your Android project, import the PagecallWebView class:
```java
import com.pagecall.PagecallWebView;
```
2. Replace your WebView instances with PagecallWebView instances:
```java
PagecallWebView webView = new PagecallWebView(this);
```
3. Please delegate `onActivityResult(...)` to enable file upload functionality:
```java
// MainActivity.java
@Override
public void onActivityResult(int requestCode, int resultCode, Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);
    webView.onActivityResult(requestCode, resultCode, intent);
}

```
4. Load the Pagecall webapp URL:
```java
// meet mode
webView.load("{room_id}", "{token}", PagecallWebView.PagecallMode.MEET);
// replay mode
webView.load("{room_id}", "{token}", PagecallWebView.PagecallMode.REPLAY);
```
5. Don't forget to add the required permissions in your `AndroidManifest.xml`:
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
```
The current version of this SDK does not detect when a user denies permissions. Please ensure that permissions are granted before entering a room.

6. You can listen to some events (Loaded, Message, Terminated) from PagecallWebView by implementing PagecallWebView. Listener delegate interface and passing to PagecallWebView.setListener(listenerImpl) See [MainActivity.java](/sample/src/main/java/com/pagecall/sample/MainActivity.java) in the sample app.

#### Layout (XML)

```xml
<com.pagecall.PagecallWebView
    android:id="@+id/webview"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

## Support

For any issues, bug reports, or feature requests, please open an issue in this repository or contact our support team at support@pagecall.com.

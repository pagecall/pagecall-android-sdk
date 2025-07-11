package com.pagecall;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.MutableContextWrapper;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

// TODO: package private
final public class PagecallWebView extends WebView {

    public interface Listener {
        default void onLoadStateChange(String message) {};
        default void onLoaded() {};
        default void onMessage(String message) {};
        default void onEvent(JSONObject payload) {};
        default void onTerminated(TerminationReason reason) {};
        default void onError(PagecallError error) {};
        default void onWillNavigate(String url) {};
    }

    public enum PagecallMode {
        MEET("meet"),
        REPLAY("replay");

        private final String value;

        PagecallMode(String value) {
            this.value = value;
        }

        public static PagecallMode fromString(String modeStr) {
            for (PagecallMode mode : PagecallMode.values()) {
                if (mode.toString().equalsIgnoreCase(modeStr)) {
                    return mode;
                }
            }
            throw new IllegalArgumentException("Invalid enum value: " + modeStr);
        }

        public String getBaseURLString() {
            switch (this.value) {
                case "meet":
                    return "https://app.pagecall.com/meet";
                case "replay":
                    return "https://app.pagecall.com/replay";
                default:
                    throw new IllegalArgumentException("Unsupported PagecallMode");
            }
        }
    }

    private Listener listener;

    final static String version = "0.0.50";

    private final static String[] defaultPagecallUrls = {"app.pagecall", "demo.pagecall", "192.168"};
    private final static String jsInterfaceName = "pagecallAndroidBridge";
    private HashMap<String, Consumer<String>> subscribers = new HashMap<>();

    private String[] pagecallUrls = null;
    private NativeBridge nativeBridge = null;

    private Context context;

    public PagecallWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (android.os.Build.VERSION.SDK_INT >= 24) {
            init(context);
        }
        this.context = context;
    }

    public PagecallWebView(Context context) {
        super(context);
        if (android.os.Build.VERSION.SDK_INT >= 24) {
            init(context);
        }
        this.context = context;
    }

    @Override
    public void setWebChromeClient(WebChromeClient client) {
        throw new UnsupportedOperationException("PagecallWebView does not support setWebChromeClient");
    }

    @Override
    public void setWebViewClient(WebViewClient client) {
        throw new UnsupportedOperationException("PagecallWebView does not support setWebViewClient");
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void load(@NonNull String roomId, @NonNull String accessToken, @NonNull PagecallMode mode) {
        this.load(roomId, accessToken, mode, new HashMap<>());
    }

    public void load(@NonNull String roomId, @NonNull String accessToken, @NonNull PagecallMode mode, @Nullable HashMap<String, String> queryItems) {
        if (listener != null) {
            listener.onLoadStateChange("Loading room: " + roomId + " with access token: " + accessToken + " in mode: " + mode);
        }
        Uri baseUri = Uri.parse(mode.getBaseURLString());
        Uri.Builder uriBuilder = baseUri.buildUpon()
                .appendQueryParameter("room_id", roomId)
                .appendQueryParameter("access_token", accessToken);
        if (queryItems != null && !queryItems.isEmpty()) {
            for (Map.Entry<String, String> entry : queryItems.entrySet()) {
                uriBuilder.appendQueryParameter(entry.getKey(), entry.getValue());
            }
        }

        super.loadUrl(uriBuilder.toString());
    }

    /**
     * @deprecated use load(roomId, accessToken, mode) instead.
     */
    @Override
    @Deprecated
    public void loadUrl(@NonNull String url) {
        super.loadUrl(url);
    }

    /**
     * @deprecated use load(roomId, accessToken, mode) instead.
     */
    @Override
    @Deprecated
    public void loadUrl(@NonNull String url, @NonNull Map<String, String> additionalHttpHeaders) {
        super.loadUrl(url, additionalHttpHeaders);
    }

    /**
     * @deprecated use load(roomId, accessToken, mode) instead.
     */
    @Override
    @Deprecated
    public void loadData(@NonNull String data, @Nullable String mimeType, @Nullable String encoding) {
        super.loadData(data, mimeType, encoding);
    }

    /**
     * @deprecated use load(roomId, accessToken, mode) instead.
     */
    @Override
    @Deprecated
    public void loadDataWithBaseURL(@Nullable String baseUrl, @NonNull String data, @Nullable String mimeType, @Nullable String encoding, @Nullable String historyUrl) {
        super.loadDataWithBaseURL(baseUrl, data, mimeType, encoding, historyUrl);
    }

    private PagecallWebChromeClient webChromeClient;

    private void sendChatForDebug(String message) {
        String escapedMessage = message.replace("'", "\\'");
        this.evaluateJavascriptWithLog("Pagecall.chat.sendMessage('" + escapedMessage + "');");
    }

    private void updateCommunicationDevice() {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        List<AudioDeviceInfo> devices = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? audioManager.getAvailableCommunicationDevices() : Arrays.asList(audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS));

        // 1. Bluetooth
        AudioDeviceInfo bluetoothDevice = null;
        for (AudioDeviceInfo deviceInfo : devices) {
            if (deviceInfo.getType() == AudioDeviceInfo.TYPE_BLE_HEADSET || deviceInfo.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
                bluetoothDevice = deviceInfo;
                break;
            }
        }
        if (bluetoothDevice != null) {
            audioManager.startBluetoothSco();
            audioManager.setBluetoothScoOn(true);
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                audioManager.setCommunicationDevice(bluetoothDevice);
            }
            if (this.nativeBridge != null) {
                this.nativeBridge.log("deviceChange", "Bluetooth output detected: " + bluetoothDevice.getProductName());
            }
            return;
        } else {
            audioManager.stopBluetoothSco();
        }

        // 2. External devices (Headsets, earpieces, ...)
        AudioDeviceInfo externalDevice = null;
        for (AudioDeviceInfo deviceInfo : devices) {
            if (deviceInfo.getType() != AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) {
                externalDevice = deviceInfo;
                break;
            }
        }
        if (externalDevice != null) {
            audioManager.setSpeakerphoneOn(false);
            audioManager.setMode(AudioManager.MODE_NORMAL); // Using MODE_IN_COMMUNICATION routes to builtin speaker in Android 10
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                audioManager.setCommunicationDevice(externalDevice);
            }
            if (this.nativeBridge != null) {
                this.nativeBridge.log("deviceChange", "External output detected: " + externalDevice.getProductName());
            }
            return;
        }

        // 3. Builtin
        AudioDeviceInfo builtinDevice = devices.get(0);
        audioManager.setSpeakerphoneOn(true);
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.clearCommunicationDevice();
        }
        if (this.nativeBridge != null) {
            this.nativeBridge.log("deviceChange", builtinDevice != null ? "Builtin speaker: " + builtinDevice.getProductName() : "Default");
        }
    }

    final AudioDeviceCallback audioDeviceCallback = new AudioDeviceCallback() {
        @Override
        public void onAudioDevicesAdded(AudioDeviceInfo[] addedDevices) {
            updateCommunicationDevice();
        }

        @Override
        public void onAudioDevicesRemoved(AudioDeviceInfo[] removedDevices) {
            updateCommunicationDevice();
        }
    };

    protected void init(Context context) {
        // Check if the context is ActivityContext or MutableContextWrapper
        if (!(context instanceof Activity) && !(context instanceof MutableContextWrapper)) {
            throw new IllegalArgumentException("The provided context is neither an Activity nor a MutableContextWrapper.");
        }
        if (this.pagecallUrls == null) {
            pagecallUrls = defaultPagecallUrls;
        }
        this.getSettings().setJavaScriptEnabled(true);
        this.getSettings().setDomStorageEnabled(true);
        this.getSettings().setMediaPlaybackRequiresUserGesture(false);
        String userAgent = this.getSettings().getUserAgentString();

        this.getSettings().setUserAgentString(userAgent + " PagecallAndroidSDK/" + version);


        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, null);

        if (nativeBridge == null) nativeBridge = new NativeBridge(this, subscribers);
        nativeBridge.listenBridgeMessages(jsonMessage -> {
            if (this.listener == null) return;
            String action = jsonMessage.optString("action", "");
            NativeBridgeAction bridgeAction = NativeBridgeAction.fromString(action);

            if (bridgeAction == NativeBridgeAction.REQUEST_AUDIO_VOLUME) return;

            switch (bridgeAction) {
                case LOADED: {
                    this.listener.onLoaded();
                    this.nativeBridge.log("initialDevice", "Triggering updateCommunicationDevice to log initial device");
                    updateCommunicationDevice();
                    break;
                }
                case TERMINATED: {
                    JSONObject payload = jsonMessage.optJSONObject("payload");
                    String reasonString = payload.optString("reason");
                    TerminationReason reason = TerminationReason.fromString(reasonString);
                    this.listener.onTerminated(reason);
                    break;
                }
                case MESSAGE: {
                    JSONObject payload = jsonMessage.optJSONObject("payload");
                    String message = payload.optString("message");
                    this.listener.onMessage(message);
                    break;
                }
                case EVENT: {
                    JSONObject payload = jsonMessage.optJSONObject("payload");
                    this.listener.onEvent(payload);
                    break;
                }
            }
        });
        this.addJavascriptInterface(nativeBridge, jsInterfaceName);

        super.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Log.d("PagecallWebView", "shouldOverrideUrlLoading: " + request.getUrl().toString());
                if (request.isForMainFrame() && listener != null) {
                    listener.onWillNavigate(request.getUrl().toString());
                }
                return super.shouldOverrideUrlLoading(view, request);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                Log.d("PagecallWebView", "Navigation started: " + url);
                if (listener != null) {
                    listener.onLoadStateChange("Navigation started: " + url);
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (listener != null) {
                    listener.onLoadStateChange("Navigation finished: " + url);
                }
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O) {
                    // Do not evaluate native code under android 8. It will use web sdk.
                    return;
                }
                if (isUrlContainsPagecallUrl(url)) {
                    String jsCode = getNativeJS();
                    view.evaluateJavascript(jsCode, null);
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (listener != null) {
                    listener.onError(new PagecallError(error.getDescription().toString()));
                }
            }

            @Override
            public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (!detail.didCrash()) {
                        Log.e("PagecallWebView", "System killed the WebView rendering process to reclaim memory.");
                        if (listener != null) {
                            listener.onError(new PagecallError("Out of memory"));
                        }
                        return true; // The app continues executing.
                    }
                }
                Log.e("PagecallWebView", "Renderer crashed because of an internal error, such as a memory access violation.");
                if (listener != null) {
                    listener.onError(new PagecallError("Render process has gone"));
                }
                return false;
            }
        });

        this.webChromeClient = new PagecallWebChromeClient(this);
        super.setWebChromeClient(this.webChromeClient);
    }

    public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
        if (this.webChromeClient != null) {
            this.webChromeClient.handleActivityResult(requestCode, resultCode, intent);
        }
    }

    private boolean isUrlContainsPagecallUrl(String url) {
        for (String pagecallUrl : pagecallUrls) {
            if (url.contains(pagecallUrl)) {
                return true;
            }
        }
        return false;
    }

    private void evaluateJavascriptWithLog(String script) {
        String finalScript = "(function userScript(){" + script + "})()";
        this.post(() -> this.evaluateJavascript(finalScript, value -> {
            // todo: handle result
        }));
    }

    public void sendMessage(String message) {
        String script = MessageFormat.format(
                "if (!window.Pagecall) return false; window.Pagecall.sendMessage(\"{0}\"); return true;"
                , message
        );
        this.evaluateJavascriptWithLog(script);
    }

    private void setValueRaw(String key, Object value) {
        final String script =
                "(function(window) {" +
                "  try {" +
                "    window.PagecallUI?.set('" + key + "', " + value + ");" +
                "    return 'Success';" +
                "  } catch (e) {" +
                "    return 'Error: ' + e.message;" +
                "  }" +
                "})(window);";

        evaluateJavascript(script, returnValue -> {
            if (returnValue.startsWith("\"Error:")) {
                Log.e("PagecallWebView", "setValueRaw script error: " + returnValue);
            }
        });
    }

    public void setValue(String key, String value) {
        setValueRaw(key, "\"" + value + "\"");
    }

    public void setValue(String key, Number value) {
        setValueRaw(key, value);
    }

    private final String subscriptionsStorageName = "__pagecallNativeSubscriptions";

    private Runnable subscribe(String target, Consumer<String> subscriber) {
        String id = UUID.randomUUID().toString();
        subscribers.put(id, subscriber);
        String returningScript = String.format(
                "const callback = (value) => {" +
                        "  window.%s.postMessage(JSON.stringify({" +
                        "    type: \"subscription\"," +
                        "    payload: {" +
                        "      id: \"%s\"," +
                        "      value" +
                        "    }" +
                        "  }));" +
                        "};" +
                        "const subscription = %s.subscribe(callback);" +
                        "if (!window[\"%s\"]) window[\"%s\"] = {};" +
                        "window[\"%s\"][\"%s\"] = subscription;",
                jsInterfaceName, id, target, subscriptionsStorageName, subscriptionsStorageName, subscriptionsStorageName, id);
        evaluateJavascriptWithLog(returningScript);

        return () -> {
            String script = String.format("window[\"%s\"][\"%s\"].unsubscribe();", subscriptionsStorageName, id);
            evaluateJavascriptWithLog(script);
            subscribers.remove(id);
        };
    }

    private String getNativeJS() {
        String jsCode = null;
        AssetManager assetManager = getContext().getAssets();
        try {
            // Use txt to prevent react-native from including js file into its bundle
            InputStream inputStream = assetManager.open("js/PagecallNative.js.txt");
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();
            jsCode = new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return jsCode;
    }

    private void destroyBridge() {
        this.removeJavascriptInterface(jsInterfaceName);
        if (nativeBridge != null) {
            this.nativeBridge.destroy();
            this.nativeBridge = null;
        }
    }

    @Override
    public void destroy() {
        evaluateJavascript("Pagecall.terminate()", value -> super.destroy());
        destroyBridge();

        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        audioManager.unregisterAudioDeviceCallback(audioDeviceCallback);
        audioManager.stopBluetoothSco();
        audioManager.setMode(AudioManager.MODE_NORMAL);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.clearCommunicationDevice();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        destroyBridge();
    }
}

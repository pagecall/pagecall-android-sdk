package com.pagecall;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.util.AttributeSet;
import android.view.KeyEvent;
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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

// TODO: package private
final public class PagecallWebView extends WebView {

    public interface Listener {
        void onLoaded();
        void onMessage(String message);
        void onTerminated(TerminationReason reason);
        void onError(WebResourceError error);
    }

    public enum PagecallMode {
        MEET("meet"),
        REPLAY("replay")
        ;

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
            switch(this.value) {
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

    final static String version = "0.0.30-rc0";

    private final static String[] defaultPagecallUrls = {"app.pagecall", "demo.pagecall", "192.168"};
    private final static String jsInterfaceName = "pagecallAndroidBridge";
    private HashMap<String, Consumer<String>> subscribers = new HashMap<>();

    private String[] pagecallUrls = null;
    private NativeBridge nativeBridge = null;

    private Context context;
    private Boolean isChime = false;

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

    public boolean handleVolumeKeys(int keyCode, KeyEvent event) {
        if (!this.isChime) return false;
        // chime일 때만 아래 코드를 실행
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_RAISE,
                        AudioManager.FLAG_SHOW_UI);
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_LOWER,
                        AudioManager.FLAG_SHOW_UI);
                return true;
            default:
                return false;
        }
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void load(@NonNull String roomId, @NonNull String accessToken, @NonNull PagecallMode mode) {
        this.load(roomId, accessToken, mode, new HashMap<>());
    }
    public void load(@NonNull String roomId, @NonNull String accessToken, @NonNull PagecallMode mode, @Nullable HashMap<String, String> queryItems) {
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
    @Override @Deprecated
    public void loadUrl(@NonNull String url, @NonNull Map<String, String> additionalHttpHeaders) {
        super.loadUrl(url, additionalHttpHeaders);
    }

    /**
     * @deprecated use load(roomId, accessToken, mode) instead.
     */
    @Override @Deprecated
    public void loadData(@NonNull String data, @Nullable String mimeType, @Nullable String encoding) {
        super.loadData(data, mimeType, encoding);
    }

    /**
     * @deprecated use load(roomId, accessToken, mode) instead.
     */
    @Override @Deprecated
    public void loadDataWithBaseURL(@Nullable String baseUrl, @NonNull String data, @Nullable String mimeType, @Nullable String encoding, @Nullable String historyUrl) {
        super.loadDataWithBaseURL(baseUrl, data, mimeType, encoding, historyUrl);
    }

    private PagecallWebChromeClient webChromeClient;

    protected void init(Context context) {
        if (this.pagecallUrls == null) {
            pagecallUrls = defaultPagecallUrls;
        }
        this.getSettings().setJavaScriptEnabled(true);
        this.getSettings().setDomStorageEnabled(true);
        this.getSettings().setMediaPlaybackRequiresUserGesture(false);
        String userAgent = this.getSettings().getUserAgentString();

        this.getSettings().setUserAgentString(userAgent + " PagecallAndroidSDK/" + version);

        if (nativeBridge == null) nativeBridge = new NativeBridge(this, subscribers);
        nativeBridge.listenBridgeMessages(jsonMessage -> {
            if (this.listener == null) return;
            String action = jsonMessage.optString("action", "");
            NativeBridgeAction bridgeAction = NativeBridgeAction.fromString(action);

            if (bridgeAction == NativeBridgeAction.REQUEST_AUDIO_VOLUME) return;

            switch (bridgeAction) {
                case LOADED: {
                    this.listener.onLoaded();
                    this.post(() -> this.evaluateJavascript("!!Pagecall.media.chimeSession$", value -> {
                        if ("true".equals(value)) {
                            // Chime
                            this.isChime = true;
                            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                        } else {
                            // MI
                        }
                    }));
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
            }
        });
        this.addJavascriptInterface(nativeBridge, jsInterfaceName);

        super.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
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
                    listener.onError(error);
                }
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
        String finalScript = "(function userScript(){"+ script + "})()";
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
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        destroyBridge();
    }
}

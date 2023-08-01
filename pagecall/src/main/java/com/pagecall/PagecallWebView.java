package com.pagecall;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.UUID;
import java.util.function.Consumer;

// TODO: package private
public class PagecallWebView extends WebView {

    public interface Listener {
        void onLoaded();
        void onMessage(String message);
        void onTerminated(TerminationReason reason);
    }

    private Listener listener;

    final static String version = "0.0.22";

    private final static String[] defaultPagecallUrls = {"app.pagecall", "demo.pagecall", "192.168"};
    private final static String jsInterfaceName = "pagecallAndroidBridge";
    private HashMap<String, Consumer<String>> subscribers = new HashMap<>();

    private String[] pagecallUrls = null;
    private NativeBridge nativeBridge = null;

    private Context context;
    private Boolean isChime = false;

    public PagecallWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
        this.context = context;
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

    public PagecallWebView(Context context) {
        super(context);
        init(context);
        this.context = context;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    private PagecallWebChromeClient webChromeClient;

    protected void init(Context context) {
        if (this.pagecallUrls == null) {
            pagecallUrls = defaultPagecallUrls;
        }
        this.getSettings().setJavaScriptEnabled(true);
        this.getSettings().setDomStorageEnabled(true);
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

        this.setWebViewClient(new WebViewClient() {
            public void onPageFinished(WebView view, String url) {
                if (isUrlContainsPagecallUrl(url)) {
                    String jsCode = getNativeJS();
                    view.evaluateJavascript(jsCode, null);
                }
            }
        });
        this.webChromeClient = new PagecallWebChromeClient(this);
        this.setWebChromeClient(this.webChromeClient);
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

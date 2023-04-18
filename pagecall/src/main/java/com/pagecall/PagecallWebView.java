package com.pagecall;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetManager;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

// TODO: package private
public class PagecallWebView extends WebView {
    private final static String defaultPagecallUrl = "app.pagecall";
    private final static String jsInterfaceName = "pagecallAndroidBridge";

    private String pagecallUrl = null;
    private NativeBridge nativeBridge = null;

    public PagecallWebView(Context context, String pagecallUrl) {
        this(context);
        this.pagecallUrl = pagecallUrl;
    }

    @SuppressLint("SetJavaScriptEnabled")
    public PagecallWebView(Context context) {
        super(context);

        if (this.pagecallUrl == null) {
            pagecallUrl = defaultPagecallUrl;
        }

        this.getSettings().setJavaScriptEnabled(true);
        this.getSettings().setDomStorageEnabled(true);
        String userAgent = this.getSettings().getUserAgentString();
        this.getSettings().setUserAgentString(userAgent + " Pagecall");

        if (nativeBridge == null) nativeBridge = new NativeBridge(this);
        this.addJavascriptInterface(nativeBridge, jsInterfaceName);

        this.setWebViewClient(new WebViewClient() {
            public void onPageFinished(WebView view, String url) {
                if (url.contains(pagecallUrl)) {
                    String jsCode = getNativeJS();
                    view.evaluateJavascript(jsCode, null);
                }
            }
        });
        this.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                request.grant(request.getResources());
            }
        });
    }

    private String getNativeJS() {
        String jsCode = null;
        AssetManager assetManager = getContext().getAssets();
        try {
            InputStream inputStream = assetManager.open("js/PagecallNative.js");
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


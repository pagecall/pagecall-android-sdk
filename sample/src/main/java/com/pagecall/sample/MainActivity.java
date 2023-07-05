package com.pagecall.sample;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.pagecall.PagecallWebView;
import com.pagecall.TerminationReason;

public class MainActivity extends AppCompatActivity implements PagecallWebView.Listener {

    private Button toggleButton;
    private Button sendButton;
    private PagecallWebView webView;
    private RelativeLayout mainLayout;
    private FrameLayout webViewContainer;
    private boolean isWebViewVisible;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PagecallWebView.setWebContentsDebuggingEnabled(true);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        toggleButton = findViewById(R.id.toggle_button);
        sendButton = findViewById(R.id.send_button);

        mainLayout = findViewById(R.id.main_layout);
        webViewContainer = findViewById(R.id.webview_container);

        isWebViewVisible = false;

        toggleButton.setOnClickListener(v -> {
            if (isWebViewVisible) {
                // Remove the WebView from the container
                webViewContainer.removeAllViews();
                webView.destroy();
                webView = null;
                isWebViewVisible = false;
            } else {
                // Create and show the WebView
                webView = new PagecallWebView(MainActivity.this);
                webView.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
                webView.loadUrl("https://demo.pagecall.net/join/six-canvas/230531abcjurung?build=latest&chime=0");
                webView.setListener(this);
                webViewContainer.addView(webView);
                isWebViewVisible = true;
            }
        });
        sendButton.setOnClickListener(v -> {
            if (webView != null) {
                webView.sendMessage("'hihi'");
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        webView.onActivityResult(requestCode, resultCode, intent);
    }

    // PagecallWebView.Listener implementations
    @Override
    public void onLoaded() {
        Log.d("SampleApp", "Room is loaded");
    }

    @Override
    public void onMessage(String message) {
        Log.d("SampleApp", "Message received: " + message);
    }

    @Override
    public void onTerminated(TerminationReason reason) {
        if (reason == TerminationReason.INTERNAL) {
            Log.d("SampleApp", "Terminated with internal reason");
        }
        if (reason == TerminationReason.OTHER) {
            Log.d("SampleApp", "Terminated with other reason (" + reason.getOtherReason() +")");
        }
    }
}

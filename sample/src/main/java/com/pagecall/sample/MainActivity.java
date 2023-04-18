package com.pagecall.sample;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.pagecall.PagecallWebView;

public class MainActivity extends AppCompatActivity {

    private Button toggleButton;
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
        mainLayout = findViewById(R.id.main_layout);
        webViewContainer = findViewById(R.id.webview_container);

        isWebViewVisible = false;

        toggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isWebViewVisible) {
                    // Remove the WebView from the container
                    webViewContainer.removeAllViews();
                    webView.destroy();
                    webView = null;
                    isWebViewVisible = false;
                } else {
                    // Create and show the WebView
                    webView = new PagecallWebView(MainActivity.this, "demo.pagecall");
                    webView.setLayoutParams(new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT));
                    webView.loadUrl("https://demo.pagecall.net/join/six-canvas/230321abcjurung?build=latest&chime=0");
                    webViewContainer.addView(webView);
                    isWebViewVisible = true;
                }
            }
        });
    }
}

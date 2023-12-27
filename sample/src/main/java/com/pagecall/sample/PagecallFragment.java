package com.pagecall.sample;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceError;
import android.widget.FrameLayout;

import androidx.appcompat.widget.Toolbar;

import com.pagecall.PagecallWebView;
import com.pagecall.TerminationReason;

import java.util.HashMap;

public class PagecallFragment extends Fragment implements PagecallWebView.Listener {
    private PagecallWebView webView;
    private FrameLayout webViewContainer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PagecallWebView.setWebContentsDebuggingEnabled(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pagecall, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(PagecallFragment.this).navigate(R.id.action_pagecallFragment_to_homeFragment);
            }
        });
        webViewContainer = view.findViewById(R.id.webview_container);
        webView = new PagecallWebView(view.getContext());
        webView.setListener(this);
        webView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        webViewContainer.addView(webView);

        PagecallFragmentArgs args = PagecallFragmentArgs.fromBundle(getArguments());
        String mode = args.getMode();
        String roomId = args.getRoomId();
        String accessToken = args.getAccessToken();
        String query = args.getQuery();

        if (!query.isEmpty()) {
            // parse query parameter string to HashMap<String, String>
            HashMap<String, String> queryItems = new HashMap<>();
            String[] params = query.split("&");
            for (String param : params) {
                String[] keyValue = param.split("=");
                if (keyValue.length == 2) {
                    String key = keyValue[0];
                    String value = keyValue[1];
                    queryItems.put(key, value);
                }
            }
            webView.load(roomId, accessToken, PagecallWebView.PagecallMode.fromString(mode), queryItems);
        } else {
            webView.load(roomId, accessToken, PagecallWebView.PagecallMode.fromString(mode));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        webView.destroy();
        webView = null;
        webViewContainer.removeAllViews();
    }

    public void processActivityResult(int requestCode, int resultCode, Intent intent) {
        // handle file upload
        webView.onActivityResult(requestCode, resultCode, intent);
    }

    public boolean processKeyDown(int keyCode, KeyEvent event) {
        if (webView.handleVolumeKeys(keyCode, event)) {
            return true;
        }
        return false;
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
            Log.d("SampleApp", "Terminated with other reason (" + reason.getOtherReason() + ")");
        }
        NavHostFragment.findNavController(PagecallFragment.this).navigate(R.id.action_pagecallFragment_to_homeFragment);
    }

    @Override
    public void onError(WebResourceError error) {
        Log.d("SampleApp", "Error occurred " + error.getDescription());
    }
}
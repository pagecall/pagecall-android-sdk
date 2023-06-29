package com.pagecall;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcelable;
import android.webkit.MimeTypeMap;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import java.util.ArrayList;

public class PagecallWebChromeClient extends WebChromeClient {
    public final static int IMAGE_SELECTOR_REQ = 1;

    private PagecallWebView mWebView;

    private ValueCallback mFilePathCallback;

    public PagecallWebChromeClient(PagecallWebView webView) {
        this.mWebView = webView;
    }

    @Override
    public void onPermissionRequest(final PermissionRequest request) {
        request.grant(request.getResources());
    }

    @Override
    public boolean onShowFileChooser(WebView webView, ValueCallback filePathCallback, FileChooserParams fileChooserParams) {
        String[] acceptTypes = fileChooserParams.getAcceptTypes();
        boolean allowMultiple = fileChooserParams.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE;

        mFilePathCallback = filePathCallback;
        ArrayList<Parcelable> extraIntents = new ArrayList<>();
        Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
        Intent fileSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
        fileSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
        fileSelectionIntent.setType(MimeType.DEFAULT.value);
        fileSelectionIntent.putExtra(Intent.EXTRA_MIME_TYPES, getAcceptedMimeType(acceptTypes));
        fileSelectionIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultiple);
        chooserIntent.putExtra(Intent.EXTRA_INTENT, fileSelectionIntent);
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, extraIntents.toArray(new Parcelable[]{}));

        ((Activity) this.mWebView.getContext()).startActivityForResult(chooserIntent, IMAGE_SELECTOR_REQ);

        return true;
    }

    public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
        if (requestCode == IMAGE_SELECTOR_REQ) {
            if (resultCode == Activity.RESULT_OK) {
                if (intent != null && mFilePathCallback != null) {
                    mFilePathCallback.onReceiveValue(getSelectedFiles(intent, resultCode));
                    mFilePathCallback = null;
                }
            }
            else {
                // Occurred if pressed back button without selecting files
                if (mFilePathCallback != null) {
                    mFilePathCallback.onReceiveValue(null);
                }
            }
        }
    }


    private enum MimeType {
        DEFAULT("*/*"),
        IMAGE("image"),
        VIDEO("video");

        private final String value;

        MimeType(String value) {
            this.value = value;
        }
    }

    private String[] getAcceptedMimeType(String[] types) {
        if (noAcceptTypesSet(types)) {
            return new String[]{MimeType.DEFAULT.value};
        }
        String[] mimeTypes = new String[types.length];
        for (int i = 0; i < types.length; i++) {
            String t = types[i];
            // convert file extensions to mime types
            if (t.matches("\\.\\w+")) {
                String mimeType = getMimeTypeFromExtension(t.replace(".", ""));
                if(mimeType != null) {
                    mimeTypes[i] = mimeType;
                } else {
                    mimeTypes[i] = t;
                }
            } else {
                mimeTypes[i] = t;
            }
        }
        return mimeTypes;
    }

    private Boolean noAcceptTypesSet(String[] types) {
        // when our array returned from getAcceptTypes() has no values set from the webview
        // i.e. <input type="file" />, without any "accept" attr
        // will be an array with one empty string element, afaik

        return types.length == 0 || (types.length == 1 && types[0] != null && types[0].length() == 0);
    }

    private String getMimeTypeFromExtension(String extension) {
        String type = null;
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }
}

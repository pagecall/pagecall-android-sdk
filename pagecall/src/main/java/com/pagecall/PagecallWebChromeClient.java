package com.pagecall;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Parcelable;
import android.webkit.MimeTypeMap;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import java.util.ArrayList;

public class PagecallWebChromeClient extends WebChromeClient {

    public final static int IMAGE_SELECTOR_REQ = 911014;

    private PagecallWebView webView;

    private ValueCallback filePathCallback;

    public PagecallWebChromeClient(PagecallWebView webView) {
        this.webView = webView;
    }

    @Override
    public void onPermissionRequest(final PermissionRequest request) {
        request.grant(request.getResources());
    }

    @Override
    public boolean onShowFileChooser(WebView webView, ValueCallback filePathCallback, FileChooserParams fileChooserParams) {
        String[] acceptTypes = fileChooserParams.getAcceptTypes();
        boolean allowMultiple = fileChooserParams.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE;

        this.filePathCallback = filePathCallback;
        ArrayList<Parcelable> extraIntents = new ArrayList<>();
        Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
        Intent fileSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
        fileSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
        fileSelectionIntent.setType("*/*"); // default acceptable MimeType
        fileSelectionIntent.putExtra(Intent.EXTRA_MIME_TYPES, getAcceptedMimeType(acceptTypes));
        fileSelectionIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultiple);
        chooserIntent.putExtra(Intent.EXTRA_INTENT, fileSelectionIntent);
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, extraIntents.toArray(new Parcelable[]{}));

        ((Activity) this.webView.getContext()).startActivityForResult(chooserIntent, IMAGE_SELECTOR_REQ);

        return true;
    }

    public void handleActivityResult(final int requestCode, final int resultCode, final Intent intent) {
        if (requestCode == IMAGE_SELECTOR_REQ) {
            if (resultCode == Activity.RESULT_OK) {
                if (intent != null && filePathCallback != null) {
                    filePathCallback.onReceiveValue(getSelectedFiles(intent, resultCode));
                    filePathCallback = null;
                }
            }
            else {
                // Occurred if pressed back button without selecting files
                if (filePathCallback != null) {
                    filePathCallback.onReceiveValue(null);
                }
            }
        }
    }

    private String[] getAcceptedMimeType(String[] types) {
        if (noAcceptTypesSet(types)) {
            return new String[]{"*/*"};
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

    private Uri[] getSelectedFiles(Intent data, int resultCode) {
        if (data == null) {
            return null;
        }

        // we have multiple files selected
        if (data.getClipData() != null) {
            final int numSelectedFiles = data.getClipData().getItemCount();
            Uri[] result = new Uri[numSelectedFiles];
            for (int i = 0; i < numSelectedFiles; i++) {
                result[i] = data.getClipData().getItemAt(i).getUri();
            }
            return result;
        }

        // we have one file selected
        if (data.getData() != null && resultCode == Activity.RESULT_OK && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return WebChromeClient.FileChooserParams.parseResult(resultCode, data);
        }

        return null;
    }

}

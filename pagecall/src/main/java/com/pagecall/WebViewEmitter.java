package com.pagecall;

import android.util.Log;
import android.util.Pair;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;


class WebViewEmitter {
    private PagecallWebView webView;
    private Map<String, Callback> eventIdToCallback;

    public WebViewEmitter(PagecallWebView webView) {
        this.webView = webView;
        this.eventIdToCallback = new HashMap<>();
    }

    public interface Callback {
        void onResponse(Error error, String result);
    }

    private void rawEmit(String eventName, String message) {
        rawEmit(eventName, message, null);
    }

    private void rawEmit(String eventName, String message, String eventId) {
        String[] args = {eventName, message, eventId};
        String[] filteredArgs = Arrays.stream(args).filter(arg ->
                arg != null
        ).map(arg ->
                // 각 argument를 "로 감싼 JS 코드를 실행하기 때문에 argument에 "이 포함되어 있으면 SyntaxError가 뜬다.
                MessageFormat.format("\"{0}\"", arg.replaceAll("\"", "\\\\\""))
        ).toArray(String[]::new);
        final String script = MessageFormat.format(" window.PagecallNative.emit({0})", String.join(", ", filteredArgs));

        runScript(script, result -> {
            if (result == null) {
                System.out.println("Failed to PagecallNative.emit");
            }
        });
    }

    public void emit(NativeBridgeEvent eventName) {
        rawEmit(eventName.getValue(), null);
    }

    public void emit(NativeBridgeEvent eventName, String message) {
        rawEmit(eventName.getValue(), message);
    }

    public void emit(NativeBridgeEvent eventName, JSONObject json) {
        jsonEmit(eventName.getValue(), json, null);
    }

    public void request(NativeBridgeRequest eventName, JSONObject json, Callback callback) {
        jsonEmit(eventName.getValue(), json, callback);
    }

    public String requestSync(NativeBridgeRequest eventName, JSONObject json) throws Error, InterruptedException, ExecutionException {
        CompletableFuture<Pair<Error, String>> future = new CompletableFuture<>();

        jsonEmit(eventName.getValue(), json, (error, result) -> {
            future.complete(Pair.create(error, result));
        });

        Pair<Error, String> result = future.get();
        if (result.first != null) throw result.first;
        return result.second;
    }

    private void jsonEmit(String eventName, JSONObject json, Callback callback) {
        String stringifiedJson;
        try {
            stringifiedJson = json.toString();
        } catch (Exception e) {
            if (callback != null) {
                callback.onResponse(new Error("Failed to stringify"), null);
            }
            return;
        }

        if (callback != null) {
            String eventId = UUID.randomUUID().toString();
            eventIdToCallback.put(eventId, callback);
            rawEmit(eventName, stringifiedJson, eventId);
        } else {
            rawEmit(eventName, stringifiedJson);
        }
    }

    public void resolve(String eventId, String error, String result) {
        Callback callback = eventIdToCallback.get(eventId);
        if (callback != null) {
            eventIdToCallback.remove(eventId);
            if (error != null) {
                callback.onResponse(new Error(error), null);
            } else {
                callback.onResponse(null, result);
            }
        } else {
            System.out.println("Event not found (id: " + eventId);
        }
    }

    public void emit(NativeBridgeEvent eventName, byte[] data) {
        String string = new String(data, StandardCharsets.UTF_8);
        emit(eventName, string);
    }

    public void error(String name, String message) {
        System.out.println("errorLog " + name + " " + message);
        JSONObject json = new JSONObject();
        try {
            json.put("name", name);
            json.put("message", message);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
        emit(NativeBridgeEvent.ERROR, json);
    }

    public void log(String name, String message) {
        NativeBridgeErrorEvent errorEvent = new NativeBridgeErrorEvent(name, message);
        JSONObject json = new JSONObject();
        try {
            json.put("name", errorEvent.getName());
            json.put("name", errorEvent.getName());
        } catch (JSONException e) {
            return;
        }
    }

    public void runScript(final String script, Consumer<String> callback) {
        webView.post(() -> webView.evaluateJavascript(script, value -> {
            callback.accept(value);
        }));
    }

    public void response(String requestId, String data) {
        String script = "";
        if (data != null) {
            script = "window.PagecallNative.response('" + requestId + "', '" + data + "')";
        } else {
            script = "window.PagecallNative.response('" + requestId + "')";
        }
        runScript(script, value -> {
            if (value != null && !value.isEmpty()) return;
            Log.e("PagecallNative.response: ", value);
        });
    }

    public void responseError(String requestId, String errorMessage) {
        final String script = MessageFormat.format("window.PagecallNative.throw(\"{0}\", \"{1}\")", requestId, errorMessage);
        runScript(script, value -> {
            if (value != null && !value.isEmpty()) {
                Log.e("Failed to PagecallNative.response", value);
            }
        });
    }
}

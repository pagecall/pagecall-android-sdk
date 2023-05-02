package com.pagecall;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.util.Log;
import android.webkit.JavascriptInterface;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;


class NativeBridge {
    private PagecallWebView pagecallWebView;
    private MediaController mediaController;
    private Context context;
    private WebViewEmitter emitter;

    private HashMap<String, Consumer<String>> subscribers;

    private Boolean isAudioPaused = false;

    public Boolean loaded = false;
    private ArrayList<Consumer<Boolean>> loadConsumers = new ArrayList<Consumer<Boolean>>();
    public void listenLoaded(Consumer<Boolean> listener) {
        if (loaded) {
            listener.accept(true);
            return;
        }
        this.loadConsumers.add(listener);
    }

    private void setIsAudioPaused(Boolean value) {
        this.isAudioPaused = value;
        this.synchronizePauseState();
    }

    private void synchronizePauseState() {
        if (mediaController == null) return;

        Boolean success = isAudioPaused ? mediaController.pauseAudio() : mediaController.resumeAudio();
        if (success) {
            emitter.log("AudioStateChange", isAudioPaused ? "Paused" : "Resumed");
        } else {
            emitter.error("AudioStateChangeError", isAudioPaused ? "Failed to pause: no producer" : "Failed to resume: no producer");
        }
    }


    public static final int REQUEST_AUDIO_PERMISSION = 1001;
    public static final int REQUEST_VIDEO_PERMISSION = 1002;

    public interface PermissionCallback {
        void onError(Error error);
    }

    public static class MediaConstraints {
        public final Boolean audio;
        public final Boolean video;

        public MediaConstraints(Boolean audio, Boolean video) {
            this.audio = audio;
            this.video = video;
        }

        public JSONObject toJSON() {
            JSONObject obj = new JSONObject();
            try {
                obj.put("audio", audio);
                obj.put("video", video);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return obj;
        }
    }

    private MediaConstraints getPermissions(JSONObject constraint) {
        return new MediaConstraints(
                getAudioStatus(constraint.optBoolean("audio")),
                getVideoStatus(constraint.optBoolean("video"))
        );
    }

    private Boolean requestPermission(NativeBridgeMediaType mediaType) {
        Context context = pagecallWebView.getContext();
        switch (mediaType.mediaType) {
            case "audio": {
                // FIXME The line below blocks
                ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_AUDIO_PERMISSION);
                return true;
            }
            case "video": {
                ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.CAMERA}, REQUEST_VIDEO_PERMISSION);
                return true;
            }
            default: {
                return false;
            }
        }
    }

    private Boolean getAudioStatus(Boolean audio) {
        Context context = pagecallWebView.getContext();
        if (audio != null && audio) {
            int status = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO);
            switch (status) {
                case PackageManager.PERMISSION_GRANTED:
                    return true;
                case PackageManager.PERMISSION_DENIED:
                    return false;
                default:
                    return null;
            }
        }
        return null;
    }

    private Boolean getVideoStatus(Boolean video) {
        Context context = pagecallWebView.getContext();
        if (video != null && video) {
            int status = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA);
            switch (status) {
                case PackageManager.PERMISSION_GRANTED:
                    return true;
                case PackageManager.PERMISSION_DENIED:
                    return false;
                default:
                    return null;
            }
        }
        return null;
    }

    public NativeBridge(PagecallWebView webView, HashMap<String, Consumer<String>> subscribers) {
        this.pagecallWebView = webView;
        this.context = webView.getContext();
        this.emitter = new WebViewEmitter(this.pagecallWebView);
        this.subscribers = subscribers;
    }

    private JSONObject safeParseJSON(String json) {
        if (json == null || json.isEmpty()) {
            return new JSONObject();
        }
        try {
            JSONObject jsonObject = new JSONObject(json);
            return jsonObject;
        } catch (JSONException e) {
            Log.e("NativeBridge", "Failed to parse json: " + json);
        }
        return new JSONObject();
    }

    @JavascriptInterface
    public void postMessage(String message) {
        Log.d("Ryan123", "postMessage: " + message);
        JSONObject jsonObject = safeParseJSON(message);

        String action = jsonObject.optString("action", "");
        String requestId = jsonObject.optString("requestId", "");
        String payload = jsonObject.optString("payload", "{}");
        String postType = jsonObject.optString("type", "");


        JSONObject payloadData = safeParseJSON(payload);

        if (postType.equals("subscription")) {
            String id = payloadData.optString("id");
            String value = payloadData.optString("value");

            Consumer<String> subscriber = subscribers.get(id);

            if (subscriber != null) {
                subscriber.accept(value);
            }
        }

        if (action.isEmpty()) return;

        NativeBridgeAction bridgeAction = NativeBridgeAction.fromString(action);

        final BiConsumer<Exception, String> respond = (error, data) -> {
            if (error != null) {
                if (requestId != null) {
                    emitter.responseError(requestId, error.getLocalizedMessage());
                } else {
                    emitter.error("RequestFailed", error.getLocalizedMessage());
                }
            } else {
                if (requestId != null) {
                    emitter.response(requestId, data);
                } else {
                    System.out.println("Missing requestId");
                    emitter.error("RequestIdMissing", action + " succeeded without requestId");
                }
            }
        };
        final BiConsumer<Exception, JSONObject> respondObject = (error, data) -> {
            respond.accept(error, data.toString());
        };
        final BiConsumer<Exception, JSONArray> respondArray = (error, data) -> {
            respond.accept(error, data.toString());
        };
        final BiConsumer<Exception, Double> respondNumber = (error, data) -> {
            respond.accept(error, data.toString());
        };
        final BiConsumer<Exception, Boolean> respondBoolean = (error, data) -> {
            respond.accept(error, data.toString());
        };
        final Consumer<Exception> respondEmpty = (error) -> {
            respond.accept(error, null);
        };

        try {
            switch (bridgeAction) {
                case LOADED:
                    this.loaded = true;
                    this.loadConsumers.forEach(consumer -> {
                        consumer.accept(true);
                    });
                    loadConsumers.clear();
                    return;
                case INITIALIZE:
                    if (mediaController != null) {
                        respondObject.accept(new PagecallError("Must be disposed first"), null);
                        return;
                    }

                    MediaInfraController.MiInitialPayload initialPayload = new MediaInfraController.MiInitialPayload(payloadData);
                    this.mediaController = new MediaInfraController(emitter, initialPayload, context);
                    this.synchronizePauseState();
                    respondEmpty.accept(null);
                    return;

                case GET_PERMISSIONS:
                    respondObject.accept(null, getPermissions(payloadData).toJSON());
                    return;

                case REQUEST_PERMISSION:
                    respondBoolean.accept(null, true);
                    // TODO restore
                    // Boolean isGranted = requestPermission(new MediaType(payloadData));
                    // respondBoolean.accept(null, isGranted);
                    return;

                case GET_AUDIO_DEVICES:
                    AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                    if (audioManager != null) {
                        AudioDeviceInfo[] audioInputDevices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS);
                        AudioDeviceInfo[] audioOutputDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
                        AudioDeviceInfo[] audioDevices = new AudioDeviceInfo[audioInputDevices.length + audioOutputDevices.length];
                        System.arraycopy(audioInputDevices, 0, audioDevices, 0, audioInputDevices.length);
                        System.arraycopy(audioOutputDevices, 0, audioDevices, audioInputDevices.length, audioOutputDevices.length);

                        MediaDeviceInfo[] deviceList = MediaDeviceInfo.convertToMediaDeviceInfo(audioDevices);
                        respondArray.accept(null, MediaDeviceInfo.convertToJSONArray(deviceList));
                    } else {
                        respondEmpty.accept(new PagecallError("Missing audioManager"));
                    }
                    return;

                case REQUEST_AUDIO_VOLUME:
                    double volume = AudioRecordManager.getMicrophoneVolume(context);
                    respondNumber.accept(null, volume);
                    return;

                case PAUSE_AUDIO:
                    this.setIsAudioPaused(true);
                    respondEmpty.accept(null);
                    return;

                case RESUME_AUDIO:
                    this.setIsAudioPaused(false);
                    respondEmpty.accept(null);
                    return;

                case RESPONSE:
                    emitter.resolve(payloadData.getString("eventId"), JsonUtil.getStringNullable(payloadData, "error"), payloadData.optString("result"));
                    return;
                /**
                 * Below requires mediaController to exist
                 */
                case START:
                    if (mediaController == null) {
                        respondEmpty.accept(new PagecallError("Missing mediaController"));
                    } else {
                        mediaController.start(new MediaInfraController.AudioProducerCallback() {
                            @Override
                            public void onResult(Exception error) {
                                respondEmpty.accept(error);
                            }
                        });
                        this.synchronizePauseState();
                        // emit decibel after entering
                        AudioRecordManager.startEmitVolumeSchedule(context, emitter);
                    }
                    return;
                case DISPOSE:
                    if (mediaController != null) {
                        mediaController.dispose();
                        this.mediaController = null;
                    }
                    respondEmpty.accept(null);
                    return;
                case SET_AUDIO_DEVICE:
                    // No op for MediaInfraController
                    respondEmpty.accept(null);
                    return;
                case CONSUME:
                    if (!(mediaController instanceof MediaInfraController)) {
                        respondObject.accept(new PagecallError("Missing mediaController, initialize first"), null);
                        return;
                    }

                    JSONObject response = safeParseJSON(payload);

                    String sessionId = response.optString("appPeerId");
                    String producerId = response.optString("producerId");
                    String id = response.optString("id");
                    String kind = response.optString("kind");
                    String rtpParameters = response.optString("rtpParameters");
                    String type = response.optString("type");
                    String appData = response.optString("appData");
                    boolean producerPaused = response.optBoolean("paused");

                    ((MediaInfraController) mediaController).consume(
                            sessionId,
                            id,
                            producerId,
                            kind,
                            rtpParameters,
                            type,
                            appData,
                            producerPaused
                    );
                    respondEmpty.accept(null);
                    return;
            }
        } catch (Exception e) {
            respondEmpty.accept(e);
        }
    }

    public void destroy() {
        if (this.mediaController != null) {
            mediaController.dispose();
            mediaController = null;
        }
        AudioRecordManager.dispose();
    }
}
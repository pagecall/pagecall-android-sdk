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

    private ArrayList<Consumer<JSONObject>> bridgeMessageConsumers = new ArrayList();
    public void listenBridgeMessages(Consumer<JSONObject> listener) {
        this.bridgeMessageConsumers.add(listener);
    }

    private void setIsAudioPaused(Boolean value) {
        this.isAudioPaused = value;
        this.synchronizePauseState();
    }

    private void synchronizePauseState() {
        Log.d("Ryan123", "synchronizePauseState");
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

    private void requestPermission(NativeBridgeMediaType mediaType) {
        Log.d("Ryan123", "requestPermission 0");
        // TODO get return value
        Context context = pagecallWebView.getContext();
        Log.d("Ryan123", "requestPermission 1");
        switch (mediaType.mediaType) {
            case "audio": {
                Log.d("Ryan123", "requestPermission 2");
                ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_AUDIO_PERMISSION);
                Log.d("Ryan123", "requestPermission 3");
                return;
            }
            case "video": {
                ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.CAMERA}, REQUEST_VIDEO_PERMISSION);
                return;
            }
            default: {
                return;
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
        Log.d("Ryan123", "######### " + message);
        JSONObject jsonObject = safeParseJSON(message);
        this.bridgeMessageConsumers.forEach(consumer -> {
            consumer.accept(jsonObject);
        });

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
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        try {
            switch (bridgeAction) {
                case LOADED:
                    this.loaded = true;
                    return;
                case INITIALIZE:
                    Log.d("Ryan123", "INITIALIZE 0");
                    if (audioManager != null) {
                        Log.d("Ryan123", "INITIALIZE 1");
                        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                    } else {
                        Log.d("Ryan123", "INITIALIZE 2");
                        respondEmpty.accept(new PagecallError("Missing audioManager"));
                    }
                    if (mediaController != null) {
                        Log.d("Ryan123", "INITIALIZE 3");
                        respondObject.accept(new PagecallError("Must be disposed first"), null);
                        return;
                    }
                    Log.d("Ryan123", "INITIALIZE 4");
                    MediaInfraController.MiInitialPayload initialPayload = new MediaInfraController.MiInitialPayload(payloadData);
                    Log.d("Ryan123", "INITIALIZE 5");
                    this.mediaController = new MediaInfraController(emitter, initialPayload, context);
                    Log.d("Ryan123", "INITIALIZE 6");
                    this.synchronizePauseState();
                    Log.d("Ryan123", "INITIALIZE 7");
                    respondEmpty.accept(null);
                    return;

                case GET_PERMISSIONS:
                    respondObject.accept(null, getPermissions(payloadData).toJSON());
                    return;

                case REQUEST_PERMISSION:
                    Log.d("Ryan123", "REQUEST_PERMISSION 0");
                    requestPermission(new NativeBridgeMediaType(payloadData));
                    Log.d("Ryan123", "REQUEST_PERMISSION 1");
                    respondBoolean.accept(null, true);
                    return;

                case GET_AUDIO_DEVICES:
                    Log.d("Ryan123", "GET_AUDIO_DEVICES 0");
                    if (audioManager != null) {
                        Log.d("Ryan123", "GET_AUDIO_DEVICES 1");
                        AudioDeviceInfo[] audioInputDevices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS);
                        Log.d("Ryan123", "GET_AUDIO_DEVICES 2");
                        AudioDeviceInfo[] audioOutputDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
                        Log.d("Ryan123", "GET_AUDIO_DEVICES 3");
                        AudioDeviceInfo[] audioDevices = new AudioDeviceInfo[audioInputDevices.length + audioOutputDevices.length];
                        Log.d("Ryan123", "GET_AUDIO_DEVICES 4");
                        System.arraycopy(audioInputDevices, 0, audioDevices, 0, audioInputDevices.length);
                        Log.d("Ryan123", "GET_AUDIO_DEVICES 5");
                        System.arraycopy(audioOutputDevices, 0, audioDevices, audioInputDevices.length, audioOutputDevices.length);
                        Log.d("Ryan123", "GET_AUDIO_DEVICES 6");

                        MediaDeviceInfo[] deviceList = MediaDeviceInfo.convertToMediaDeviceInfo(audioDevices);
                        Log.d("Ryan123", "GET_AUDIO_DEVICES 7");

                        /**
                         * 코어앱에서 불필요하게 디바이스를 많이 보여주지않기 위해 input 중 하나만 넘겨줌.
                         * SET_AUDIO_DEVICE를 하지 않기 때문에 괜찮다.
                         * TODO: SET_AUDIO_DEVICE를 하게 되면 알맞게 전달해야함.
                         */
                        MediaDeviceInfo[] pickedDeviceList = MediaDeviceInfo.pickOneInput(deviceList);
                        Log.d("Ryan123", "GET_AUDIO_DEVICES 8");

                        respondArray.accept(null, MediaDeviceInfo.convertToJSONArray(pickedDeviceList));

                    } else {
                        Log.d("Ryan123", "GET_AUDIO_DEVICES ELSE");
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
                    Log.d("Ryan123", "START 0");
                    if (mediaController == null) {
                        Log.d("Ryan123", "START 1");
                        respondEmpty.accept(new PagecallError("Missing mediaController"));
                        Log.d("Ryan123", "START 2");
                    } else {
                        Log.d("Ryan123", "START 3");
                        mediaController.start(new MediaInfraController.AudioProducerCallback() {
                            @Override
                            public void onResult(Exception error) {
                                respondEmpty.accept(error);
                            }
                        });
                        Log.d("Ryan123", "START 4");
                        this.synchronizePauseState();
                        Log.d("Ryan123", "START 5");
                        // emit decibel after entering
                        AudioRecordManager.startEmitVolumeSchedule(context, emitter);
                        Log.d("Ryan123", "START 6");
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
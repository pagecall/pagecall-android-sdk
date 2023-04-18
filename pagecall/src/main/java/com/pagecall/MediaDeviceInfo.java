package com.pagecall;

import android.media.AudioDeviceInfo;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import org.json.JSONArray;
import org.json.JSONException;


class MediaDeviceInfo {

    private static final Gson GSON = new Gson();

    public static MediaDeviceInfo[] convertToMediaDeviceInfo(AudioDeviceInfo[] audioDeviceInfoList) {
        MediaDeviceInfo[] mediaDeviceInfoList = new MediaDeviceInfo[audioDeviceInfoList.length];

        for (int i = 0; i < audioDeviceInfoList.length; i++) {
            AudioDeviceInfo audioDeviceInfo = audioDeviceInfoList[i];
            String deviceId = Integer.toString(audioDeviceInfo.getId());
            String groupId = "";
            MediaDeviceKind kind;

            boolean isSource = audioDeviceInfo.isSource();
            boolean isSink = audioDeviceInfo.isSink();
            if (isSource && !isSink) {
                kind = MediaDeviceKind.AUDIO_INPUT;
            } else if (!isSource && isSink) {
                kind = MediaDeviceKind.AUDIO_OUTPUT;
            } else {
                kind = MediaDeviceKind.AUDIO_INPUT;
            }

            String label = audioDeviceInfo.getProductName().toString() + "-" + deviceId;

            mediaDeviceInfoList[i] = new MediaDeviceInfo(deviceId, groupId, kind, label);
        }

        return mediaDeviceInfoList;
    }

    @NonNull
    public static JSONArray convertToJSONArray(@NonNull MediaDeviceInfo[] mediaDeviceInfoList) throws JSONException {
        String json = GSON.toJson(mediaDeviceInfoList);
        return new JSONArray(json);
    }

    @SerializedName("deviceId")
    final private String deviceId;

    @SerializedName("groupId")
    final private String groupId;

    @SerializedName("kind")
    final private MediaDeviceKind kind;

    @SerializedName("label")
    final private String label;

    public MediaDeviceInfo(String deviceId, String groupId, MediaDeviceKind kind, String label) {
        this.deviceId = deviceId;
        this.groupId = groupId;
        this.kind = kind;
        this.label = label;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getGroupId() {
        return groupId;
    }

    public MediaDeviceKind getKind() {
        return kind;
    }

    public String getLabel() {
        return label;
    }
}
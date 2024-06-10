package com.pagecall;

import android.util.Log;

import androidx.annotation.NonNull;

import com.amazonaws.services.chime.sdk.meetings.audiovideo.AudioVideoObserver;
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.RemoteVideoSource;
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionStatus;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class ChimeAudioVideoObserver implements AudioVideoObserver {
    private WebViewEmitter emitter;
    private ChimeController chimeController;

    ChimeAudioVideoObserver(WebViewEmitter emitter, ChimeController chimeController) {
        this.emitter = emitter;
        this.chimeController = chimeController;
    }

    private void emitAudioDeviceList() {
        // audio가 start, stop 될 때마다 장치 리스트를 업데이트해준다.
        // (stop 상태에서는 활성화된 장치 하나만 보내줘야함. getAudioDevice() 코멘트 참고.)
        try {
            MediaDeviceInfo[] mediaDeviceInfoList = chimeController.getAudioDevices();
            this.emitter.emit(NativeBridgeEvent.AUDIO_DEVICES, MediaDeviceInfo.convertToJSONArray(mediaDeviceInfoList));
        } catch (Exception error) {
            error.printStackTrace();
            Log.e("ChimeDeviceChangeObserver", "Error creating JSON object.");
        }
    }

    @Override
    public void onAudioSessionStartedConnecting(boolean reconnecting) {
    }

    @Override
    public void onAudioSessionStarted(boolean reconnecting) {
        this.chimeController.setAudioSessionStarted(true);
        JSONObject json = new JSONObject();
        try {
            json.put("reconnecting", reconnecting);
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e("ChimeAudioVideoObserver", "Error creating JSON object.");
        }
        this.emitter.emit(NativeBridgeEvent.CONNECTED, json);
        this.emitAudioDeviceList();
    }

    @Override
    public void onAudioSessionDropped() {
    }

    @Override
    public void onAudioSessionStopped(MeetingSessionStatus sessionStatus) {
        this.chimeController.setAudioSessionStarted(false);
        JSONObject json = new JSONObject();
        try {
            json.put("statusCode", sessionStatus.getStatusCode().getValue());
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("ChimeAudioVideoObserver", "Error creating JSON object: " + e.getMessage());
        }
        this.emitter.emit(NativeBridgeEvent.DISCONNECTED, json);
        this.emitAudioDeviceList();
    }

    @Override
    public void onAudioSessionCancelledReconnect() {
    }

    @Override
    public void onConnectionRecovered() {
    }

    @Override
    public void onConnectionBecamePoor() {
    }

    @Override
    public void onVideoSessionStartedConnecting() {
    }

    @Override
    public void onVideoSessionStarted(MeetingSessionStatus sessionStatus) {
    }

    @Override
    public void onVideoSessionStopped(MeetingSessionStatus sessionStatus) {
    }

    @Override
    public void onRemoteVideoSourceUnavailable(@NonNull List<RemoteVideoSource> list) {
    }

    @Override
    public void onRemoteVideoSourceAvailable(@NonNull List<RemoteVideoSource> list) {
    }

    @Override
    public void onCameraSendAvailabilityUpdated(boolean b) {
    }
}

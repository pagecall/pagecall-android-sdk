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
    public WebViewEmitter emitter;

    ChimeAudioVideoObserver(WebViewEmitter emitter) {
        this.emitter = emitter;
    }

    @Override
    public void onAudioSessionStartedConnecting(boolean reconnecting) {
    }

    @Override
    public void onAudioSessionStarted(boolean reconnecting) {
        JSONObject json = new JSONObject();
        try {
            json.put("reconnecting", reconnecting);
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e("ChimeAudioVideoObserver", "Error creating JSON object.");
        }
        this.emitter.emit(NativeBridgeEvent.CONNECTED, json);
    }

    @Override
    public void onAudioSessionDropped() {
    }

    @Override
    public void onAudioSessionStopped(MeetingSessionStatus sessionStatus) {
        JSONObject json = new JSONObject();
        try {
            json.put("statusCode", sessionStatus.getStatusCode().getValue());
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e("ChimeAudioVideoObserver", "Error creating JSON object.");
        }
        this.emitter.emit(NativeBridgeEvent.DISCONNECTED, json);
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

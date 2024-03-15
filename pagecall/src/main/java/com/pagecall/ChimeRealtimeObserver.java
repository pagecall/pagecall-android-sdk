package com.pagecall;

import android.util.Log;

import androidx.annotation.NonNull;

import com.amazonaws.services.chime.sdk.meetings.audiovideo.AttendeeInfo;
import com.amazonaws.services.chime.sdk.meetings.audiovideo.SignalUpdate;
import com.amazonaws.services.chime.sdk.meetings.audiovideo.VolumeUpdate;
import com.amazonaws.services.chime.sdk.meetings.realtime.RealtimeObserver;

import org.json.JSONException;
import org.json.JSONObject;

public class ChimeRealtimeObserver implements RealtimeObserver {
    private WebViewEmitter emitter;
    private String myAttendeeId;

    ChimeRealtimeObserver(WebViewEmitter emitter, String myAttendeeId) {
        this.emitter = emitter;
        this.myAttendeeId = myAttendeeId;
    }

    private void emitAudioStatusEvent(String sessionId, Boolean muted) {
        JSONObject payload = new JSONObject();
        try {
            payload.put("sessionId", sessionId);
            payload.put("muted", muted);
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e("ChimeRealtimeObserver", "Error creating JSON object.");
        }
        this.emitter.emit(NativeBridgeEvent.AUDIO_STATUS, payload);
    }

    @Override
    public void onAttendeesDropped(@NonNull AttendeeInfo[] attendeeInfos) {
    }

    @Override
    public void onAttendeesJoined(@NonNull AttendeeInfo[] attendeeInfos) {
        // mute 상태로 입장하는 유저나, 이미 mute 상태로 방에 있었던 유저는 join 이후 mute event가 불리므로 기본적으로 unmute로 판단할 수 있음
        for (AttendeeInfo attendeeInfo : attendeeInfos) {
            this.emitAudioStatusEvent(attendeeInfo.getExternalUserId(), false);
        }
    }

    @Override
    public void onAttendeesLeft(@NonNull AttendeeInfo[] attendeeInfos) {
    }

    @Override
    public void onAttendeesMuted(@NonNull AttendeeInfo[] attendeeInfos) {
        for (AttendeeInfo attendeeInfo : attendeeInfos) {
            this.emitAudioStatusEvent(attendeeInfo.getExternalUserId(), true);
        }
    }

    @Override
    public void onAttendeesUnmuted(@NonNull AttendeeInfo[] attendeeInfos) {
        for (AttendeeInfo attendeeInfo : attendeeInfos) {
            this.emitAudioStatusEvent(attendeeInfo.getExternalUserId(), false);
        }
    }

    @Override
    public void onSignalStrengthChanged(@NonNull SignalUpdate[] signalUpdates) {
    }

    @Override
    public void onVolumeChanged(@NonNull VolumeUpdate[] volumeUpdates) {
        for (VolumeUpdate volumeUpdate : volumeUpdates) {
            if (volumeUpdate.getAttendeeInfo().getAttendeeId().equals(this.myAttendeeId)) {
                double audioVolume = 0;
                switch (volumeUpdate.getVolumeLevel()) {
                    case NotSpeaking:
                        audioVolume = 0;
                        break;
                    case Low:
                        audioVolume = 0.25;
                        break;
                    case Medium:
                        audioVolume = 0.5;
                        break;
                    case High:
                        audioVolume = 0.75;
                        break;
                    default:
                        break;
                }
                this.emitter.emit(NativeBridgeEvent.AUDIO_VOLUME, String.valueOf(audioVolume));
            }
        }
    }
}

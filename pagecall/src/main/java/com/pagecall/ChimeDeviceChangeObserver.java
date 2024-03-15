package com.pagecall;

import android.util.Log;

import androidx.annotation.NonNull;

import com.amazonaws.services.chime.sdk.meetings.device.DeviceChangeObserver;
import com.amazonaws.services.chime.sdk.meetings.device.MediaDevice;
import com.amazonaws.services.chime.sdk.meetings.session.DefaultMeetingSession;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.util.List;

public class ChimeDeviceChangeObserver implements DeviceChangeObserver {
    public WebViewEmitter emitter;
    public DefaultMeetingSession meetingSession;

    ChimeDeviceChangeObserver(WebViewEmitter emitter, DefaultMeetingSession meetingSession) {
        this.emitter = emitter;
        this.meetingSession = meetingSession;
    }

    @Override
    public void onAudioDeviceChanged(@NonNull List<MediaDevice> deviceList) {
        MediaDeviceInfo[] mediaDeviceInfoList = new MediaDeviceInfo[deviceList.size()];
        for (int i = 0; i < deviceList.size(); i++) {
            MediaDevice device = deviceList.get(i);
            mediaDeviceInfoList[i] = new MediaDeviceInfo(device.getLabel(), "DefaultGroupId", MediaDeviceKind.AUDIO_INPUT, device.getLabel());
        }

        try {
            this.emitter.emit(NativeBridgeEvent.AUDIO_DEVICES, MediaDeviceInfo.convertToJSONArray(mediaDeviceInfoList));
            MediaDevice activeDevice = this.meetingSession.getAudioVideo().getActiveAudioDevice();
            MediaDeviceInfo activeMediaDeviceInfo = new MediaDeviceInfo(activeDevice.getLabel(), "DefaultGroupId", MediaDeviceKind.AUDIO_INPUT, activeDevice.getLabel());
            this.emitter.emit(NativeBridgeEvent.AUDIO_DEVICE, new JSONObject(new Gson().toJson(activeMediaDeviceInfo)));
        } catch(Exception error) {
            error.printStackTrace();
            Log.e("ChimeDeviceChangeObserver", "Error creating JSON object.");
        }
    }
}

package com.pagecall;

import android.util.Log;

import androidx.annotation.NonNull;

import com.amazonaws.services.chime.sdk.meetings.device.DeviceChangeObserver;
import com.amazonaws.services.chime.sdk.meetings.device.MediaDevice;
import com.amazonaws.services.chime.sdk.meetings.session.DefaultMeetingSession;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ChimeDeviceChangeObserver implements DeviceChangeObserver {
    private WebViewEmitter emitter;
    private ChimeController chimeController;
    private DefaultMeetingSession meetingSession;

    ChimeDeviceChangeObserver(WebViewEmitter emitter, ChimeController chimeController, DefaultMeetingSession meetingSession) {
        this.emitter = emitter;
        this.chimeController = chimeController;
        this.meetingSession = meetingSession;
    }

    private MediaDeviceInfo getActiveMediaDeviceInfo() {
        MediaDevice activeDevice = this.meetingSession.getAudioVideo().getActiveAudioDevice();
        if (activeDevice != null) {
            MediaDeviceInfo activeMediaDeviceInfo = new MediaDeviceInfo(activeDevice.getLabel(), "DefaultGroupId", MediaDeviceKind.AUDIO_INPUT, activeDevice.getLabel());
            return activeMediaDeviceInfo;
        }
        return null;
    }

    @Override
    public void onAudioDeviceChanged(@NonNull List<MediaDevice> deviceList) {
        boolean isAudioSessionStarted = this.chimeController.getAudioSessionStarted();
        List<MediaDevice> sortedDeviceList = deviceList.stream().sorted(Comparator.comparingInt(MediaDevice::getOrder)).collect(Collectors.toList());

        // 오디오 세션이 시작되지 않으면 임의로 오디오 장치를 선택할 수 없기에 현재 활성화된 장치 하나만 보여준다
        // reference: https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.device/-device-controller/choose-audio-device.html
        MediaDeviceInfo[] mediaDeviceInfoList = new MediaDeviceInfo[isAudioSessionStarted ? deviceList.size() : 1];
        MediaDeviceInfo activeMediaDeviceInfo = this.getActiveMediaDeviceInfo();

        for (int i = 0; i < mediaDeviceInfoList.length; i++) {
            MediaDevice device = sortedDeviceList.get(i);
            MediaDeviceInfo mediaDeviceInfo = new MediaDeviceInfo(device.getLabel(), "DefaultGroupId", MediaDeviceKind.AUDIO_INPUT, device.getLabel());
            if (activeMediaDeviceInfo == null && i == 0) {
                // 간혹 activeMediaDeviceInfo가 없는 경우가 있다. 우선순위가 높은 디바이스를 활성화 장치로 간주한다.
                activeMediaDeviceInfo = mediaDeviceInfo;
            }
            mediaDeviceInfoList[i] = mediaDeviceInfo;
        }

        try {
            this.emitter.emit(NativeBridgeEvent.AUDIO_DEVICES, MediaDeviceInfo.convertToJSONArray(mediaDeviceInfoList));
            if (activeMediaDeviceInfo != null) {
                this.emitter.emit(NativeBridgeEvent.AUDIO_DEVICE, new JSONObject(new Gson().toJson(activeMediaDeviceInfo)));
            }
        } catch (Exception error) {
            error.printStackTrace();
            Log.e("ChimeDeviceChangeObserver", "Error creating JSON object.");
        }
    }
}

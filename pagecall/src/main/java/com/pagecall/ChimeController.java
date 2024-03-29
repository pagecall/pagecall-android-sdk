package com.pagecall;

import android.content.Context;

import com.amazonaws.services.chime.sdk.meetings.device.MediaDevice;
import com.amazonaws.services.chime.sdk.meetings.session.Attendee;
import com.amazonaws.services.chime.sdk.meetings.session.CreateAttendeeResponse;
import com.amazonaws.services.chime.sdk.meetings.session.CreateMeetingResponse;
import com.amazonaws.services.chime.sdk.meetings.session.DefaultMeetingSession;
import com.amazonaws.services.chime.sdk.meetings.session.Meeting;
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionConfiguration;
import com.amazonaws.services.chime.sdk.meetings.utils.logger.ConsoleLogger;
import com.amazonaws.services.chime.sdk.meetings.utils.logger.LogLevel;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class ChimeController extends MediaController {
    static class MeetingInfo {
        @SerializedName("meetingResponse")
        private MeetingResponse meetingResponse;

        @SerializedName("attendeeResponse")
        private AttendeeResponse attendeeResponse;

        public MeetingResponse getMeetingResponse() {
            return meetingResponse;
        }

        public AttendeeResponse getAttendeeResponse() {
            return attendeeResponse;
        }
    }

    static class MeetingResponse {
        @SerializedName("Meeting")
        private Meeting meeting;

        public Meeting getMeeting() {
            return meeting;
        }
    }

    static class AttendeeResponse {
        @SerializedName("Attendee")
        private Attendee attendee;

        public Attendee getAttendee() {
            return attendee;
        }
    }

    // Deserialize the response to object.
    static class ChimeInitialPayload {
        public MeetingInfo meetingInfo;

        ChimeInitialPayload(JSONObject data) {
            this.meetingInfo = new Gson().fromJson(
                    data.toString(),
                    MeetingInfo.class
            );
        }
    }

    private WebViewEmitter emitter;
    private DefaultMeetingSession meetingSession;
    private ChimeRealtimeObserver realtimeObserver;
    private ChimeAudioVideoObserver audioVideoObserver;
    private ChimeMetricsObserver metricsObserver;
    private ChimeDeviceChangeObserver deviceChangeObserver;
    private boolean isAudioSessionStarted = false;

    ChimeController(WebViewEmitter emitter, ChimeInitialPayload initialPayload, Context context) {
        this.emitter = emitter;
        MeetingSessionConfiguration configuration = new MeetingSessionConfiguration(
                new CreateMeetingResponse(initialPayload.meetingInfo.meetingResponse.meeting),
                new CreateAttendeeResponse(initialPayload.meetingInfo.attendeeResponse.attendee)
        );
        this.meetingSession = new DefaultMeetingSession(configuration, new ConsoleLogger(LogLevel.VERBOSE), context);
        this.realtimeObserver = new ChimeRealtimeObserver(this.emitter, initialPayload.meetingInfo.attendeeResponse.attendee.getAttendeeId());
        this.audioVideoObserver = new ChimeAudioVideoObserver(this.emitter, this);
        this.metricsObserver = new ChimeMetricsObserver(this.emitter);
        this.deviceChangeObserver = new ChimeDeviceChangeObserver(this.emitter, this, this.meetingSession);

        this.meetingSession.getAudioVideo().addRealtimeObserver(this.realtimeObserver);
        this.meetingSession.getAudioVideo().addAudioVideoObserver(this.audioVideoObserver);
        this.meetingSession.getAudioVideo().addMetricsObserver(this.metricsObserver);
        this.meetingSession.getAudioVideo().addDeviceChangeObserver(this.deviceChangeObserver);
    }

    public void setAudioSessionStarted(boolean isStarted) {
        this.isAudioSessionStarted = isStarted;
    }

    public boolean getAudioSessionStarted() {
        return this.isAudioSessionStarted;
    }

    public void setAudioDevice(String deviceLabel) throws PagecallError {
        List<MediaDevice> audioDevices = this.meetingSession.getAudioVideo().listAudioDevices();
        Optional<MediaDevice> targetDevice = audioDevices.stream().filter(mediaDevice -> mediaDevice.getLabel().equals(deviceLabel)).findFirst();
        if (targetDevice.isPresent()) {
            this.meetingSession.getAudioVideo().chooseAudioDevice(targetDevice.get());
        } else {
            throw new PagecallError("Missing device with id: " + deviceLabel);
        }
    }

    public MediaDeviceInfo[] getAudioDevices() {
        boolean isAudioStarted = this.getAudioSessionStarted();
        List<MediaDevice> audioDeviceList = this.meetingSession.getAudioVideo().listAudioDevices();
        MediaDeviceInfo[] sortedDeviceInfoList = audioDeviceList.stream()
                .sorted(Comparator.comparingInt(MediaDevice::getOrder))
                .map(device -> new MediaDeviceInfo(device.getLabel(), "DefaultGroupId", MediaDeviceKind.AUDIO_INPUT, device.getLabel()))
                .toArray(MediaDeviceInfo[]::new);
        if (sortedDeviceInfoList.length == 0) {
            return new MediaDeviceInfo[0];
        } else if (isAudioStarted) {
            return sortedDeviceInfoList;
        } else {
            // 오디오 세션이 시작되지 않으면 임의로 오디오 장치를 선택할 수 없기에 현재 활성화된 장치 하나만 보여준다
            // reference: https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.device/-device-controller/choose-audio-device.html
            return Arrays.copyOfRange(sortedDeviceInfoList, 0, 1);
        }
    }

    @Override
    public Boolean pauseAudio() {
        if (this.meetingSession != null) {
            this.meetingSession.getAudioVideo().realtimeLocalMute();
            return true;
        }
        return false;
    }

    @Override
    public Boolean resumeAudio() {
        if (this.meetingSession != null) {
            this.meetingSession.getAudioVideo().realtimeLocalUnmute();
            return true;
        }
        return false;
    }

    @Override
    public void start(AudioProducerCallback callback) {
        try {
            this.meetingSession.getAudioVideo().start();
            callback.onResult(null);
        } catch (Exception error) {
            callback.onResult(error);
        }
    }

    @Override
    public void dispose() {
        this.setAudioSessionStarted(false);
        this.meetingSession.getAudioVideo().stop();

        this.meetingSession.getAudioVideo().removeRealtimeObserver(this.realtimeObserver);
        this.meetingSession.getAudioVideo().removeAudioVideoObserver(this.audioVideoObserver);
        this.meetingSession.getAudioVideo().removeMetricsObserver(this.metricsObserver);
        this.meetingSession.getAudioVideo().removeDeviceChangeObserver(this.deviceChangeObserver);
    }
}

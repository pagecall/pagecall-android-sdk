package com.pagecall;

import android.content.Context;
import android.util.Log;

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

    ChimeController(WebViewEmitter emitter, ChimeInitialPayload initialPayload, Context context) {
        this.emitter = emitter;
        try {
            MeetingSessionConfiguration configuration = new MeetingSessionConfiguration(
                    new CreateMeetingResponse(initialPayload.meetingInfo.meetingResponse.meeting),
                    new CreateAttendeeResponse(initialPayload.meetingInfo.attendeeResponse.attendee)
            );
            this.meetingSession = new DefaultMeetingSession(configuration, new ConsoleLogger(LogLevel.VERBOSE), context);
            this.realtimeObserver = new ChimeRealtimeObserver(this.emitter, initialPayload.meetingInfo.attendeeResponse.attendee.getAttendeeId());
            this.audioVideoObserver = new ChimeAudioVideoObserver(this.emitter);
            this.metricsObserver = new ChimeMetricsObserver(this.emitter);
            this.deviceChangeObserver = new ChimeDeviceChangeObserver(this.emitter, this.meetingSession);

            this.meetingSession.getAudioVideo().addRealtimeObserver(this.realtimeObserver);
            this.meetingSession.getAudioVideo().addAudioVideoObserver(this.audioVideoObserver);
            this.meetingSession.getAudioVideo().addMetricsObserver(this.metricsObserver);
            this.meetingSession.getAudioVideo().addDeviceChangeObserver(this.deviceChangeObserver);
        } catch (Exception error) {
            Log.e("tommy", error.getStackTrace().toString());
            Log.e("tommy", error.toString());
        }
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
        List<MediaDevice> audioDeviceList = meetingSession.getAudioVideo().listAudioDevices();
        return audioDeviceList.stream().map(device -> new MediaDeviceInfo(device.getLabel(), "DefaultGroupId", MediaDeviceKind.AUDIO_INPUT, device.getLabel())).toArray(MediaDeviceInfo[]::new);
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
            meetingSession.getAudioVideo().start();
            callback.onResult(null);
        } catch (Exception error) {
            callback.onResult(error);
        }
    }

    @Override
    public void dispose() {
        meetingSession.getAudioVideo().stop();
    }
}

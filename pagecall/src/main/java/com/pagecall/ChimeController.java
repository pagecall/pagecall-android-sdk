package com.pagecall;

import android.content.Context;

import androidx.annotation.Nullable;

import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.DefaultEglCoreFactory;
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.EglCoreFactory;
import com.amazonaws.services.chime.sdk.meetings.session.Attendee;
import com.amazonaws.services.chime.sdk.meetings.session.CreateAttendeeResponse;
import com.amazonaws.services.chime.sdk.meetings.session.CreateMeetingResponse;
import com.amazonaws.services.chime.sdk.meetings.session.DefaultMeetingSession;
import com.amazonaws.services.chime.sdk.meetings.session.Meeting;
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSession;
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionConfiguration;
import com.amazonaws.services.chime.sdk.meetings.utils.logger.ConsoleLogger;
import com.amazonaws.services.chime.sdk.meetings.utils.logger.LogLevel;
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger;
import com.google.gson.Gson;

import org.json.JSONObject;



class ChimeController extends MediaController {

    static Gson gson = new Gson();

    @Nullable
    static public MeetingSessionConfiguration createChimeConfiguration(JSONObject rawConfiguration) {
        try {
            JSONObject meetingResponse = rawConfiguration.optJSONObject("meetingResponse");
            JSONObject attendeeResponse = rawConfiguration.optJSONObject("attendeeResponse");
            if (meetingResponse == null || attendeeResponse == null) {
                return null;
            }

            String meetingString = meetingResponse.optString("Meeting");
            String attendeeString = attendeeResponse.optString("Attendee");

            Meeting meeting = gson.fromJson(meetingString, Meeting.class);
            Attendee attendee = gson.fromJson(attendeeString, Attendee.class);

            return new MeetingSessionConfiguration(new CreateMeetingResponse(meeting), new CreateAttendeeResponse(attendee));
        } catch (Error error) {
            return null;
        }
    }
    private WebViewEmitter emitter;
    private Context context;

    private MeetingSession meetingSession;
    private Logger logger = new ConsoleLogger(LogLevel.INFO);
    private EglCoreFactory eglCoreFactory = new DefaultEglCoreFactory();
    ChimeController(WebViewEmitter emitter, MeetingSessionConfiguration configuration, Context context) {
        this.emitter = emitter;
        this.context = context;
        this.meetingSession = new DefaultMeetingSession(configuration, logger, context, eglCoreFactory);
    }
    @Override
    public Boolean pauseAudio() {
        return meetingSession.getAudioVideo().realtimeLocalMute();
    }

    @Override
    public Boolean resumeAudio() {
        return meetingSession.getAudioVideo().realtimeLocalUnmute();
    }

    @Override
    public void start(ErrorCallback callback) {
        try {
            meetingSession.getAudioVideo().start();
            callback.onResult(null);
        } catch(Error error) {
            callback.onResult(new Exception(error));
        }
    }

    @Override
    public void dispose() {
        meetingSession.getAudioVideo().stop();
    }

    public void startScreenShare() {

    }

}

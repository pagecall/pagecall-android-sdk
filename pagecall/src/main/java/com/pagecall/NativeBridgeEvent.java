package com.pagecall;

enum NativeBridgeEvent {
    AUDIO_DEVICE("audioDevice"),
    AUDIO_DEVICES("audioDevices"),
    AUDIO_VOLUME("audioVolume"),
    AUDIO_STATUS("audioStatus"),
    AUDIO_SESSION_ROUTE_CHANGED("audioSessionRouteChanged"),
    AUDIO_SESSION_INTERRUPTED("audioSessionInterrupted"),
    MEDIA_STAT("mediaStat"),
    AUDIO_ENDED("audioEnded"),
    VIDEO_ENDED("videoEnded"),
    SCREENSHARE_ENDED("screenshareEnded"),
    CONNECTED("connected"),
    DISCONNECTED("disconnected"),
    MEETING_ENDED("meetingEnded"),
    LOG("log"),
    ERROR("error"),
    CONNECT_TRANSPORT("connectTransport"),
    ;

    private final String value;

    NativeBridgeEvent(String value) {
        this.value = value;
    }

    String getValue() {
        return this.value;
    }

    static NativeBridgeEvent fromString(String value) {
        for (NativeBridgeEvent nativeBridgeEvent : NativeBridgeEvent.values()) {
            if (nativeBridgeEvent.value.equals(value)) {
                return nativeBridgeEvent;
            }
        }
        throw new IllegalArgumentException("Invalid value: " + value);
    }
}

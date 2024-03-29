package com.pagecall;

enum NativeBridgeAction {
    LOADED("loaded"),
    TERMINATED("terminated"),
    MESSAGE("message"),
    EVENT("event"),
    INITIALIZE("initialize"),
    DISPOSE("dispose"),
    START("start"),
    GET_PERMISSIONS("getPermissions"),
    GET_MEDIA_STATS("getMediaStats"),
    REQUEST_PERMISSION("requestPermission"),
    PAUSE_AUDIO("pauseAudio"),
    RESUME_AUDIO("resumeAudio"),
    GET_AUDIO_DEVICES("getAudioDevices"),
    SET_AUDIO_DEVICE("setAudioDevice"),
    REQUEST_AUDIO_VOLUME("requestAudioVolume"),
    CONSUME("consume"),
    RESPONSE("response"),

    ;

    private final String value;

    NativeBridgeAction(String value) {
        this.value = value;
    }

    String getValue() {
        return this.value;
    }

    static NativeBridgeAction fromString(String value) {
        for (NativeBridgeAction nativeBridgeAction : NativeBridgeAction.values()) {
            if (nativeBridgeAction.value.equals(value)) {
                return nativeBridgeAction;
            }
        }
        throw new IllegalArgumentException("Invalid value: " + value);
    }
}

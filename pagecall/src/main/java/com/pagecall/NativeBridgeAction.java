package com.pagecall;

enum NativeBridgeAction {
    LOADED("loaded"), // js loaded (when window.PagecallUI exists)
    INITIALIZE("initialize"),
    DISPOSE("dispose"),
    START("start"),
    GET_PERMISSIONS("getPermissions"),
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

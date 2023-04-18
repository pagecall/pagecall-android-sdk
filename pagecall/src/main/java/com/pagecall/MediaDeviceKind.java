package com.pagecall;

import androidx.annotation.NonNull;

enum MediaDeviceKind {
    AUDIO_INPUT("audioinput"),
    AUDIO_OUTPUT("audiooutput");

    private final String kind;

    // TODO: Clarify implementation intention later.
    private MediaDeviceKind(String kind) {
        this.kind = kind;
    }

    @NonNull
    @Override
    public String toString() {
        return kind;
    }
}

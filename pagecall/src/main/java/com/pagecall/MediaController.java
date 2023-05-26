package com.pagecall;

interface ErrorCallback {
    void onResult(Exception error);
}
abstract class MediaController {
    public abstract Boolean pauseAudio();

    public abstract Boolean resumeAudio();

    public abstract void start(ErrorCallback callback);

    public abstract void dispose();
}

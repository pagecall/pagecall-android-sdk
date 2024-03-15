package com.pagecall;

abstract class MediaController {
    public abstract Boolean pauseAudio();

    public abstract Boolean resumeAudio();

    public abstract void start(AudioProducerCallback callback);

    public abstract void dispose();

    interface AudioProducerCallback {
        void onResult(Exception error);
    }
}

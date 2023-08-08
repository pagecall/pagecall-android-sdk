package com.pagecall;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Statically Manages AudioRecord.
 */
class AudioRecordManager {
    private static AudioRecord audioRecord;
    private static ScheduledExecutorService volumeEmitter;

    private static int bufferSize;
    private static short[] buffer;

    private static final int SAMPLE_RATE = 8000;

    private static final double AMPLITUDE_IDLE = 1000.0;
    private static final double AMPLITUDE_MAX = 10000.0;

    /**
     * Checks Permission, computes and returns amplitude in 0 ~ 10000
     *
     * @param context Android Context
     * @return 0 if permission is not granted
     */
    static double getMicrophoneAmplitude(@NonNull Context context) {
        // Check Permission
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return 0;
        }

        // Create AudioRecord if null
        if (audioRecord == null) {
            bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            if (bufferSize != AudioRecord.ERROR_BAD_VALUE && bufferSize != AudioRecord.ERROR) {
                buffer = new short[bufferSize];
                audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
            }

        }
        if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
            audioRecord.startRecording();
        }

        // Get Volume using Buffer Amplitudes
        int readSize = audioRecord.read(buffer, 0, bufferSize);
        if (readSize > 0) {
            int maxAmplitude = 0;
            for (int i = 0; i < readSize; i++) {
                maxAmplitude = Math.max(maxAmplitude, Math.abs(buffer[i]));
            }

            return maxAmplitude;
        } else {
            return 0;
        }
    }

    /**
     * returns volume in 0 ~ 1
     *
     * @param context Android Context
     * @return 0 if permission is not granted
     */
    static double getMicrophoneVolume(@NonNull Context context) {
        double amplitude = getMicrophoneAmplitude(context);
        double volume = (amplitude - AMPLITUDE_IDLE) / AMPLITUDE_MAX;
        return Math.max(0, Math.min(1, volume));
    }

    /**
     * emit microphone decibel every second
     * @param context Android Context
     * @param emitter
     */
    static void startEmitVolumeSchedule(@NonNull Context context, @NonNull WebViewEmitter emitter) {
        if (volumeEmitter == null) volumeEmitter = Executors.newSingleThreadScheduledExecutor();
        volumeEmitter.scheduleAtFixedRate(
                () -> emitter.emit(NativeBridgeEvent.AUDIO_VOLUME, Double.toString(getMicrophoneVolume(context))),
                0,
                500,
                TimeUnit.MILLISECONDS
        );
    }


    static void shutdownSchedule() {
        if (volumeEmitter != null) {
            volumeEmitter.shutdown();
            volumeEmitter = null;
        }
    }

    static void dispose() {
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
        shutdownSchedule();
    }
}
package com.pagecall;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

public class BluetoothReceiver extends BroadcastReceiver {
    private static final String TAG = "BluetoothReceiver";

    BluetoothReceiver(Context context) {
        Log.d(TAG, "START");
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            audioManager.stopBluetoothSco();
            audioManager.setBluetoothScoOn(false);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;

        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        if (device == null) {
            Log.d(TAG, "no device for action: " + action);
            return;
        }
        Log.d(TAG, "device: " + device.toString());

        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        switch (action) {
            case BluetoothDevice.ACTION_ACL_CONNECTED:
                Log.d(TAG, "Bluetooth device connected");
                if (audioManager != null) {
                    audioManager.startBluetoothSco();
                    audioManager.setBluetoothScoOn(true);
                    audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                }
                break;

            case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                Log.d(TAG, "Bluetooth device disconnected");
                if (audioManager != null) {
                    audioManager.stopBluetoothSco();
                    audioManager.setBluetoothScoOn(false);
                    audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                }
                break;

            case AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED:
                int state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, AudioManager.SCO_AUDIO_STATE_ERROR);
                if (state == AudioManager.SCO_AUDIO_STATE_CONNECTED) {
                    Log.d(TAG, "SCO audio connected");
                } else if (state == AudioManager.SCO_AUDIO_STATE_DISCONNECTED) {
                    Log.d(TAG, "SCO audio disconnected");
                } else {
                    Log.d(TAG, "SCO audio state updated: " + state);
                }
                break;
        }
    }
}
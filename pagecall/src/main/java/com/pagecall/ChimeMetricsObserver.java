package com.pagecall;

import android.util.Log;

import androidx.annotation.NonNull;

import com.amazonaws.services.chime.sdk.meetings.audiovideo.metric.MetricsObserver;
import com.amazonaws.services.chime.sdk.meetings.audiovideo.metric.ObservableMetric;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

public class ChimeMetricsObserver implements MetricsObserver {
    private WebViewEmitter emitter;

    ChimeMetricsObserver(WebViewEmitter emitter) {
        this.emitter = emitter;
    }

    @Override
    public void onMetricsReceived(@NonNull Map<ObservableMetric, ?> metrics) {
        for (Map.Entry<ObservableMetric, ?> entry : metrics.entrySet()) {
            try {
                ObservableMetric metric = entry.getKey();
                Double value = (Double) entry.getValue();
                JSONObject mediaStat = new JSONObject();
                mediaStat.put("event", "audio");
                mediaStat.put("key", metric.toString());
                mediaStat.put("value", value);
                this.emitter.emit(NativeBridgeEvent.MEDIA_STAT, mediaStat);
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e("MiController", "Error creating JSON object.");
            }
        }
    }
}

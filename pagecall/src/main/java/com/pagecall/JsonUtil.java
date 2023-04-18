package com.pagecall;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

/**
 * Utility Class for JSON-format Manipulation
 */
class JsonUtil {
    /**
     * Nullable Getter considering JSONObject.NULL
     *
     * @return null if {@param object} is empty or {@link JSONObject.NULL}
     */
    @Nullable
    static String getStringNullable(@NonNull JSONObject object, @NonNull String key) {
        @NonNull String optString = object.optString(key);
        if (object.isNull(key) || optString.isEmpty()) return null;
        else return optString;
    }
}
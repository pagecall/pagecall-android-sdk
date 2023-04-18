package com.pagecall;

import org.json.JSONObject;

class NativeBridgeMediaType {
    String mediaType;

    public NativeBridgeMediaType(JSONObject object) {
        this.mediaType = object.optString("mediaType");
    }
}

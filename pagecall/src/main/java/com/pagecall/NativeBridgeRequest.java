package com.pagecall;

enum NativeBridgeRequest {
    PRODUCE("produce"),
    ;

    private final String value;

    NativeBridgeRequest(String value) {
        this.value = value;
    }

    String getValue() {
        return this.value;
    }

    static NativeBridgeRequest fromString(String value) {
        for (NativeBridgeRequest nativeBridgeRequest : NativeBridgeRequest.values()) {
            if (nativeBridgeRequest.value.equals(value)) {
                return nativeBridgeRequest;
            }
        }
        throw new IllegalArgumentException("Invalid value: " + value);
    }
}

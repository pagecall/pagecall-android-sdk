package com.pagecall;

public enum TerminationReason {
    INTERNAL("internal"),
    OTHER ("other")
    ;

    private final String value;

    private String otherReason; // set only if value == "other"

    TerminationReason(String value) {
        this.value = value;
    }

    public String getValue() { return this.value; }
    public String getOtherReason() { return this.otherReason; }

    static TerminationReason fromString(String value) {
        for (TerminationReason reason : TerminationReason.values()) {
            if (reason.value.equals(value)) {
                return reason;
            }
        }
        TerminationReason other = OTHER;
        other.otherReason = value;
        return OTHER;
    }
}

package com.marketinghub.worker.targetingrequest;

public enum TargetingCandidateType {
    INTEREST,
    BEHAVIOR,
    WORK_POSITION;

    public static TargetingCandidateType from(String value) {
        if (value == null) return INTEREST;
        String normalized = value.trim().toUpperCase().replace('-', '_');
        for (TargetingCandidateType type : values()) {
            if (type.name().equalsIgnoreCase(normalized)) {
                return type;
            }
        }
        return INTEREST;
    }
}

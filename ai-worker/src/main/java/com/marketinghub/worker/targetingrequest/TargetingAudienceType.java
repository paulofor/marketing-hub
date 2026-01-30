package com.marketinghub.worker.targetingrequest;

public enum TargetingAudienceType {
    PROSPECT,
    REMARKETING;

    public String orDefault() {
        return name();
    }
}

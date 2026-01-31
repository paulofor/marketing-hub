package com.marketinghub.facebookadsworker.facebooktargeting;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Status possíveis para um candidato após a validação no Facebook.
 */
public enum TargetingCandidateStatus {
    PENDING_FACEBOOK_MATCH,
    VALIDATED,
    NO_MATCH;

    @JsonValue
    public String toJson() {
        return name();
    }
}

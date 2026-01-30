package com.marketinghub.targeting;

/**
 * Status de um candidato de targeting até validação no Facebook.
 */
public enum TargetingCandidateStatus {
    PENDING_FACEBOOK_MATCH,
    VALIDATED,
    NO_MATCH
}

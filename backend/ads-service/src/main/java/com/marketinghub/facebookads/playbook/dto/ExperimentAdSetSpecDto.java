package com.marketinghub.facebookads.playbook.dto;

import com.marketinghub.facebookads.playbook.ExperimentAdSetSpecSlot;

import java.time.Instant;

/**
 * DTO describing each targeting spec.
 */
public record ExperimentAdSetSpecDto(
        Long id,
        ExperimentAdSetSpecSlot slot,
        String label,
        Integer ageMin,
        Integer ageMax,
        String targetingSpec,
        String validationStatus,
        String validationResponse,
        String reachStatus,
        Long reachLowerBound,
        Long reachUpperBound,
        Instant createdAt,
        Instant updatedAt
) {
}

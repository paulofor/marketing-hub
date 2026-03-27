package com.marketinghub.proof.dto;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO representing a cataloged proof artifact.
 */
@Data
public class ProofArtifactDto {
    private Long id;
    private UUID hypothesisId;
    private Long experimentId;
    private Long marketNicheId;
    private String marketNicheName;
    private Long visualProofId;
    private String visualProofName;
    private String stage;
    private String stageLabel;
    private String status;
    private String customType;
    private String typeLabel;
    private String assetPlan;
    private String assetUrl;
    private String message;
    private String deliveryNotes;
    private String prompt;
    private String model;
    private Instant createdAt;
    private Instant updatedAt;
}

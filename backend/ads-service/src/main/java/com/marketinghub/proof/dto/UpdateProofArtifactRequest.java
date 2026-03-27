package com.marketinghub.proof.dto;

import lombok.Data;

/**
 * Partial update payload for proof artifacts.
 */
@Data
public class UpdateProofArtifactRequest {
    private Long visualProofId;
    private String customType;
    private String stage;
    private String status;
    private String assetPlan;
    private String assetUrl;
    private String message;
    private String deliveryNotes;
    private String prompt;
    private String model;
}

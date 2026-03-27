package com.marketinghub.proof.dto;

import lombok.Data;

/**
 * Request payload to register a new proof artifact.
 */
@Data
public class CreateProofArtifactRequest {
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

package com.marketinghub.experiment.dto;

/**
 * Payload used to approve or revoke approval of an email step.
 */
public record UpdateExperimentEmailApprovalRequest(boolean approved) {
}

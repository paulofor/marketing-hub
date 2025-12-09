package com.marketinghub.experiment.dto;

/**
 * Request payload to associate a sample email with an experiment.
 */
public record UpdateSelectedSampleEmailRequest(Long sampleEmailId) {
}

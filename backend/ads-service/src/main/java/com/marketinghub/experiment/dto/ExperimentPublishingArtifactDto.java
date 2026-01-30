package com.marketinghub.experiment.dto;

/** Details about a campaign asset that is pending publication. */
public record ExperimentPublishingArtifactDto(
        String type,
        String id,
        String name,
        String status,
        String externalId
) { }

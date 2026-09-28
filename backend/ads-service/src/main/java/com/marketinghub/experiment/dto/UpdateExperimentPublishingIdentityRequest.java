package com.marketinghub.experiment.dto;

import jakarta.validation.constraints.NotNull;

/** Atualiza somente as identidades oficiais usadas na publicação de um experimento. */
public record UpdateExperimentPublishingIdentityRequest(
    Long facebookPageId, @NotNull Long instagramAccountId) {}

package com.marketinghub.socialmediaworker.dto;

import java.time.Instant;
import java.util.List;

/**
 * Representa a saida funcional auditavel da etapa YouTube.
 */
public record YoutubePublicationOutput(
        Long publicationId,
        String platform,
        YoutubePublicationAction action,
        String status,
        String externalVideoId,
        String externalUrl,
        String businessResult,
        List<String> recommendedNextActions,
        Instant finishedAt) {}

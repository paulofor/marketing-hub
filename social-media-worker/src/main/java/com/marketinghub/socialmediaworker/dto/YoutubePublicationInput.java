package com.marketinghub.socialmediaworker.dto;

import java.util.List;

/**
 * Representa a entrada funcional de uma publicacao ou planejamento no YouTube.
 */
public record YoutubePublicationInput(
        Long publicationId,
        Long productId,
        Long experimentId,
        String channelId,
        YoutubePublicationAction action,
        String videoSourceUrl,
        String title,
        String description,
        List<String> tags,
        String privacyStatus,
        String playlistTitle,
        String marketObjective) {}

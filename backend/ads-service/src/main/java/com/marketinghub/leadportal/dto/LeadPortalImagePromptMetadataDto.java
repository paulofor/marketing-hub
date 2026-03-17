package com.marketinghub.leadportal.dto;

import java.util.List;

/**
 * Metadata that helps the UI build the image prompt editor.
 */
public record LeadPortalImagePromptMetadataDto(
        String defaultTemplate,
        String defaultModel,
        int defaultBatchSize,
        List<LeadPortalImagePromptPlaceholderDto> placeholders) {}

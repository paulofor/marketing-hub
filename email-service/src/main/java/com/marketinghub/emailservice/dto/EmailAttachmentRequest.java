package com.marketinghub.emailservice.dto;

import jakarta.validation.constraints.NotBlank;

public record EmailAttachmentRequest(
        @NotBlank String id,
        @NotBlank String fileName,
        @NotBlank String contentType,
        boolean inline,
        String contentId,
        String variant,
        String resourceUrl,
        boolean download
) {
}

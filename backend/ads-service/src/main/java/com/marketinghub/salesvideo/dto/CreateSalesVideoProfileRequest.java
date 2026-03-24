package com.marketinghub.salesvideo.dto;

import com.marketinghub.salesvideo.SalesVideoKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Payload para criação de perfis de Avatar Sales Video.
 */
@Data
public class CreateSalesVideoProfileRequest {
    @NotNull
    private SalesVideoKind videoKind;

    @NotBlank
    @Size(max = 255)
    private String title;

    @Size(max = 255)
    private String personaName;

    @Size(max = 255)
    private String personaStyle;

    @Size(max = 255)
    private String voiceStyle;

    @Size(max = 64)
    private String language;

    private Integer targetDurationSeconds;

    private Long landingPageId;
}

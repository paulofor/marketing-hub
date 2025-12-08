package com.marketinghub.sampleemail.dto;

import java.time.Instant;
import lombok.Data;

/**
 * DTO para {@link com.marketinghub.sampleemail.SampleEmail}.
 */
@Data
public class SampleEmailDto {
    private Long id;
    private Long experimentId;
    private String subject;
    private String previewText;
    private String body;
    private String callToAction;
    private String model;
    private String prompt;
    private Instant createdAt;
    private Instant updatedAt;
}

package com.marketinghub.sampleemail.dto;

import lombok.Data;

/**
 * Payload para criação de um email de amostra.
 */
@Data
public class CreateSampleEmailRequest {
    private String subject;
    private String previewText;
    private String body;
    private String callToAction;
    private String model;
    private String prompt;
}

package com.marketinghub.salesvideo.dto;

import lombok.Data;

/**
 * Resultado textual devolvido pelo ai-worker para jobs de script.
 */
@Data
public class GeneratedScriptResultPayload {
    private String scriptText;
    private String hookText;
    private String ctaText;
    private String captionText;
    private String storyboardJson;
    private String prompt;
    private String model;
}

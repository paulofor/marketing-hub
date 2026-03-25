package com.marketinghub.salesvideo.dto;

import com.marketinghub.salesvideo.SalesVideoScriptSource;
import com.marketinghub.salesvideo.SalesVideoScriptStatus;
import lombok.Data;

import java.time.Instant;

/**
 * DTO simplificado de script.
 */
@Data
public class SalesVideoScriptDto {
    private Long id;
    private Integer version;
    private String scriptText;
    private String hookText;
    private String ctaText;
    private String captionText;
    private String storyboardJson;
    private SalesVideoScriptSource source;
    private String model;
    private String prompt;
    private SalesVideoScriptStatus status;
    private String approvedBy;
    private Instant approvedAt;
    private Instant createdAt;
}

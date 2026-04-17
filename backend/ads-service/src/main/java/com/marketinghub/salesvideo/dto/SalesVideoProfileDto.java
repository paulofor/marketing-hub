package com.marketinghub.salesvideo.dto;

import com.marketinghub.salesvideo.SalesVideoKind;
import com.marketinghub.salesvideo.SalesVideoStatus;
import lombok.Data;

import java.time.Instant;

/**
 * Representação REST de um perfil de vídeo de venda.
 */
@Data
public class SalesVideoProfileDto {
    private Long id;
    private Long productId;
    private Long landingPageId;
    private String tenantId;
    private String createdBy;
    private SalesVideoKind videoKind;
    private String title;
    private String personaName;
    private String personaStyle;
    private String voiceStyle;
    private String language;
    private Integer targetDurationSeconds;
    private boolean requiresConsent;
    private String consentRecordedBy;
    private Instant consentRecordedAt;
    private String consentEvidenceUrl;
    private String humanReviewApprovedBy;
    private Instant humanReviewApprovedAt;
    private String complianceNotes;
    private SalesVideoStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private SalesVideoScriptDto latestScript;
    private SalesVideoJobDto lastJob;
}

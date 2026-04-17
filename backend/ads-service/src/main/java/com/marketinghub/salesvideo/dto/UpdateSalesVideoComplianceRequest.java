package com.marketinghub.salesvideo.dto;

import lombok.Data;

/**
 * Atualiza o checklist mínimo de compliance do perfil de vídeo.
 */
@Data
public class UpdateSalesVideoComplianceRequest {
    private Boolean requiresConsent;
    private String consentRecordedBy;
    private String consentEvidenceUrl;
    private Boolean humanReviewApproved;
    private String humanReviewApprovedBy;
    private String complianceNotes;
}

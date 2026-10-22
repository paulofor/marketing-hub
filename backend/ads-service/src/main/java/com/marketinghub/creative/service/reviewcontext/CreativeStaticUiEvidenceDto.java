package com.marketinghub.creative.service.reviewcontext;

/**
 * Responsabilidade: identificar a captura própria do PDE sem apresentá-la como cliente ou licença
 * de terceiros.
 */
public record CreativeStaticUiEvidenceDto(
    String contractVersion,
    String verificationStatus,
    Long productId,
    Long cycleId,
    Long experimentId,
    String productVersion,
    Long sourceTaskId,
    Long sourceArtifactId,
    Long copySourceTaskId,
    Long copySourceArtifactId,
    String mediaUrl,
    String sha256,
    String rendererVersion,
    String rendererSha256,
    String sourceUrl,
    String inspectedUrl,
    String capturedAt,
    String sourceType,
    boolean testData,
    boolean commercialEvidenceEligible,
    boolean commercialApprovalGranted) {}

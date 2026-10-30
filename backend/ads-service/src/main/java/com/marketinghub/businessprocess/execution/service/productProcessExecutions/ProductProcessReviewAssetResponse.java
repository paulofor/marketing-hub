package com.marketinghub.businessprocess.execution.service.productProcessExecutions;

/** Responsabilidade: apresentar o arquivo exato revisado antes da decisão humana de uso. */
public record ProductProcessReviewAssetResponse(
    Long taskId, Long artifactId, String title, String contentUrl, String sha256) {}

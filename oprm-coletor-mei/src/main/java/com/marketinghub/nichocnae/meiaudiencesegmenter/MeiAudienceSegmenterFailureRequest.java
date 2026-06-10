package com.marketinghub.nichocnae.meiaudiencesegmenter;

/** Payload de falha operacional enviado ao backend pela etapa de segmentação MEI/autônomo. */
public record MeiAudienceSegmenterFailureRequest(String errorMessage) {}

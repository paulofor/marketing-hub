package com.marketinghub.nichocnae.signalextractor;

/** Representa a falha enviada ao backend quando a etapa cinco não consegue extrair sinais. */
public record SignalExtractorFailureRequest(String errorMessage) {}

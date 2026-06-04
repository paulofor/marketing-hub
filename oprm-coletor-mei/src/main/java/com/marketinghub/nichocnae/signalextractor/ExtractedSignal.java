package com.marketinghub.nichocnae.signalextractor;

/** Representa um sinal classificado extraído localmente de um snapshot curto público. */
public record ExtractedSignal(String signalType, String signalText, String evidenceExcerpt, Integer confidenceScore) {}

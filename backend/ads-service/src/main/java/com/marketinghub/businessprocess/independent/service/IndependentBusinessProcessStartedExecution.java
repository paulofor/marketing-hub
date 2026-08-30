package com.marketinghub.businessprocess.independent.service;

/** Resultado interno que correlaciona o adaptador com a referência BPM persistida. */
public record IndependentBusinessProcessStartedExecution(
    String sourceReference, String displayName) {}

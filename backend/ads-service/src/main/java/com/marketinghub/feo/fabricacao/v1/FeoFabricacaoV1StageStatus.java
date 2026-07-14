package com.marketinghub.feo.fabricacao.v1;

/** Responsabilidade: enumerar os estados operacionais de uma execução de etapa da FEO v1. */
public enum FeoFabricacaoV1StageStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    BLOCKED,
    FAILED
}

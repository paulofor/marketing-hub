package com.marketinghub.feo.fabricacao.v1.dto;

/** Responsabilidade: receber falha técnica publicada pelo worker FEO. */
public record FeoFabricacaoV1FailureRequest(String workerId, String jobId, String error) {}

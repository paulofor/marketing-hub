package com.marketinghub.feo.fabricacao.v1.dto;

/** Responsabilidade: representar a resposta de criação de uma solicitação FEO para experimento. */
public record FeoFabricacaoV1StartResponse(Long executionId, String jobId, String stageCode, String status) {
}

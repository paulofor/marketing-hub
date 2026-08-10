package com.marketinghub.agent.service.uploadportrait;

/** Responsabilidade: informar a referência persistida de uma imagem de agente enviada. */
public record AgentPortraitUploadResponse(Long assetId, String url) {}

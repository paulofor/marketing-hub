package com.marketinghub.facebookads.service.publicationstep;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

/**
 * Contrato de entrada para registrar um passo da publicação de campanha executado pelo worker.
 */
public record FacebookCampaignPublicationJobStepRequest(
        String jobId,
        Long experimentId,
        String stepName,
        String provider,
        String endpoint,
        String httpMethod,
        Integer statusCode,
        JsonNode requestPayload,
        JsonNode responsePayload,
        String errorMessage,
        Instant occurredAt) {}

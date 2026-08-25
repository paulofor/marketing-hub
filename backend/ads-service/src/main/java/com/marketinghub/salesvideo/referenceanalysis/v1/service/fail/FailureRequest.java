package com.marketinghub.salesvideo.referenceanalysis.v1.service.fail;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;

/** Contrato de falha auditável retornado pelo executor. */
public record FailureRequest(
    @NotBlank String producerExecutionId,
    @NotBlank String error,
    JsonNode artifacts,
    JsonNode rawRequest,
    JsonNode rawResponse,
    String model) {}

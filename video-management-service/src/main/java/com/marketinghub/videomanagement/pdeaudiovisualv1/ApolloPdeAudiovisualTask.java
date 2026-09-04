package com.marketinghub.videomanagement.pdeaudiovisualv1;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

/** Responsabilidade: representar o contrato BPM mínimo entregue a Apolo para a etapa audiovisual. */
public record ApolloPdeAudiovisualTask(
        Long taskId,
        String agentKey,
        String processCode,
        Integer processVersion,
        String activityId,
        String activityName,
        String title,
        String description,
        String sourceReference,
        Instant receivedAt,
        ExecutionResource executionResource,
        TaskTarget taskTarget,
        String processContextJson) {

    /** Responsabilidade: identificar o recurso especializado exigido pela atividade. */
    public record ExecutionResource(
            String resourceCode,
            String name,
            String resourceType,
            String executorReference,
            String usageInstructions) {
    }

    /** Responsabilidade: transportar a identidade do produto e o contrato PDE versionado. */
    public record TaskTarget(
            String sourceReference,
            Long experimentId,
            Long productId,
            String productSlug,
            String productName,
            String productInternalName,
            String experienceVersion,
            String publicUrl,
            JsonNode pdeContext) {
    }
}

package com.marketinghub.videomanagement.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.videomanagement.client.BackendVideoClient;
import com.marketinghub.videomanagement.client.dto.SalesVideoJob;
import com.marketinghub.videomanagement.client.dto.SalesVideoJobType;
import com.marketinghub.videomanagement.service.provider.VideoProviderException;
import org.springframework.stereotype.Component;

/** Responsabilidade: restaurar no job de pós-produção o contrato auditado do Product UGC fonte. */
@Component
public class ProductUgcPostProductionContractResolver {
    private static final String PRODUCT_UGC_STRATEGY =
            "RUNWAY_PRODUCT_UGC_WITH_DETERMINISTIC_POST_PRODUCTION";
    private final BackendVideoClient backendClient;
    private final ObjectMapper objectMapper;

    /** Configura o acesso ao job fonte canônico e o parser de seus metadados. */
    public ProductUgcPostProductionContractResolver(
            BackendVideoClient backendClient, ObjectMapper objectMapper) {
        this.backendClient = backendClient;
        this.objectMapper = objectMapper;
    }

    /** Hidrata duração e política de cortes a partir da fonte já medida por Apolo. */
    public SalesVideoJob resolve(SalesVideoJob job) {
        if (job.jobType() != SalesVideoJobType.POST_PRODUCTION) {
            return job;
        }
        ObjectNode downstream = readObject(job.metadataJson(), job.id());
        if (!PRODUCT_UGC_STRATEGY.equalsIgnoreCase(
                downstream.path("generation_strategy").asText())) return job;
        long sourceJobId = downstream.path("sourceJobId").asLong(0);
        if (sourceJobId <= 0) {
            throw invalidContract(job.id(), "job fonte ausente");
        }
        SalesVideoJob sourceJob = backendClient.fetchJob(sourceJobId);
        ObjectNode source = readObject(sourceJob.metadataJson(), sourceJob.id());
        validateLineage(job.id(), downstream, source);
        int targetDuration = source.path("targetDurationSeconds").asInt(0);
        JsonNode audit = source.path("apollo_technical_quality");
        if (targetDuration < 4
                || targetDuration > 15
                || !"APPROVED".equalsIgnoreCase(audit.path("stability_status").asText())
                || !"FFMPEG_SCENE_AWARE_VIDSTAB_GLOBAL_MOTION_DELTA"
                        .equals(audit.path("method").asText())) {
            throw invalidContract(job.id(), "duração ou parecer técnico da fonte inválido");
        }
        downstream.put("targetDurationSeconds", targetDuration);
        copyIfPresent(source, downstream, "sceneCount");
        copyIfPresent(source, downstream, "assemblyRequired");
        copyIfPresent(source, downstream, "runwayRouterConfigId");
        copyIfPresent(source, downstream, "runwayRouterRequestsJson");
        downstream.set("apollo_technical_quality", audit.deepCopy());
        downstream.set(
                "technicalQualityGate",
                effectiveTechnicalGate(source.path("technicalQualityGate"), audit));
        ObjectNode hydration = downstream.putObject("source_contract_hydration");
        hydration.put("status", "RESTORED_FROM_CANONICAL_SOURCE_JOB");
        hydration.put("source_job_id", sourceJobId);
        hydration.put("target_duration_seconds", targetDuration);
        return withMetadata(job, downstream.toString());
    }

    /** Mantém limites planejados e substitui apenas a semântica confirmada pelo parecer técnico. */
    private ObjectNode effectiveTechnicalGate(JsonNode plannedGate, JsonNode audit) {
        ObjectNode gate = plannedGate.isObject()
                ? ((ObjectNode) plannedGate).deepCopy()
                : objectMapper.createObjectNode();
        boolean cutsAllowed = audit.path("intentional_scene_cuts_allowed").asBoolean(false);
        gate.put("continuousTakeRequired", !cutsAllowed);
        gate.put("intentionalSceneCutsAllowed", cutsAllowed);
        gate.put("maximumSceneCuts", audit.path("maximum_scene_cuts").asInt(0));
        return gate;
    }

    /** Bloqueia a mistura de contratos pertencentes a ciclos, projetos ou experimentos diferentes. */
    private void validateLineage(Long jobId, JsonNode downstream, JsonNode source) {
        for (String field : new String[] {
            "videoProductionCycleId", "videoProjectId", "productId", "experimentId"
        }) {
            if (downstream.hasNonNull(field)
                    && source.hasNonNull(field)
                    && !downstream.get(field).equals(source.get(field))) {
                throw invalidContract(jobId, "linhagem divergente em " + field);
            }
        }
        if (!PRODUCT_UGC_STRATEGY.equalsIgnoreCase(
                source.path("generation_strategy").asText())) {
            throw invalidContract(jobId, "estratégia da fonte divergente");
        }
    }

    /** Copia um campo governado somente quando ele existe na fonte persistida. */
    private void copyIfPresent(ObjectNode source, ObjectNode target, String field) {
        if (source.has(field)) {
            target.set(field, source.get(field).deepCopy());
        }
    }

    /** Lê metadados como objeto e falha fechado diante de contrato ausente ou corrompido. */
    private ObjectNode readObject(String value, Long jobId) {
        try {
            JsonNode parsed = objectMapper.readTree(value);
            if (parsed != null && parsed.isObject()) {
                return (ObjectNode) parsed;
            }
        } catch (Exception ex) {
            throw new VideoProviderException(
                    "APOLLO_POST_PRODUCTION_CONTRACT_INVALID",
                    "Contrato Product UGC inválido no job " + jobId,
                    ex);
        }
        throw invalidContract(jobId, "metadados ausentes");
    }

    /** Cria falha funcional que impede finalizar vídeo com contrato incompleto. */
    private VideoProviderException invalidContract(Long jobId, String reason) {
        return new VideoProviderException(
                "APOLLO_POST_PRODUCTION_CONTRACT_INVALID",
                "Contrato Product UGC inválido no job " + jobId + ": " + reason + ".");
    }

    /** Copia o record imutável substituindo apenas os metadados restaurados. */
    private SalesVideoJob withMetadata(SalesVideoJob job, String metadata) {
        return new SalesVideoJob(
                job.id(),
                job.profileId(),
                job.scriptId(),
                job.tenantId(),
                job.providerFamily(),
                job.providerName(),
                job.providerJobId(),
                job.jobType(),
                job.status(),
                job.retryAttempt(),
                job.retryReason(),
                job.retryOfJobId(),
                job.retryNotes(),
                job.progressPercent(),
                job.failureCode(),
                job.failureDetail(),
                job.requestedBy(),
                job.requestedAt(),
                job.startedAt(),
                job.finishedAt(),
                job.expiresAt(),
                job.assetId(),
                job.posterAssetId(),
                job.vttAssetId(),
                metadata,
                job.createdAt(),
                job.updatedAt());
    }
}

package com.marketinghub.feo.fabricacao.v1.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.deliverable.Deliverable;
import com.marketinghub.deliverable.DeliverablePackage;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.feo.fabricacao.v1.FeoFabricacaoV1StageExecution;
import com.marketinghub.feo.fabricacao.v1.FeoFabricacaoV1StageStatus;
import com.marketinghub.feo.fabricacao.v1.dto.FeoFabricacaoV1CompleteRequest;
import com.marketinghub.feo.fabricacao.v1.dto.FeoFabricacaoV1ExecutionSummaryResponse;
import com.marketinghub.feo.fabricacao.v1.dto.FeoFabricacaoV1FailureRequest;
import com.marketinghub.feo.fabricacao.v1.dto.FeoFabricacaoV1PendingResponse;
import com.marketinghub.feo.fabricacao.v1.dto.FeoFabricacaoV1StartResponse;
import com.marketinghub.feo.fabricacao.v1.repository.FeoFabricacaoV1StageExecutionRepository;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.repository.jpa.deliverable.DeliverablePackageRepository;
import com.marketinghub.repository.jpa.deliverable.DeliverableRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import jakarta.persistence.EntityNotFoundException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Responsabilidade: orquestrar a fila backend da FEO v1 para fabricação de entregáveis de experimentos. */
@Service
public class FeoFabricacaoV1Service {

    private static final Logger log = LoggerFactory.getLogger(FeoFabricacaoV1Service.class);
    private static final String STAGE_PLANEJAMENTO = "planejamento-entregaveis";
    private static final String STAGE_MONTAGEM = "montagem-pacote";
    private static final Duration RUNNING_RETRY_AFTER = Duration.ofMinutes(15);
    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<Map<String, Object>>> ARTIFACT_LIST_TYPE = new TypeReference<>() {};

    private final ExperimentRepository experimentRepository;
    private final DeliverableRepository deliverableRepository;
    private final DeliverablePackageRepository deliverablePackageRepository;
    private final FeoFabricacaoV1StageExecutionRepository executionRepository;
    private final ObjectMapper objectMapper;

    /** Inicializa o serviço com repositórios canônicos e serializador JSON. */
    public FeoFabricacaoV1Service(
            ExperimentRepository experimentRepository,
            DeliverableRepository deliverableRepository,
            DeliverablePackageRepository deliverablePackageRepository,
            FeoFabricacaoV1StageExecutionRepository executionRepository,
            ObjectMapper objectMapper) {
        this.experimentRepository = experimentRepository;
        this.deliverableRepository = deliverableRepository;
        this.deliverablePackageRepository = deliverablePackageRepository;
        this.executionRepository = executionRepository;
        this.objectMapper = objectMapper;
    }

    /** Cria a etapa inicial de fabricação de entregáveis para um experimento. */
    @Transactional
    public FeoFabricacaoV1StartResponse startForExperiment(Long experimentId) {
        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new EntityNotFoundException("Experiment not found: " + experimentId));
        if (hasActiveInitialExecution(experimentId)) {
            FeoFabricacaoV1StageExecution active = executionRepository
                    .findTop20ByExperimentIdOrderByCreatedAtDesc(experimentId)
                    .stream()
                    .filter(item -> STAGE_PLANEJAMENTO.equals(item.getStageCode()))
                    .filter(item -> item.getStatus() == FeoFabricacaoV1StageStatus.PENDING
                            || item.getStatus() == FeoFabricacaoV1StageStatus.RUNNING)
                    .findFirst()
                    .orElseThrow();
            return toStartResponse(active);
        }

        FeoFabricacaoV1StageExecution execution = FeoFabricacaoV1StageExecution.builder()
                .experiment(experiment)
                .jobId("feo-exp-" + experiment.getId() + "-" + UUID.randomUUID())
                .stageCode(STAGE_PLANEJAMENTO)
                .status(FeoFabricacaoV1StageStatus.PENDING)
                .inputPayload(toJson(buildFabricationContext(experiment)))
                .build();
        return toStartResponse(executionRepository.save(execution));
    }

    /** Lista execuções recentes da FEO para o experimento. */
    @Transactional(readOnly = true)
    public List<FeoFabricacaoV1ExecutionSummaryResponse> listByExperiment(Long experimentId) {
        return executionRepository.findTop20ByExperimentIdOrderByCreatedAtDesc(experimentId)
                .stream()
                .map(this::toSummary)
                .toList();
    }

    /** Lista pendências de uma etapa para consumo pelo worker FEO. */
    @Transactional
    public List<FeoFabricacaoV1PendingResponse> listPending(String stageCode, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        return executionRepository
                .findPendingOrStaleRunning(
                        stageCode,
                        Instant.now().minus(RUNNING_RETRY_AFTER),
                        PageRequest.of(0, safeLimit))
                .stream()
                .map(this::markRunningAndMap)
                .toList();
    }

    /** Registra conclusão, persiste saída e enfileira próxima etapa quando o contrato permitir. */
    @Transactional
    public void complete(String stageCode, Long executionId, FeoFabricacaoV1CompleteRequest request) {
        FeoFabricacaoV1StageExecution execution = findExecution(stageCode, executionId);
        execution.setWorkerId(request.workerId());
        execution.setOutputPayload(toJson(request.output()));
        execution.setArtifactsPayload(toJson(request.artifacts()));
        execution.setMetricsPayload(toJson(request.metrics()));
        execution.setBlockReason(trimToNull(request.blockReason()));
        execution.setNextStageCode(trimToNull(request.nextStageCode()));
        execution.setFinishedAt(Instant.now());
        execution.setStatus(resolveCompletedStatus(request));
        executionRepository.save(execution);

        if (execution.getStatus() == FeoFabricacaoV1StageStatus.COMPLETED) {
            enqueueNextStageWhenAllowed(execution, request);
            materializePackageWhenFinal(stageCode, execution, request);
        }
    }

    /** Registra falha técnica reportada pelo worker FEO. */
    @Transactional
    public void fail(String stageCode, Long executionId, FeoFabricacaoV1FailureRequest request) {
        FeoFabricacaoV1StageExecution execution = findExecution(stageCode, executionId);
        execution.setWorkerId(request.workerId());
        execution.setErrorMessage(trimToNull(request.error()));
        execution.setFinishedAt(Instant.now());
        execution.setStatus(FeoFabricacaoV1StageStatus.FAILED);
        executionRepository.save(execution);
    }

    /** Marca a execução como assumida e converte para o contrato pending do worker. */
    private FeoFabricacaoV1PendingResponse markRunningAndMap(FeoFabricacaoV1StageExecution execution) {
        execution.setStatus(FeoFabricacaoV1StageStatus.RUNNING);
        execution.setStartedAt(Instant.now());
        FeoFabricacaoV1StageExecution saved = executionRepository.save(execution);
        return new FeoFabricacaoV1PendingResponse(
                saved.getJobId(),
                String.valueOf(saved.getId()),
                fromJson(saved.getInputPayload(), MAP_TYPE),
                Map.of("experimentId", saved.getExperiment().getId()));
    }

    /** Monta contexto mínimo de fabricação usando experimento, hipótese e pacote de entregáveis existente. */
    private Map<String, Object> buildFabricationContext(Experiment experiment) {
        Hypothesis hypothesis = experiment.getHypothesisRef();
        DeliverablePackage latestPackage = latestPackage(experiment.getId());
        List<String> deliverables = latestPackage != null
                ? latestPackage.getDeliverables().stream().map(Deliverable::getTitle).toList()
                : fallbackDeliverables(hypothesis);

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("requestId", "experiment-" + experiment.getId());
        context.put("experimentId", String.valueOf(experiment.getId()));
        context.put("offerName", latestPackage != null ? latestPackage.getName() : experiment.getName());
        context.put("niche", experiment.getNiche() != null ? experiment.getNiche().getName() : null);
        context.put("centralPromise", firstText(experiment.getFunnelPromise(), hypothesis != null ? hypothesis.getPromise() : null, experiment.getHypothesis()));
        context.put("promisedResult", firstText(experiment.getFreeReward(), experiment.getSinglePain(), hypothesis != null ? hypothesis.getEntrega() : null));
        context.put("coreMechanism", firstText(hypothesis != null ? hypothesis.getUniqueMechanism() : null, hypothesis != null ? hypothesis.getMechanism() : null));
        context.put("proofSummary", firstText(experiment.getLandingPageQualityReview(), hypothesis != null ? hypothesis.getSuccessRule() : null));
        context.put("deliverables", deliverables);
        context.put("validationSignals", List.of(
                "Gate atual permite definição de oferta e fabricação de entregáveis.",
                "FEO não libera tráfego nem altera promessa comercial validada."));
        return context;
    }

    /** Busca o pacote mais recente vinculado ao experimento. */
    private DeliverablePackage latestPackage(Long experimentId) {
        return deliverablePackageRepository.findByExperimentIdOrderByCreatedAtDesc(experimentId)
                .stream()
                .findFirst()
                .orElse(null);
    }

    /** Usa a entrega textual da hipótese como fallback quando ainda não há pacote vinculado. */
    private List<String> fallbackDeliverables(Hypothesis hypothesis) {
        if (hypothesis == null || !StringUtils.hasText(hypothesis.getEntrega())) {
            return List.of();
        }
        return List.of(hypothesis.getEntrega().trim());
    }

    /** Enfileira montagem do pacote após o planejamento retornar um plano aprovado pelo contrato. */
    private void enqueueNextStageWhenAllowed(FeoFabricacaoV1StageExecution execution, FeoFabricacaoV1CompleteRequest request) {
        if (!STAGE_PLANEJAMENTO.equals(execution.getStageCode()) || !STAGE_MONTAGEM.equals(request.nextStageCode())) {
            return;
        }
        Map<String, Object> assemblyInput = new LinkedHashMap<>();
        assemblyInput.put("context", fromJson(execution.getInputPayload(), MAP_TYPE));
        assemblyInput.put("plan", request.output());
        FeoFabricacaoV1StageExecution next = FeoFabricacaoV1StageExecution.builder()
                .experiment(execution.getExperiment())
                .jobId(execution.getJobId())
                .stageCode(STAGE_MONTAGEM)
                .status(FeoFabricacaoV1StageStatus.PENDING)
                .inputPayload(toJson(assemblyInput))
                .build();
        executionRepository.save(next);
    }

    /** Materializa o pacote final da FEO em entregáveis consumíveis pelo experimento. */
    private void materializePackageWhenFinal(
            String stageCode,
            FeoFabricacaoV1StageExecution execution,
            FeoFabricacaoV1CompleteRequest request) {
        if (!STAGE_MONTAGEM.equals(stageCode)) {
            return;
        }
        Map<String, Object> output = objectMapper.convertValue(request.output(), MAP_TYPE);
        Map<String, Object> manifest = objectMapper.convertValue(output.get("manifest"), MAP_TYPE);
        String packageTitle = stringValue(manifest.get("packageTitle"), "Pacote FEO - Experimento " + execution.getExperiment().getId())
                + " - FEO #" + execution.getId();
        List<Map<String, Object>> items = objectMapper.convertValue(manifest.get("items"), ARTIFACT_LIST_TYPE);
        LinkedHashSet<Deliverable> deliverables = new LinkedHashSet<>();
        for (Map<String, Object> item : items) {
            Deliverable deliverable = deliverableRepository.save(Deliverable.builder()
                    .niche(execution.getExperiment().getNiche())
                    .title(stringValue(item.get("fileName"), "Entregável FEO"))
                    .description(stringValue(item.get("role"), "Entregável final fabricado pela FEO."))
                    .content(toJson(item))
                    .model("feo.fabricacao.v1")
                    .prompt("Fabricado pela FEO a partir do contexto validado do experimento.")
                    .build());
            deliverables.add(deliverable);
        }
        deliverablePackageRepository.save(DeliverablePackage.builder()
                .experiment(execution.getExperiment())
                .name(packageTitle)
                .description(toJson(output.get("report")))
                .model("feo.fabricacao.v1")
                .prompt("Pacote final materializado pela etapa montagem-pacote da FEO.")
                .deliverables(deliverables)
                .build());
    }

    /** Localiza execução pelo id e etapa informada no callback. */
    private FeoFabricacaoV1StageExecution findExecution(String stageCode, Long executionId) {
        return executionRepository.findByIdAndStageCode(executionId, stageCode)
                .orElseThrow(() -> new EntityNotFoundException("FEO execution not found: " + executionId));
    }

    /** Indica se já existe planejamento pendente ou em execução para o experimento. */
    private boolean hasActiveInitialExecution(Long experimentId) {
        return executionRepository.existsByExperimentIdAndStageCodeAndStatusIn(
                experimentId,
                STAGE_PLANEJAMENTO,
                List.of(FeoFabricacaoV1StageStatus.PENDING, FeoFabricacaoV1StageStatus.RUNNING));
    }

    /** Resolve status final a partir do contrato do worker. */
    private FeoFabricacaoV1StageStatus resolveCompletedStatus(FeoFabricacaoV1CompleteRequest request) {
        if ("BLOCKED".equalsIgnoreCase(request.status()) || StringUtils.hasText(request.blockReason())) {
            return FeoFabricacaoV1StageStatus.BLOCKED;
        }
        return FeoFabricacaoV1StageStatus.COMPLETED;
    }

    /** Converte entidade para resposta de início. */
    private FeoFabricacaoV1StartResponse toStartResponse(FeoFabricacaoV1StageExecution execution) {
        return new FeoFabricacaoV1StartResponse(
                execution.getId(),
                execution.getJobId(),
                execution.getStageCode(),
                execution.getStatus().name());
    }

    /** Converte entidade para resumo operacional. */
    private FeoFabricacaoV1ExecutionSummaryResponse toSummary(FeoFabricacaoV1StageExecution execution) {
        return new FeoFabricacaoV1ExecutionSummaryResponse(
                execution.getId(),
                execution.getJobId(),
                execution.getStageCode(),
                execution.getStatus().name(),
                execution.getBlockReason(),
                execution.getErrorMessage(),
                execution.getCreatedAt(),
                execution.getFinishedAt());
    }

    /** Serializa objeto para JSON preservando falha com stack no chamador transacional. */
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            log.error("Falha ao serializar payload FEO valueType={}", value != null ? value.getClass().getName() : null, ex);
            throw new IllegalStateException("Falha ao serializar payload FEO", ex);
        }
    }

    /** Desserializa JSON para o tipo solicitado. */
    private <T> T fromJson(String json, TypeReference<T> type) {
        try {
            return objectMapper.readValue(json.getBytes(StandardCharsets.UTF_8), type);
        } catch (Exception ex) {
            log.error("Falha ao ler payload FEO jsonLength={}", json != null ? json.length() : 0, ex);
            throw new IllegalStateException("Falha ao ler payload FEO", ex);
        }
    }

    /** Retorna o primeiro texto preenchido. */
    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    /** Normaliza texto vazio como nulo. */
    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /** Converte valor arbitrário em string com fallback. */
    private String stringValue(Object value, String fallback) {
        return value == null || !StringUtils.hasText(String.valueOf(value)) ? fallback : String.valueOf(value);
    }
}

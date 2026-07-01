package com.marketinghub.gerasalespage.v1.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.gerasalespage.v1.GeraSalesPagePublicationAudit;
import com.marketinghub.gerasalespage.v1.GeraSalesPagePublicationStageAudit;
import com.marketinghub.gerasalespage.v1.GeraSalesPageStageCode;
import com.marketinghub.gerasalespage.v1.GeraSalesPageStageExecution;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.gerasalespage.v1.GeraSalesPagePublicationAuditRepository;
import com.marketinghub.repository.jpa.gerasalespage.v1.GeraSalesPagePublicationStageAuditRepository;
import com.marketinghub.repository.jpa.gerasalespage.v1.GeraSalesPageStageExecutionRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Responsabilidade: criar e consultar snapshots historicos das paginas publicadas pelo GeraSalesPage v1. */
@Service
public class GeraSalesPagePublicationAuditService {
    private static final Logger log = LoggerFactory.getLogger(GeraSalesPagePublicationAuditService.class);
    private static final String STATUS_COMPLETED = "CONCLUIDO";

    private final ExperimentRepository experimentRepository;
    private final GeraSalesPageStageExecutionRepository executionRepository;
    private final GeraSalesPagePublicationAuditRepository publicationRepository;
    private final GeraSalesPagePublicationStageAuditRepository publicationStageRepository;
    private final ObjectMapper objectMapper;

    /** Inicializa o service com repositorios e serializador usados nos snapshots. */
    public GeraSalesPagePublicationAuditService(
            ExperimentRepository experimentRepository,
            GeraSalesPageStageExecutionRepository executionRepository,
            GeraSalesPagePublicationAuditRepository publicationRepository,
            GeraSalesPagePublicationStageAuditRepository publicationStageRepository,
            ObjectMapper objectMapper) {
        this.experimentRepository = experimentRepository;
        this.executionRepository = executionRepository;
        this.publicationRepository = publicationRepository;
        this.publicationStageRepository = publicationStageRepository;
        this.objectMapper = objectMapper;
    }

    /** Cria snapshot da publicacao final quando a etapa de pacote termina com sucesso. */
    @Transactional
    public void snapshotPublication(GeraSalesPageStageExecution publicationExecution) {
        if (!isCompletedPublication(publicationExecution)
                || publicationRepository.existsByPublicationJobId(publicationExecution.getIdJob())) {
            return;
        }
        Experiment experiment = experimentRepository.findById(publicationExecution.getExperimentId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Experiment not found: " + publicationExecution.getExperimentId()));
        saveAudit(experiment, publicationExecution);
    }

    /** Lista publicacoes historicas e faz backfill quando houver execucao final antiga sem snapshot. */
    @Transactional
    public List<GeraSalesPagePublicationResponse> listPublications(Long experimentId) {
        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new EntityNotFoundException("Experiment not found: " + experimentId));
        backfillMissingSnapshots(experiment);
        return publicationRepository.findByExperimentIdOrderByPublishedAtDesc(experimentId).stream()
                .map(this::toResponse)
                .toList();
    }

    /** Cria snapshots para publicacoes antigas que ainda possuem execucoes auditaveis no banco. */
    private void backfillMissingSnapshots(Experiment experiment) {
        executionRepository.findByExperimentIdAndStageCodeAndStatusOrderByExecutionRequestedAtAsc(
                        experiment.getId(), GeraSalesPageStageCode.PUBLICATION_PACKAGE.code(), STATUS_COMPLETED)
                .stream()
                .filter(execution -> !publicationRepository.existsByPublicationJobId(execution.getIdJob()))
                .forEach(execution -> saveAudit(experiment, execution));
    }

    /** Salva uma publicação e suas etapas auditadas em tabelas normalizadas. */
    private void saveAudit(
            Experiment experiment,
            GeraSalesPageStageExecution publicationExecution) {
        List<GeraSalesPageStageExecution> stageExecutions =
                executionRepository.findByExperimentIdOrderByExecutionRequestedAtAsc(experiment.getId()).stream()
                        .filter(execution -> GeraSalesPageStageCode.contains(execution.getStageCode()))
                        .filter(execution -> STATUS_COMPLETED.equals(execution.getStatus()))
                        .filter(execution -> !execution.getExecutionRequestedAt()
                                .isAfter(publicationExecution.getExecutionRequestedAt()))
                        .toList();
        String packageJson = publicationExecution.getModelResponse();
        Map<String, Object> packagePayload = parseObject(packageJson);
        String html = stringValue(packagePayload.get("html"));
        String checkoutUrl = stringValue(packagePayload.get("checkoutUrl"));
        Instant publishedAt = publicationExecution.getCompletedAt() != null
                ? publicationExecution.getCompletedAt()
                : Instant.now();
        GeraSalesPagePublicationAudit audit = publicationRepository.save(GeraSalesPagePublicationAudit.builder()
                .experimentId(experiment.getId())
                .publicationJobId(publicationExecution.getIdJob())
                .publishedAt(publishedAt)
                .salesPageUrl(experiment.getFollowUpActionUrl())
                .checkoutUrl(StringUtils.hasText(checkoutUrl) ? checkoutUrl : experiment.getFollowUpActionUrl())
                .html(html)
                .publicationPackageJson(packageJson)
                .createdAt(Instant.now())
                .build());
        publicationStageRepository.saveAll(toStageAudits(audit.getId(), stageExecutions));
    }

    /** Converte execuções concluídas em linhas auditáveis da publicação. */
    private List<GeraSalesPagePublicationStageAudit> toStageAudits(
            Long publicationAuditId,
            List<GeraSalesPageStageExecution> executions) {
        return java.util.stream.IntStream.range(0, executions.size())
                .mapToObj(index -> toStageAudit(publicationAuditId, executions.get(index), index + 1))
                .toList();
    }

    /** Converte uma execução de etapa em snapshot normalizado. */
    private GeraSalesPagePublicationStageAudit toStageAudit(
            Long publicationAuditId,
            GeraSalesPageStageExecution execution,
            int stageOrder) {
        return GeraSalesPagePublicationStageAudit.builder()
                .publicationAuditId(publicationAuditId)
                .stageOrder(stageOrder)
                .idJob(execution.getIdJob())
                .stageCode(execution.getStageCode())
                .status(execution.getStatus())
                .completedAt(execution.getCompletedAt())
                .promptTemplateKey(execution.getPromptTemplateKey())
                .prompt(execution.getPrompt())
                .promptMarkdownContent(execution.getPromptMarkdownContent())
                .schemaJson(execution.getSchemaJson())
                .openAiModel(execution.getOpenAiModel())
                .openAiRequestBody(execution.getOpenAiRequestBody())
                .modelResponse(execution.getModelResponse())
                .rawResponse(execution.getRawResponse())
                .inputTokens(execution.getInputTokens())
                .outputTokens(execution.getOutputTokens())
                .costUsd(execution.getCostUsd())
                .build();
    }

    /** Converte entidade persistida em resposta usada pelo frontend. */
    private GeraSalesPagePublicationResponse toResponse(GeraSalesPagePublicationAudit audit) {
        return new GeraSalesPagePublicationResponse(
                audit.getId(),
                audit.getExperimentId(),
                audit.getPublicationJobId(),
                audit.getPublishedAt(),
                audit.getSalesPageUrl(),
                audit.getCheckoutUrl(),
                audit.getHtml(),
                audit.getPublicationPackageJson(),
                publicationStageRepository.findByPublicationAuditIdOrderByStageOrderAsc(audit.getId()).stream()
                        .map(this::toStageResponse)
                        .toList());
    }

    /** Converte snapshot normalizado de etapa em contrato de leitura. */
    private GeraSalesPagePublicationStageResponse toStageResponse(GeraSalesPagePublicationStageAudit stage) {
        return new GeraSalesPagePublicationStageResponse(
                stage.getIdJob(),
                stage.getStageCode(),
                stage.getStatus(),
                stage.getCompletedAt(),
                stage.getPromptTemplateKey(),
                stage.getPrompt(),
                stage.getPromptMarkdownContent(),
                stage.getSchemaJson(),
                stage.getOpenAiModel(),
                stage.getOpenAiRequestBody(),
                stage.getModelResponse(),
                stage.getRawResponse(),
                stage.getInputTokens(),
                stage.getOutputTokens(),
                stage.getCostUsd());
    }

    /** Indica se a execução é a etapa final concluída. */
    private boolean isCompletedPublication(GeraSalesPageStageExecution execution) {
        return execution != null
                && GeraSalesPageStageCode.PUBLICATION_PACKAGE.code().equals(execution.getStageCode())
                && STATUS_COMPLETED.equals(execution.getStatus());
    }

    /** Converte JSON textual em mapa quando possível. */
    private Map<String, Object> parseObject(String json) {
        if (!StringUtils.hasText(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception ex) {
            log.debug("Pacote de publicacao do GeraSalesPage v1 nao estava em JSON.", ex);
            return Map.of();
        }
    }

    /** Extrai string de campo opcional do pacote final. */
    private String stringValue(Object value) {
        return value instanceof String text ? text : null;
    }
}

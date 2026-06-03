package com.marketinghub.geralanding.qualityreview.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.geralanding.GeraLandingStageExecution;
import com.marketinghub.geralanding.qualityreview.service.detailStageExecution.RecordBackendQualityReviewDetalheDto;
import com.marketinghub.geralanding.qualityreview.service.listStageExecutions.GeraLandingQualityReviewExecutionSummaryResponse;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.geralanding.GeraLandingStageExecutionRepository;
import jakarta.persistence.EntityNotFoundException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Responsável por executar e persistir o Quality Gate comercial da landing gerada. */
@Service
public class BackendQualityReviewService {

    private static final Logger log = LoggerFactory.getLogger(BackendQualityReviewService.class);
    private static final String STAGE_CODE = "landing-page-quality-review";
    private static final String STATUS_COMPLETED = "CONCLUIDO";
    private static final String STATUS_FAILED = "FALHA";
    private static final int APPROVAL_SCORE = 80;

    private final ExperimentRepository experimentRepository;
    private final GeraLandingStageExecutionRepository executionRepository;
    private final ObjectMapper objectMapper;

    /** Inicializa o serviço com repositórios e serializador usados pela revisão de qualidade. */
    public BackendQualityReviewService(
            ExperimentRepository experimentRepository,
            GeraLandingStageExecutionRepository executionRepository,
            ObjectMapper objectMapper) {
        this.experimentRepository = experimentRepository;
        this.executionRepository = executionRepository;
        this.objectMapper = objectMapper;
    }

    /** Cria uma execução síncrona do Quality Gate, avalia a landing atual e persiste o diagnóstico. */
    @Transactional
    public GeraLandingQualityReviewStartResponse start(Long experimentId) {
        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new EntityNotFoundException("Experiment not found: " + experimentId));
        GeraLandingStageExecution execution = createExecution(experiment, "manual/start");
        return completeQualityReview(experiment, execution);
    }

    /** Executa automaticamente o Quality Gate após a montagem do HTML final do GeraLanding. */
    @Transactional
    public String reviewAfterHtmlGeneration(Experiment experiment) {
        GeraLandingStageExecution execution = createExecution(experiment, "auto/html-geralanding");
        return completeQualityReview(experiment, execution).qualityReview();
    }

    /** Lista execuções da etapa de revisão de qualidade para o experimento informado. */
    @Transactional(readOnly = true)
    public List<GeraLandingQualityReviewExecutionSummaryResponse> listExperimentStageExecutions(Long experimentId, boolean includeCompleted) {
        List<GeraLandingStageExecution> executions = includeCompleted
                ? executionRepository.findTop20ByExperimentIdAndStageCodeOrderByExecutionRequestedAtDesc(experimentId, STAGE_CODE)
                : executionRepository.findTop20ByExperimentIdAndStageCodeAndStatusNotOrderByExecutionRequestedAtDesc(
                        experimentId,
                        STAGE_CODE,
                        STATUS_COMPLETED);
        return executions.stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    /** Retorna o detalhe persistido de uma execução específica da revisão de qualidade. */
    @Transactional(readOnly = true)
    public RecordBackendQualityReviewDetalheDto getStageExecutionDetail(Long experimentId, String idJob) {
        GeraLandingStageExecution execution = executionRepository
                .findTopByExperimentIdAndIdJobOrderByExecutionRequestedAtDesc(experimentId, toDatabaseIdJob(idJob))
                .orElseThrow(() -> new EntityNotFoundException("GeraLanding quality review execution not found for idJob: " + idJob));
        return toDetailResponse(execution);
    }

    /** Cria o registro inicial da execução de Quality Gate com status preparado para conclusão síncrona. */
    private GeraLandingStageExecution createExecution(Experiment experiment, String promptTemplateId) {
        Instant now = Instant.now();
        GeraLandingStageExecution execution = GeraLandingStageExecution.builder()
                .experimentId(experiment.getId())
                .experiment(experiment)
                .stageCode(STAGE_CODE)
                .executionRequestedAt(now)
                .createdAt(now)
                .processingStartedAt(now)
                .promptTemplateId(promptTemplateId)
                .promptContent("Quality Gate comercial automático da landing final.")
                .status(STATUS_COMPLETED)
                .idJob(toDatabaseIdJob(UUID.randomUUID().toString()))
                .build();
        return executionRepository.save(execution);
    }

    /** Avalia, serializa e grava o diagnóstico da landing no experimento e na execução. */
    private GeraLandingQualityReviewStartResponse completeQualityReview(Experiment experiment, GeraLandingStageExecution execution) {
        try {
            String qualityReview = buildQualityReviewJson(experiment);
            execution.setModelResponse(qualityReview);
            execution.setProvisionalHtml(qualityReview);
            execution.setCompletedAt(Instant.now());
            execution.setStatus(STATUS_COMPLETED);
            experiment.setLandingPageQualityReview(qualityReview);
            experimentRepository.save(experiment);
            executionRepository.save(execution);
            return new GeraLandingQualityReviewStartResponse(fromDatabaseIdJob(execution.getIdJob()), execution.getStatus(), qualityReview);
        } catch (RuntimeException ex) {
            log.error(
                    "Erro ao executar Quality Gate da landing. experimentId={}, idJob={}",
                    experiment.getId(),
                    fromDatabaseIdJob(execution.getIdJob()),
                    ex);
            execution.setCompletedAt(Instant.now());
            execution.setStatus(STATUS_FAILED);
            execution.setErrorMessage("Falha ao avaliar qualidade comercial da landing");
            execution.setErrorDetail(ex.getMessage());
            executionRepository.save(execution);
            throw ex;
        }
    }

    /** Monta o JSON de saída do Quality Gate com score, bloqueios e etapas recomendadas para regeneração. */
    private String buildQualityReviewJson(Experiment experiment) {
        ReviewAccumulator review = evaluate(experiment);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("score", review.score());
        payload.put("targetAudienceSpecificity", review.targetAudienceSpecificity());
        payload.put("blockingIssues", review.blockingIssues());
        payload.put("recommendedRegeneration", review.recommendedRegeneration());
        payload.put("approvalRecommendation", review.score() >= APPROVAL_SCORE && review.blockingIssues().isEmpty()
                ? "APPROVE_FOR_PUBLICATION"
                : "REGENERATE_BEFORE_PUBLICATION");
        return writeJson(payload);
    }

    /** Calcula penalidades objetivas usando os artefatos canônicos e o HTML final disponível. */
    private ReviewAccumulator evaluate(Experiment experiment) {
        int score = 100;
        List<String> issues = new ArrayList<>();
        List<String> regeneration = new ArrayList<>();
        String html = normalizeText(firstText(experiment.getHtmlGeraLanding(), experiment.getLandingPageHtml()));
        String copy = normalizeText(experiment.getLandingPageCopy());
        String wireframe = normalizeText(experiment.getLandingPageWireframe());
        String imagePlanning = normalizeText(experiment.getLandingPageImagePlanning());
        String designPreset = normalizeText(experiment.getLandingPageDesignPreset());
        String all = String.join(" ", html, copy, wireframe, imagePlanning, designPreset).toLowerCase(Locale.ROOT);

        if (!StringUtils.hasText(html)) {
            score -= 30;
            addIssue(issues, regeneration, "A landing não possui HTML final para revisão publicável", "LANDING_PAGE_HTML");
        }
        if (!containsAny(all, "dor", "problema", "dificuldade", "frustração", "medo", "travado")) {
            score -= 12;
            addIssue(issues, regeneration, "A página não deixa explícita a dor específica que o produto remove", "LANDING_PAGE_COPY");
        }
        if (!containsAny(all, "resultado", "transformação", "benefício", "ganho", "clareza", "facilidade")) {
            score -= 12;
            addIssue(issues, regeneration, "A primeira dobra e a copy não vendem claramente a transformação principal", "LANDING_PAGE_COPY");
        }
        if (!containsAny(all, "mecanismo", "método", "processo", "passo", "roteiro", "diagnóstico", "plano")) {
            score -= 10;
            addIssue(issues, regeneration, "O mecanismo de entrega não está plausível ou não foi explicado em passos concretos", "LANDING_PAGE_WIREFRAME");
        }
        if (!containsAny(all, "preview", "exemplo", "amostra", "antes", "depois", "mockup", "relatório", "checklist")) {
            score -= 14;
            addIssue(issues, regeneration, "A prova visual é genérica ou não demonstra a entrega aplicada", "LANDING_PAGE_IMAGE_PLANNING");
        }
        if (!containsAny(all, "receba", "começar", "quero", "acessar", "preencher", "solicitar", "garantir")) {
            score -= 10;
            addIssue(issues, regeneration, "O CTA não orienta o avanço do usuário com benefício imediato", "LANDING_PAGE_COPY");
        }
        if (!containsAny(html.toLowerCase(Locale.ROOT), "<form", "type=\"email\"", "name=\"email\"", "data-field=\"email\"")) {
            score -= 10;
            addIssue(issues, regeneration, "O formulário não está evidente como ponto principal de conversão", "LANDING_PAGE_DESIGN_PRESET");
        }
        if (containsAny(html.toLowerCase(Locale.ROOT), "<!-- auto:", "debuginfo", "legacypreviewhtml", "rendermode")) {
            score -= 20;
            addIssue(issues, regeneration, "O HTML final contém metadado técnico proibido no artefato publicável", "LANDING_PAGE_HTML");
        }

        return new ReviewAccumulator(Math.max(0, score), resolveSpecificity(all), List.copyOf(issues), List.copyOf(regeneration));
    }

    /** Adiciona problema e etapa recomendada sem duplicar valores na saída final. */
    private void addIssue(List<String> issues, List<String> regeneration, String issue, String stage) {
        if (!issues.contains(issue)) {
            issues.add(issue);
        }
        if (!regeneration.contains(stage)) {
            regeneration.add(stage);
        }
    }

    /** Classifica especificidade de público por sinais textuais de nicho, persona e contexto operacional. */
    private String resolveSpecificity(String text) {
        int signals = 0;
        signals += containsAny(text, "para ", "profissional", "cliente", "negócio", "rotina") ? 1 : 0;
        signals += containsAny(text, "mei", "empresa", "equipe", "paciente", "aluno", "lead") ? 1 : 0;
        signals += containsAny(text, "quando", "sem precisar", "mesmo que", "em minutos", "passo a passo") ? 1 : 0;
        if (signals >= 3) {
            return "high";
        }
        if (signals == 2) {
            return "medium";
        }
        return "low";
    }

    /** Verifica se o texto contém pelo menos uma das opções informadas. */
    private boolean containsAny(String text, String... candidates) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        for (String candidate : candidates) {
            if (text.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    /** Retorna o primeiro texto preenchido entre valores candidatos. */
    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return "";
    }

    /** Normaliza texto nulo para vazio, preservando conteúdo para análise textual. */
    private String normalizeText(String value) {
        return value != null ? value : "";
    }

    /** Serializa a saída estruturada do Quality Gate em JSON. */
    private String writeJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Não foi possível serializar Quality Gate da landing", ex);
        }
    }

    /** Converte o resumo persistido para o DTO público da etapa. */
    private GeraLandingQualityReviewExecutionSummaryResponse toSummaryResponse(GeraLandingStageExecution execution) {
        return new GeraLandingQualityReviewExecutionSummaryResponse(
                fromDatabaseIdJob(execution.getIdJob()),
                execution.getStatus(),
                execution.getExecutionRequestedAt(),
                execution.getCostUsd());
    }

    /** Converte a execução persistida para o detalhe público da etapa. */
    private RecordBackendQualityReviewDetalheDto toDetailResponse(GeraLandingStageExecution execution) {
        return new RecordBackendQualityReviewDetalheDto(
                fromDatabaseIdJob(execution.getIdJob()),
                execution.getExperimentId(),
                execution.getStageCode(),
                execution.getExecutionRequestedAt(),
                execution.getCreatedAt(),
                execution.getProcessingStartedAt(),
                execution.getCompletedAt(),
                execution.getPromptTemplateId(),
                execution.getPromptContent(),
                execution.getPrompt(),
                execution.getOpenAiRequestBody(),
                execution.getOpenAiModel(),
                execution.getSchemaJson(),
                execution.getPromptMarkdownContent(),
                execution.getStatus(),
                execution.getOpenAiJobId(),
                execution.getModelResponse(),
                execution.getProvisionalHtml(),
                execution.getErrorMessage(),
                execution.getErrorDetail(),
                execution.getInputTokens(),
                execution.getOutputTokens(),
                execution.getCostUsd());
    }

    /** Converte o id_job textual para o formato persistido em banco. */
    private byte[] toDatabaseIdJob(String idJob) {
        return idJob.getBytes(StandardCharsets.UTF_8);
    }

    /** Converte o id_job persistido em banco para texto. */
    private String fromDatabaseIdJob(byte[] idJob) {
        return new String(idJob, StandardCharsets.UTF_8);
    }

    /** Representa o resultado intermediário calculado antes da serialização JSON. */
    private record ReviewAccumulator(
            int score,
            String targetAudienceSpecificity,
            List<String> blockingIssues,
            List<String> recommendedRegeneration
    ) {
        /** Mantém o contrato interno imutável do acumulador de revisão. */
        private ReviewAccumulator {}
    }
}

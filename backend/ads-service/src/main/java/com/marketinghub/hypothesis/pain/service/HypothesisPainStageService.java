package com.marketinghub.hypothesis.pain.service;

import com.marketinghub.hypothesis.pain.HypothesisPainStageExecution;
import com.marketinghub.hypothesis.pain.service.detailStageExecution.HypothesisPainExecutionDetailResponse;
import com.marketinghub.hypothesis.pain.service.listStageExecutions.HypothesisPainExecutionSummaryResponse;
import com.marketinghub.hypothesis.pain.service.pending.HypothesisPainPendingExecution;
import com.marketinghub.hypothesis.pain.service.pending.HypothesisPainPendingNiche;
import com.marketinghub.hypothesis.pain.service.recebeResposta.RecebeRespostaRequest;
import com.marketinghub.hypothesis.pain.service.start.HypothesisPainStartResponse;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.repository.jpa.hypothesis.HypothesisPainStageExecutionRepository;
import com.marketinghub.repository.jpa.niche.MarketNicheRepository;
import jakarta.persistence.EntityNotFoundException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Responsabilidade: orquestrar execuções auditáveis da etapa Dor do pipeline de hipótese. */
@Service
public class HypothesisPainStageService {
    private static final Logger log = LoggerFactory.getLogger(HypothesisPainStageService.class);
    private static final String STAGE_CODE = "hypothesis-pain";
    private static final String STATUS_STARTED = "INICIADO";
    private static final String STATUS_WAITING_OPENAI_DISPATCH = "AGUARDANDO_RETORNO_OPENAI";
    private static final String STATUS_PROCESSING = "PROCESSANDO";
    private static final String STATUS_COMPLETED = "CONCLUIDO";
    private static final String STATUS_FAILED = "FALHA";

    private final MarketNicheRepository marketNicheRepository;
    private final HypothesisPainStageExecutionRepository executionRepository;

    /** Inicializa o serviço com os repositórios canônicos de nicho e execução de etapa. */
    public HypothesisPainStageService(
            MarketNicheRepository marketNicheRepository,
            HypothesisPainStageExecutionRepository executionRepository) {
        this.marketNicheRepository = marketNicheRepository;
        this.executionRepository = executionRepository;
    }

    /** Inicia uma nova execução manual da etapa Dor para o nicho informado. */
    @Transactional
    public HypothesisPainStartResponse start(Long marketNicheId) {
        Instant now = Instant.now();
        MarketNiche niche = marketNicheRepository.findById(marketNicheId)
                .orElseThrow(() -> new EntityNotFoundException("Market niche not found: " + marketNicheId));
        HypothesisPainStageExecution execution = HypothesisPainStageExecution.builder()
                .marketNicheId(niche.getId())
                .marketNiche(niche)
                .stageCode(STAGE_CODE)
                .executionRequestedAt(now)
                .createdAt(now)
                .promptTemplateId("manual/start")
                .promptContent("Início manual da etapa Dor via tela de nova hipótese.")
                .status(STATUS_STARTED)
                .idJob(toDatabaseIdJob(UUID.randomUUID().toString()))
                .build();
        HypothesisPainStageExecution saved = executionRepository.save(execution);
        return new HypothesisPainStartResponse(fromDatabaseIdJob(saved.getIdJob()), saved.getStatus());
    }

    /** Lista execuções da etapa Dor para o nicho informado. */
    @Transactional(readOnly = true)
    public List<HypothesisPainExecutionSummaryResponse> listStageExecutions(Long marketNicheId, boolean includeCompleted) {
        List<HypothesisPainStageExecution> executions = includeCompleted
                ? executionRepository.findTop20ByMarketNicheIdAndStageCodeOrderByExecutionRequestedAtDesc(marketNicheId, STAGE_CODE)
                : executionRepository.findTop20ByMarketNicheIdAndStageCodeAndStatusNotOrderByExecutionRequestedAtDesc(
                        marketNicheId,
                        STAGE_CODE,
                        STATUS_COMPLETED);
        return executions.stream().map(this::toSummaryResponse).toList();
    }

    /** Retorna o detalhe completo de auditoria de uma execução pelo jobid. */
    @Transactional(readOnly = true)
    public HypothesisPainExecutionDetailResponse detail(String idJob) {
        return executionRepository.findTopByIdJobOrderByExecutionRequestedAtDesc(toDatabaseIdJob(idJob))
                .map(this::toDetailResponse)
                .orElseThrow(() -> new EntityNotFoundException("Hypothesis pain execution not found for idJob: " + idJob));
    }

    /** Lista os jobs iniciados da etapa Dor para processamento pelo Worker AI. */
    @Transactional(readOnly = true)
    public List<HypothesisPainPendingExecution> listPending() {
        return executionRepository.findTop20ByStageCodeAndStatusOrderByExecutionRequestedAtAsc(STAGE_CODE, STATUS_STARTED)
                .stream()
                .map(execution -> new HypothesisPainPendingExecution(
                        execution.getMarketNicheId(),
                        fromDatabaseIdJob(execution.getIdJob()),
                        execution.getStageCode(),
                        execution.getExecutionRequestedAt(),
                        toPendingNiche(execution.getMarketNiche())))
                .toList();
    }

    /** Marca uma execução como em processamento para evitar recaptura por outro ciclo do worker. */
    @Transactional
    public void markRunning(String idJob) {
        HypothesisPainStageExecution execution = findExecution(idJob);
        execution.setProcessingStartedAt(Instant.now());
        execution.setStatus(STATUS_PROCESSING);
        executionRepository.save(execution);
    }

    /** Recebe prompt, schema e request cru despachados para a OpenAI. */
    @Transactional
    public void markWaitingOpenAiDispatch(
            String idJob,
            String prompt,
            String promptMarkdownContent,
            String schemaJson,
            String requestBodyJson,
            String openAiModel,
            String openAiJobId) {
        HypothesisPainStageExecution execution = findExecution(idJob);
        execution.setPrompt(prompt);
        execution.setPromptMarkdownContent(resolvePromptMarkdownContent(prompt, promptMarkdownContent));
        execution.setSchemaJson(schemaJson);
        execution.setOpenAiRequestBody(requestBodyJson);
        execution.setOpenAiModel(openAiModel);
        execution.setOpenAiJobId(openAiJobId);
        execution.setProcessingStartedAt(Instant.now());
        execution.setStatus(STATUS_WAITING_OPENAI_DISPATCH);
        executionRepository.save(execution);
    }

    /** Conclui ou falha a execução da etapa Dor com a resposta devolvida pelo Worker AI. */
    @Transactional
    public void markCompletedFromResponse(String idJob, RecebeRespostaRequest request) {
        HypothesisPainStageExecution execution = executionRepository.findTopByIdJobOrderByExecutionRequestedAtDesc(toDatabaseIdJob(idJob))
                .or(() -> executionRepository.findTopByMarketNicheIdAndStageCodeOrderByExecutionRequestedAtDesc(
                        request.marketNicheId(),
                        request.stageCode()))
                .orElseThrow(() -> new EntityNotFoundException("Hypothesis pain execution not found for idJob: " + idJob));
        try {
            execution.setModelResponse(request.modelResponse());
            if (StringUtils.hasText(request.openAiJobId())) {
                execution.setOpenAiJobId(request.openAiJobId());
            }
            execution.setInputTokens(request.inputTokens());
            execution.setOutputTokens(request.outputTokens());
            execution.setCostUsd(request.costUsd());
            String normalizedErrorDetail = StringUtils.hasText(request.errorDetail()) ? request.errorDetail().trim() : null;
            String normalizedErrorMessage = normalizeErrorMessage(request.errorMessage(), normalizedErrorDetail);
            execution.setErrorMessage(normalizedErrorMessage);
            execution.setErrorDetail(normalizedErrorDetail);
            execution.setCompletedAt(Instant.now());
            execution.setStatus(normalizedErrorMessage != null ? STATUS_FAILED : STATUS_COMPLETED);
            executionRepository.save(execution);
        } catch (RuntimeException ex) {
            log.error(
                    "Erro ao concluir resposta da etapa Dor (idJob={}, marketNicheId={}, stageCode={}, openAiJobId={}, modelResponseLength={}, errorMessage={})",
                    idJob,
                    request.marketNicheId(),
                    request.stageCode(),
                    request.openAiJobId(),
                    request.modelResponse() != null ? request.modelResponse().length() : 0,
                    request.errorMessage(),
                    ex);
            throw ex;
        }
    }

    /** Normaliza a mensagem de erro e garante status de falha quando há detalhe técnico. */
    private String normalizeErrorMessage(String errorMessage, String normalizedErrorDetail) {
        if (StringUtils.hasText(errorMessage)) {
            return errorMessage.trim();
        }
        if (StringUtils.hasText(normalizedErrorDetail)) {
            return "Falha ao processar etapa Dor";
        }
        return null;
    }

    /** Resolve o markdown bruto do prompt para auditoria. */
    private String resolvePromptMarkdownContent(String prompt, String promptMarkdownContent) {
        if (StringUtils.hasText(promptMarkdownContent)) {
            return promptMarkdownContent;
        }
        return prompt;
    }

    /** Busca uma execução pelo identificador técnico do job. */
    private HypothesisPainStageExecution findExecution(String idJob) {
        return executionRepository.findTopByIdJobOrderByExecutionRequestedAtDesc(toDatabaseIdJob(idJob))
                .orElseThrow(() -> new EntityNotFoundException("Hypothesis pain execution not found for idJob: " + idJob));
    }

    /** Converte a entidade de nicho para o contrato pendente consumido pelo Worker AI. */
    private HypothesisPainPendingNiche toPendingNiche(MarketNiche niche) {
        if (niche == null) {
            return null;
        }
        return new HypothesisPainPendingNiche(
                niche.getId(),
                niche.getName(),
                niche.getDescription(),
                niche.getDemandVolume(),
                niche.getPromises(),
                niche.getOffers(),
                niche.getBaseSegmentation(),
                niche.getInterests(),
                niche.getDemographicFilters(),
                niche.getExtraTips());
    }

    /** Converte uma execução para resumo operacional. */
    private HypothesisPainExecutionSummaryResponse toSummaryResponse(HypothesisPainStageExecution execution) {
        return new HypothesisPainExecutionSummaryResponse(
                fromDatabaseIdJob(execution.getIdJob()),
                execution.getMarketNicheId(),
                execution.getHypothesisId() != null ? execution.getHypothesisId().toString() : null,
                execution.getStageCode(),
                execution.getStatus(),
                execution.getExecutionRequestedAt(),
                execution.getProcessingStartedAt(),
                execution.getCompletedAt(),
                execution.getOpenAiModel(),
                execution.getOpenAiJobId(),
                execution.getInputTokens(),
                execution.getOutputTokens(),
                execution.getCostUsd(),
                execution.getErrorMessage(),
                execution.getModelResponse());
    }

    /** Converte uma execução para detalhe completo de auditoria. */
    private HypothesisPainExecutionDetailResponse toDetailResponse(HypothesisPainStageExecution execution) {
        return new HypothesisPainExecutionDetailResponse(
                fromDatabaseIdJob(execution.getIdJob()),
                execution.getMarketNicheId(),
                execution.getHypothesisId() != null ? execution.getHypothesisId().toString() : null,
                execution.getStageCode(),
                execution.getStatus(),
                execution.getExecutionRequestedAt(),
                execution.getProcessingStartedAt(),
                execution.getCompletedAt(),
                execution.getPromptTemplateId(),
                execution.getPromptContent(),
                execution.getPrompt(),
                execution.getPromptMarkdownContent(),
                execution.getSchemaJson(),
                execution.getOpenAiRequestBody(),
                execution.getOpenAiModel(),
                execution.getOpenAiJobId(),
                execution.getModelResponse(),
                execution.getErrorMessage(),
                execution.getErrorDetail(),
                execution.getInputTokens(),
                execution.getOutputTokens(),
                execution.getCostUsd());
    }

    /** Converte jobid textual para bytes persistidos no mesmo padrão do GeraLanding. */
    private byte[] toDatabaseIdJob(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    /** Converte jobid persistido para texto no mesmo padrão do GeraLanding. */
    private String fromDatabaseIdJob(byte[] value) {
        return new String(value, StandardCharsets.UTF_8);
    }
}

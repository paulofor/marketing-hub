package com.marketinghub.hypothesis.pain.service;

import com.marketinghub.hypothesis.pain.HypothesisPainCostCalculator;
import com.marketinghub.hypothesis.pain.HypothesisPainStageExecution;
import com.marketinghub.hypothesis.pain.service.detailStageExecution.HypothesisPainExecutionDetailResponse;
import com.marketinghub.hypothesis.pain.service.listStageExecutions.HypothesisPainExecutionSummaryResponse;
import com.marketinghub.hypothesis.pain.service.pending.HypothesisPainPendingExecution;
import com.marketinghub.hypothesis.pain.service.pending.HypothesisPainPendingNiche;
import com.marketinghub.hypothesis.pain.service.recebeResposta.RecebeRespostaRequest;
import com.marketinghub.hypothesis.pain.service.start.HypothesisPainStartResponse;
import com.marketinghub.hypothesis.pain.service.summary.HypothesisStageFinalSummaryResponse;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.repository.jpa.hypothesis.HypothesisPainStageExecutionRepository;
import com.marketinghub.repository.jpa.niche.MarketNicheRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Responsabilidade: orquestrar execuções auditáveis das etapas do pipeline de hipótese por nicho. */
@Service
public class HypothesisPainStageService {
    private static final Logger log = LoggerFactory.getLogger(HypothesisPainStageService.class);
    private static final String STAGE_CODE = "hypothesis-pain";
    private static final String RESULT_STAGE_CODE = "hypothesis-result";
    private static final String MECHANISM_STAGE_CODE = "hypothesis-mechanism";
    private static final String PROOF_STAGE_CODE = "hypothesis-proof";
    private static final String OFFER_STAGE_CODE = "hypothesis-offer";
    private static final String STATUS_STARTED = "INICIADO";
    private static final String STATUS_WAITING_OPENAI_DISPATCH = "AGUARDANDO_RETORNO_OPENAI";
    private static final String STATUS_PROCESSING = "PROCESSANDO";
    private static final String STATUS_COMPLETED = "CONCLUIDO";
    private static final String STATUS_FAILED = "FALHA";
    private static final Duration OPERATIONAL_LEASE_TIMEOUT = Duration.ofMinutes(45);
    private static final List<String> LEASE_GUARDED_STATUSES = List.of(
            STATUS_PROCESSING,
            STATUS_WAITING_OPENAI_DISPATCH);

    private final MarketNicheRepository marketNicheRepository;
    private final HypothesisPainStageExecutionRepository executionRepository;
    private final HypothesisPainCostCalculator costCalculator;

    /** Inicializa o serviço com os repositórios canônicos e o calculador interno de custo da etapa. */
    public HypothesisPainStageService(
            MarketNicheRepository marketNicheRepository,
            HypothesisPainStageExecutionRepository executionRepository,
            HypothesisPainCostCalculator costCalculator) {
        this.marketNicheRepository = marketNicheRepository;
        this.executionRepository = executionRepository;
        this.costCalculator = costCalculator;
    }

    /** Inicia uma nova execução manual da etapa Dor para o nicho informado. */
    @Transactional
    public HypothesisPainStartResponse start(Long marketNicheId) {
        return startStage(marketNicheId, STAGE_CODE, "Dor", false, false, false, false);
    }

    /** Inicia uma nova execução manual da etapa Resultado para o nicho informado. */
    @Transactional
    public HypothesisPainStartResponse startResult(Long marketNicheId) {
        return startStage(marketNicheId, RESULT_STAGE_CODE, "Resultado", true, false, false, false);
    }

    /** Inicia uma nova execução manual da etapa Mecanismo para o nicho informado. */
    @Transactional
    public HypothesisPainStartResponse startMechanism(Long marketNicheId) {
        return startStage(marketNicheId, MECHANISM_STAGE_CODE, "Mecanismo", true, true, false, false);
    }

    /** Inicia uma nova execução manual da etapa Prova para o nicho informado. */
    @Transactional
    public HypothesisPainStartResponse startProof(Long marketNicheId) {
        return startStage(marketNicheId, PROOF_STAGE_CODE, "Prova", true, true, true, false);
    }

    /** Inicia uma nova execução manual da etapa Oferta para o nicho informado. */
    @Transactional
    public HypothesisPainStartResponse startOffer(Long marketNicheId) {
        return startStage(marketNicheId, OFFER_STAGE_CODE, "Oferta", true, true, true, true);
    }

    /** Inicia uma nova execução manual de uma etapa do pipeline para o nicho informado. */
    private HypothesisPainStartResponse startStage(
            Long marketNicheId,
            String stageCode,
            String stageLabel,
            boolean requiresCompletedPain,
            boolean requiresCompletedResult,
            boolean requiresCompletedMechanism,
            boolean requiresCompletedProof) {
        Instant now = Instant.now();
        MarketNiche niche = marketNicheRepository.findById(marketNicheId)
                .orElseThrow(() -> new EntityNotFoundException("Market niche not found: " + marketNicheId));
        if (requiresCompletedPain) {
            requireCompletedPain(marketNicheId);
        }
        if (requiresCompletedResult) {
            requireCompletedResult(marketNicheId);
        }
        if (requiresCompletedMechanism) {
            requireCompletedMechanism(marketNicheId);
        }
        if (requiresCompletedProof) {
            requireCompletedProof(marketNicheId);
        }
        HypothesisPainStageExecution execution = HypothesisPainStageExecution.builder()
                .marketNicheId(niche.getId())
                .marketNiche(niche)
                .stageCode(stageCode)
                .executionRequestedAt(now)
                .createdAt(now)
                .promptTemplateId("manual/start")
                .promptContent("Início manual da etapa " + stageLabel + " via tela de nova hipótese.")
                .status(STATUS_STARTED)
                .idJob(toDatabaseIdJob(UUID.randomUUID().toString()))
                .build();
        HypothesisPainStageExecution saved = executionRepository.save(execution);
        return new HypothesisPainStartResponse(fromDatabaseIdJob(saved.getIdJob()), saved.getStatus());
    }

    /** Lista execuções da etapa Dor para o nicho informado. */
    @Transactional(readOnly = true)
    public List<HypothesisPainExecutionSummaryResponse> listStageExecutions(Long marketNicheId, boolean includeCompleted) {
        return listStageExecutions(marketNicheId, STAGE_CODE, includeCompleted);
    }

    /** Lista execuções da etapa Resultado para o nicho informado. */
    @Transactional(readOnly = true)
    public List<HypothesisPainExecutionSummaryResponse> listResultStageExecutions(Long marketNicheId, boolean includeCompleted) {
        return listStageExecutions(marketNicheId, RESULT_STAGE_CODE, includeCompleted);
    }

    /** Lista execuções da etapa Mecanismo para o nicho informado. */
    @Transactional(readOnly = true)
    public List<HypothesisPainExecutionSummaryResponse> listMechanismStageExecutions(
            Long marketNicheId,
            boolean includeCompleted) {
        return listStageExecutions(marketNicheId, MECHANISM_STAGE_CODE, includeCompleted);
    }

    /** Lista execuções da etapa Prova para o nicho informado. */
    @Transactional(readOnly = true)
    public List<HypothesisPainExecutionSummaryResponse> listProofStageExecutions(
            Long marketNicheId,
            boolean includeCompleted) {
        return listStageExecutions(marketNicheId, PROOF_STAGE_CODE, includeCompleted);
    }

    /** Lista execuções da etapa Oferta para o nicho informado. */
    @Transactional(readOnly = true)
    public List<HypothesisPainExecutionSummaryResponse> listOfferStageExecutions(
            Long marketNicheId,
            boolean includeCompleted) {
        return listStageExecutions(marketNicheId, OFFER_STAGE_CODE, includeCompleted);
    }

    /** Lista execuções de uma etapa específica para o nicho informado. */
    private List<HypothesisPainExecutionSummaryResponse> listStageExecutions(Long marketNicheId, String stageCode, boolean includeCompleted) {
        List<HypothesisPainStageExecution> executions = includeCompleted
                ? executionRepository.findByMarketNicheIdAndStageCodeOrderByExecutionRequestedAtDesc(marketNicheId, stageCode)
                : executionRepository.findTop20ByMarketNicheIdAndStageCodeAndStatusNotOrderByExecutionRequestedAtDesc(
                        marketNicheId,
                        stageCode,
                        STATUS_COMPLETED);
        return executions.stream().map(this::toSummaryResponse).toList();
    }

    /** Retorna o detalhe completo de auditoria de uma execução pelo jobid dentro do nicho informado. */
    @Transactional(readOnly = true)
    public HypothesisPainExecutionDetailResponse detailForNiche(Long marketNicheId, String idJob) {
        HypothesisPainStageExecution execution = findExecution(idJob);
        if (!marketNicheId.equals(execution.getMarketNicheId())) {
            throw new EntityNotFoundException(
                    "Hypothesis pain execution not found for marketNicheId: " + marketNicheId + " and idJob: " + idJob);
        }
        return toDetailResponse(execution);
    }

    /** Retorna o detalhe completo de auditoria de uma execução pelo jobid. */
    @Transactional(readOnly = true)
    public HypothesisPainExecutionDetailResponse detail(String idJob) {
        return executionRepository.findTopByIdJobOrderByExecutionRequestedAtDesc(toDatabaseIdJob(idJob))
                .map(this::toDetailResponse)
                .orElseThrow(() -> new EntityNotFoundException("Hypothesis pain execution not found for idJob: " + idJob));
    }

    /** Lista o conteúdo final concluído de cada etapa do framework para uso como insumo em próximos pipelines. */
    @Transactional(readOnly = true)
    public List<HypothesisStageFinalSummaryResponse> listFinalSummary(Long marketNicheId) {
        return List.of(
                toFinalSummary(marketNicheId, "pain", 1, "Dor do nicho", STAGE_CODE),
                toFinalSummary(marketNicheId, "result", 2, "Resultado desejado", RESULT_STAGE_CODE),
                toFinalSummary(marketNicheId, "mechanism", 3, "Mecanismo", MECHANISM_STAGE_CODE),
                toFinalSummary(marketNicheId, "proof", 4, "Prova", PROOF_STAGE_CODE),
                toFinalSummary(marketNicheId, "offer", 5, "Oferta", OFFER_STAGE_CODE));
    }

    /** Monta o resumo de uma etapa com a origem exata do conteúdo final no banco de dados. */
    private HypothesisStageFinalSummaryResponse toFinalSummary(
            Long marketNicheId,
            String slug,
            int stageNumber,
            String stageTitle,
            String stageCode) {
        return executionRepository.findTopByMarketNicheIdAndStageCodeAndStatusOrderByExecutionRequestedAtDesc(
                        marketNicheId,
                        stageCode,
                        STATUS_COMPLETED)
                .map(execution -> new HypothesisStageFinalSummaryResponse(
                        slug,
                        stageNumber,
                        stageTitle,
                        stageCode,
                        fromDatabaseIdJob(execution.getIdJob()),
                        execution.getStatus(),
                        execution.getCompletedAt(),
                        execution.getModelResponse(),
                        "hypothesis_pain_stage_execution",
                        "model_response"))
                .orElseGet(() -> new HypothesisStageFinalSummaryResponse(
                        slug,
                        stageNumber,
                        stageTitle,
                        stageCode,
                        null,
                        null,
                        null,
                        null,
                        "hypothesis_pain_stage_execution",
                        "model_response"));
    }

    /** Lista os jobs iniciados da etapa Dor para processamento pelo Worker AI. */
    @Transactional
    public List<HypothesisPainPendingExecution> listPending() {
        return listPendingByStage(STAGE_CODE);
    }

    /** Lista os jobs iniciados da etapa Resultado para processamento pelo Worker AI. */
    @Transactional
    public List<HypothesisPainPendingExecution> listResultPending() {
        return listPendingByStage(RESULT_STAGE_CODE);
    }

    /** Lista os jobs iniciados da etapa Mecanismo para processamento pelo Worker AI. */
    @Transactional
    public List<HypothesisPainPendingExecution> listMechanismPending() {
        return listPendingByStage(MECHANISM_STAGE_CODE);
    }

    /** Lista os jobs iniciados da etapa Prova para processamento pelo Worker AI. */
    @Transactional
    public List<HypothesisPainPendingExecution> listProofPending() {
        return listPendingByStage(PROOF_STAGE_CODE);
    }

    /** Lista os jobs iniciados da etapa Oferta para processamento pelo Worker AI. */
    @Transactional
    public List<HypothesisPainPendingExecution> listOfferPending() {
        return listPendingByStage(OFFER_STAGE_CODE);
    }

    /** Lista os jobs iniciados de uma etapa para processamento pelo Worker AI após aplicar recuperação de lease vencido. */
    private List<HypothesisPainPendingExecution> listPendingByStage(String stageCode) {
        recoverExpiredOperationalLeases(stageCode);
        return executionRepository.findTop20ByStageCodeAndStatusOrderByExecutionRequestedAtAsc(stageCode, STATUS_STARTED)
                .stream()
                .map(execution -> new HypothesisPainPendingExecution(
                        execution.getMarketNicheId(),
                        fromDatabaseIdJob(execution.getIdJob()),
                        execution.getStageCode(),
                        execution.getStatus(),
                        execution.getExecutionRequestedAt(),
                        execution.getProcessingStartedAt(),
                        toPendingNiche(execution.getMarketNiche()),
                        pendingPainResponse(execution),
                        pendingResultResponse(execution),
                        pendingMechanismResponse(execution),
                        pendingProofResponse(execution)))
                .toList();
    }

    /** Recupera leases antigos com segurança, sem recapturar execuções que já possuem job ativo na OpenAI. */
    private void recoverExpiredOperationalLeases(String stageCode) {
        Instant now = Instant.now();
        Instant threshold = now.minus(OPERATIONAL_LEASE_TIMEOUT);
        List<HypothesisPainStageExecution> expiredExecutions = executionRepository
                .findTop50ByStageCodeAndStatusInAndCompletedAtIsNullAndProcessingStartedAtBeforeOrderByProcessingStartedAtAsc(
                        stageCode,
                        LEASE_GUARDED_STATUSES,
                        threshold);
        expiredExecutions.forEach(execution -> applyExpiredLeaseDecision(execution, now));
        if (!expiredExecutions.isEmpty()) {
            executionRepository.saveAll(expiredExecutions);
        }
    }

    /** Decide se o lease vencido pode voltar para a fila ou se deve falhar para evitar duplicidade na OpenAI. */
    private void applyExpiredLeaseDecision(HypothesisPainStageExecution execution, Instant now) {
        String previousStatus = execution.getStatus();
        String timeoutMessage = "Timeout operacional: execução ficou em " + previousStatus
                + " por mais de " + OPERATIONAL_LEASE_TIMEOUT.toMinutes()
                + " minutos sem conclusão. ";
        if (STATUS_PROCESSING.equals(previousStatus) && !StringUtils.hasText(execution.getOpenAiJobId())) {
            execution.setStatus(STATUS_STARTED);
            execution.setProcessingStartedAt(null);
            execution.setErrorMessage(
                    timeoutMessage + "Job recuperado automaticamente e devolvido para a fila de processamento.");
            execution.setErrorDetail(
                    "Recuperação automática por lease vencido em " + now + ". Nenhum openai_job_id estava associado ao job.");
            return;
        }
        execution.setStatus(STATUS_FAILED);
        execution.setCompletedAt(now);
        execution.setErrorMessage(
                timeoutMessage + "Job marcado como FALHA para evitar recaptura enquanto há possível execução ativa na OpenAI.");
        execution.setErrorDetail(
                "Bloqueio automático por lease vencido em " + now + ". openai_job_id=" + execution.getOpenAiJobId());
    }

    /** Retorna a Dor concluída quando a etapa pendente precisa desse contexto. */
    private String pendingPainResponse(HypothesisPainStageExecution execution) {
        return RESULT_STAGE_CODE.equals(execution.getStageCode())
                || MECHANISM_STAGE_CODE.equals(execution.getStageCode())
                || PROOF_STAGE_CODE.equals(execution.getStageCode())
                || OFFER_STAGE_CODE.equals(execution.getStageCode())
                ? latestCompletedPainResponse(execution.getMarketNicheId())
                : null;
    }

    /** Retorna o Resultado concluído quando a etapa pendente precisa desse contexto. */
    private String pendingResultResponse(HypothesisPainStageExecution execution) {
        return MECHANISM_STAGE_CODE.equals(execution.getStageCode())
                        || PROOF_STAGE_CODE.equals(execution.getStageCode())
                        || OFFER_STAGE_CODE.equals(execution.getStageCode())
                ? latestCompletedResultResponse(execution.getMarketNicheId())
                : null;
    }

    /** Retorna o Mecanismo concluído quando a etapa pendente precisa desse contexto. */
    private String pendingMechanismResponse(HypothesisPainStageExecution execution) {
        return PROOF_STAGE_CODE.equals(execution.getStageCode())
                || OFFER_STAGE_CODE.equals(execution.getStageCode())
                ? latestCompletedMechanismResponse(execution.getMarketNicheId())
                : null;
    }

    /** Retorna a Prova concluída quando a etapa pendente precisa desse contexto. */
    private String pendingProofResponse(HypothesisPainStageExecution execution) {
        return OFFER_STAGE_CODE.equals(execution.getStageCode())
                ? latestCompletedProofResponse(execution.getMarketNicheId())
                : null;
    }

    /** Garante que a etapa Dor tenha sido concluída com resposta antes de liberar Resultado. */
    private void requireCompletedPain(Long marketNicheId) {
        if (!StringUtils.hasText(latestCompletedPainResponse(marketNicheId))) {
            throw new IllegalStateException(
                    "A etapa Dor precisa estar concluída antes de iniciar Resultado para o nicho: " + marketNicheId);
        }
    }

    /** Garante que a etapa Resultado tenha sido concluída com resposta antes de liberar Mecanismo. */
    private void requireCompletedResult(Long marketNicheId) {
        if (!StringUtils.hasText(latestCompletedResultResponse(marketNicheId))) {
            throw new IllegalStateException(
                    "A etapa Resultado precisa estar concluída antes de iniciar Mecanismo para o nicho: " + marketNicheId);
        }
    }

    /** Garante que a etapa Mecanismo tenha sido concluída com resposta antes de liberar Oferta. */
    private void requireCompletedMechanism(Long marketNicheId) {
        if (!StringUtils.hasText(latestCompletedMechanismResponse(marketNicheId))) {
            throw new IllegalStateException(
                    "A etapa Mecanismo precisa estar concluída antes de iniciar Oferta para o nicho: " + marketNicheId);
        }
    }

    /** Garante que a etapa Prova tenha sido concluída com resposta antes de liberar Oferta. */
    private void requireCompletedProof(Long marketNicheId) {
        if (!StringUtils.hasText(latestCompletedProofResponse(marketNicheId))) {
            throw new IllegalStateException(
                    "A etapa Prova precisa estar concluída antes de iniciar Oferta para o nicho: " + marketNicheId);
        }
    }

    /** Retorna a resposta da Dor concluída mais recente para contextualizar etapas seguintes. */
    private String latestCompletedPainResponse(Long marketNicheId) {
        return latestCompletedStageResponse(marketNicheId, STAGE_CODE);
    }

    /** Retorna a resposta do Resultado concluído mais recente para contextualizar Mecanismo. */
    private String latestCompletedResultResponse(Long marketNicheId) {
        return latestCompletedStageResponse(marketNicheId, RESULT_STAGE_CODE);
    }

    /** Retorna a resposta do Mecanismo concluído mais recente para contextualizar Oferta. */
    private String latestCompletedMechanismResponse(Long marketNicheId) {
        return latestCompletedStageResponse(marketNicheId, MECHANISM_STAGE_CODE);
    }

    /** Retorna a resposta da Prova concluída mais recente para contextualizar Oferta. */
    private String latestCompletedProofResponse(Long marketNicheId) {
        return latestCompletedStageResponse(marketNicheId, PROOF_STAGE_CODE);
    }

    /** Retorna a resposta concluída mais recente de uma etapa para contextualizar próximas etapas. */
    private String latestCompletedStageResponse(Long marketNicheId, String stageCode) {
        return executionRepository.findTopByMarketNicheIdAndStageCodeAndStatusOrderByExecutionRequestedAtDesc(
                        marketNicheId,
                        stageCode,
                        STATUS_COMPLETED)
                .map(HypothesisPainStageExecution::getModelResponse)
                .filter(StringUtils::hasText)
                .orElse(null);
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
            BigDecimal previousCostUsd = execution.getCostUsd();
            String normalizedErrorDetail = StringUtils.hasText(request.errorDetail()) ? request.errorDetail().trim() : null;
            String normalizedErrorMessage = normalizeErrorMessage(request.errorMessage(), normalizedErrorDetail);
            BigDecimal calculatedCostUsd = calculateInternalCostUsd(execution, request, normalizedErrorMessage);
            BigDecimal costDeltaUsd = calculateCostDeltaUsd(previousCostUsd, calculatedCostUsd);
            execution.setCostUsd(calculatedCostUsd);
            execution.setErrorMessage(normalizedErrorMessage);
            execution.setErrorDetail(normalizedErrorDetail);
            execution.setCompletedAt(Instant.now());
            execution.setStatus(normalizedErrorMessage != null ? STATUS_FAILED : STATUS_COMPLETED);
            executionRepository.save(execution);
            costCalculator.addFlexCostDeltaToNiche(execution.getMarketNiche(), costDeltaUsd);
        } catch (RuntimeException ex) {
            log.error(
                    "Erro ao concluir resposta da etapa do pipeline de hipótese (idJob={}, marketNicheId={}, stageCode={}, openAiJobId={}, modelResponseLength={}, errorMessage={})",
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

    /** Calcula internamente o custo flex da resposta usando o modelo salvo e os tokens recebidos. */
    private BigDecimal calculateInternalCostUsd(
            HypothesisPainStageExecution execution, RecebeRespostaRequest request, String normalizedErrorMessage) {
        boolean tokensAusentes = request.inputTokens() == null && request.outputTokens() == null;
        if (normalizedErrorMessage != null && tokensAusentes) {
            return null;
        }
        if (tokensAusentes) {
            throw new IllegalStateException("Tokens ausentes para cálculo de custo da etapa Dor");
        }
        return costCalculator.calculateFlexCostUsd(
                execution.getOpenAiModel(),
                request.inputTokens(),
                request.outputTokens());
    }

    /** Calcula a diferença de custo em USD para manter o custo do nicho idempotente. */
    private BigDecimal calculateCostDeltaUsd(BigDecimal previousCostUsd, BigDecimal currentCostUsd) {
        BigDecimal previous = previousCostUsd != null ? previousCostUsd : BigDecimal.ZERO;
        BigDecimal current = currentCostUsd != null ? currentCostUsd : BigDecimal.ZERO;
        return current.subtract(previous);
    }

    /** Normaliza a mensagem de erro e garante status de falha quando há detalhe técnico. */
    private String normalizeErrorMessage(String errorMessage, String normalizedErrorDetail) {
        if (StringUtils.hasText(errorMessage)) {
            return errorMessage.trim();
        }
        if (StringUtils.hasText(normalizedErrorDetail)) {
            return "Falha ao processar etapa do pipeline de hipótese";
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

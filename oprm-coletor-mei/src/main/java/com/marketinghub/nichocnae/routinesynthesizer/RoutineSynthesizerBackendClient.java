package com.marketinghub.nichocnae.routinesynthesizer;

import com.marketinghub.oprmcoletormei.marketimport.config.OprmMarketImportCollectorProperties;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Chama exclusivamente endpoints OPRM nichocnae do backend para executar a etapa seis pelo coletor. */
@Component
public class RoutineSynthesizerBackendClient {
    private static final Logger log = LoggerFactory.getLogger(RoutineSynthesizerBackendClient.class);
    private static final String PENDING_PATH = "/api/internal/oprm/nichocnae/routine-synthesizer/stage-executions/pending";
    private static final String COMPLETE_PATH_PREFIX = "/api/internal/oprm/nichocnae/routine-synthesizer/stage-executions/";
    private static final String COMPLETE_PATH_SUFFIX = "/complete";
    private static final String FAIL_PATH_SUFFIX = "/fail";
    private static final String SYNTHESIZED_BY = "oprmRoutineSynthesizer";

    private final OprmMarketImportCollectorProperties collectorProperties;
    private final RestClient restClient;

    /** Inicializa o cliente com a URL base do backend e RestClient compartilhado do coletor. */
    public RoutineSynthesizerBackendClient(OprmMarketImportCollectorProperties collectorProperties, RestClient restClient) {
        this.collectorProperties = collectorProperties;
        this.restClient = restClient;
    }

    /** Lista ciclos pendentes que a etapa seis deve transformar em cartão de rotina. */
    public List<RoutineSynthesizerPending> listPendingCycles() {
        String url = collectorProperties.backendBaseUrl() + PENDING_PATH;
        try {
            RoutineSynthesizerPending[] response = restClient.get().uri(url).retrieve().body(RoutineSynthesizerPending[].class);
            List<RoutineSynthesizerPending> pendingCycles = response == null ? List.of() : Arrays.asList(response);
            log.info("Pendências da etapa seis OPRM nichocnae carregadas (endpoint={}, pendingCount={})", url, pendingCycles.size());
            return pendingCycles;
        } catch (RestClientException ex) {
            log.error("Erro ao listar pendências da etapa seis OPRM nichocnae (endpoint={})", url, ex);
            throw ex;
        }
    }

    /** Envia ao backend o cartão de rotina sintetizado para concluir a etapa seis de um ciclo. */
    public RoutineSynthesizerOutput completeStageExecution(RoutineSynthesizerPending pending, RoutineCardDraft draft) {
        String url = collectorProperties.backendBaseUrl() + COMPLETE_PATH_PREFIX + pending.researchCycleId() + COMPLETE_PATH_SUFFIX;
        RoutineSynthesizerCompletionRequest request = toCompletionRequest(pending, draft);
        try {
            RoutineSynthesizerCompletionResponse response = restClient.post()
                    .uri(url)
                    .body(request)
                    .retrieve()
                    .body(RoutineSynthesizerCompletionResponse.class);
            if (response == null) {
                throw new IllegalStateException("Backend OPRM nichocnae retornou corpo vazio ao concluir etapa seis.");
            }
            return new RoutineSynthesizerOutput(
                    response.routineCardId(), response.researchCycleId(), response.cycleStatus(), response.nicheName(), response.confidenceScore(), response.createdAt());
        } catch (RestClientException | IllegalStateException ex) {
            log.error(
                    "Erro ao concluir etapa seis OPRM nichocnae no backend (endpoint={}, researchCycleId={}, confidenceScore={})",
                    url,
                    pending.researchCycleId(),
                    draft == null ? null : draft.confidenceScore(),
                    ex);
            throw ex;
        }
    }

    /** Notifica o backend que a síntese de rotina falhou na etapa seis. */
    public void failStageExecution(RoutineSynthesizerPending pending, RuntimeException error) {
        String url = collectorProperties.backendBaseUrl() + COMPLETE_PATH_PREFIX + pending.researchCycleId() + FAIL_PATH_SUFFIX;
        String message = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
        try {
            restClient.post().uri(url).body(new RoutineSynthesizerFailureRequest(message)).retrieve().toBodilessEntity();
        } catch (RestClientException ex) {
            log.error(
                    "Erro ao notificar falha da etapa seis OPRM nichocnae no backend (endpoint={}, researchCycleId={})",
                    url,
                    pending.researchCycleId(),
                    ex);
            throw ex;
        }
    }

    /** Converte o rascunho do cartão para o contrato esperado pelo endpoint complete da etapa seis. */
    RoutineSynthesizerCompletionRequest toCompletionRequest(RoutineSynthesizerPending pending, RoutineCardDraft draft) {
        return new RoutineSynthesizerCompletionRequest(
                pending.researchCycleId(),
                pending.nicheName(),
                draft.routineSummary(),
                draft.painsSummary(),
                draft.resultsSummary(),
                draft.mechanismOpportunitiesSummary(),
                draft.evidenceSummary(),
                draft.sourceDomains(),
                draft.confidenceScore(),
                SYNTHESIZED_BY);
    }
}

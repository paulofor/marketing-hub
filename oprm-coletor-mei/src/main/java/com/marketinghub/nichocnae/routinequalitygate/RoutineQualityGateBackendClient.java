package com.marketinghub.nichocnae.routinequalitygate;

import com.marketinghub.oprmcoletormei.marketimport.config.OprmMarketImportCollectorProperties;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Chama exclusivamente endpoints OPRM NichoCNAE do backend para executar a etapa sete pelo coletor. */
@Component
public class RoutineQualityGateBackendClient {
    private static final Logger log = LoggerFactory.getLogger(RoutineQualityGateBackendClient.class);
    private static final String PENDING_PATH = "/api/internal/oprm/nichocnae/routine-quality-gate/stage-executions/pending";
    private static final String COMPLETE_PATH_PREFIX = "/api/internal/oprm/nichocnae/routine-quality-gate/stage-executions/";
    private static final String COMPLETE_PATH_SUFFIX = "/complete";
    private static final String FAIL_PATH_SUFFIX = "/fail";
    private static final String REPROCESS_PATH_PREFIX = "/api/oprm/nichocnae/routine-research-orchestrator/recent-processed/";
    private static final String REPROCESS_PATH_SUFFIX = "/reprocess";
    private static final String AUTO_QUALITY_REPROCESS_TRIGGER = "AUTO_QUALITY_REPROCESS";
    private static final String CHECKED_BY = "oprmRoutineQualityGate";

    private final OprmMarketImportCollectorProperties collectorProperties;
    private final RestClient restClient;

    /** Inicializa o cliente com a URL base do backend e RestClient compartilhado do coletor. */
    public RoutineQualityGateBackendClient(OprmMarketImportCollectorProperties collectorProperties, RestClient restClient) {
        this.collectorProperties = collectorProperties;
        this.restClient = restClient;
    }

    /** Lista cartões pendentes que a etapa sete deve avaliar. */
    public List<RoutineQualityGatePending> listPendingCards() {
        String url = collectorProperties.backendBaseUrl() + PENDING_PATH;
        try {
            RoutineQualityGatePending[] response = restClient.get().uri(url).retrieve().body(RoutineQualityGatePending[].class);
            List<RoutineQualityGatePending> pendingCards = response == null ? List.of() : Arrays.asList(response);
            log.info("Pendências da etapa sete OPRM nichocnae carregadas (endpoint={}, pendingCount={})", url, pendingCards.size());
            return pendingCards;
        } catch (RestClientException ex) {
            log.error("Erro ao listar pendências da etapa sete OPRM nichocnae (endpoint={})", url, ex);
            throw ex;
        }
    }

    /** Envia ao backend a decisão de qualidade para concluir a etapa sete de um ciclo. */
    public RoutineQualityGateOutput completeStageExecution(RoutineQualityGatePending pending, RoutineQualityDecision decision) {
        String url = collectorProperties.backendBaseUrl() + COMPLETE_PATH_PREFIX + pending.researchCycleId() + COMPLETE_PATH_SUFFIX;
        RoutineQualityGateCompletionRequest request = toCompletionRequest(pending, decision);
        try {
            RoutineQualityGateCompletionResponse response = restClient.post()
                    .uri(url)
                    .body(request)
                    .retrieve()
                    .body(RoutineQualityGateCompletionResponse.class);
            if (response == null) {
                throw new IllegalStateException("Backend OPRM nichocnae retornou corpo vazio ao concluir etapa sete.");
            }
            return new RoutineQualityGateOutput(
                    response.routineCardId(),
                    response.researchCycleId(),
                    response.cycleStatus(),
                    response.qualityStatus(),
                    response.readyForHypothesis(),
                    response.specificityScore(),
                    response.confidenceScore(),
                    response.duplicationScore(),
                    response.checkedAt());
        } catch (RestClientException | IllegalStateException ex) {
            log.error(
                    "Erro ao concluir etapa sete OPRM nichocnae no backend (endpoint={}, researchCycleId={}, routineCardId={}, qualityStatus={})",
                    url,
                    pending.researchCycleId(),
                    pending.routineCardId(),
                    decision == null ? null : decision.qualityStatus(),
                    ex);
            throw ex;
        }
    }


    /** Solicita ao backend apenas a gravação de um novo ciclo quando o coletor decidiu reprocessar a qualidade. */
    public void reprocessAfterQualityRejection(RoutineQualityGateOutput output) {
        String url = collectorProperties.backendBaseUrl() + REPROCESS_PATH_PREFIX + output.researchCycleId() + REPROCESS_PATH_SUFFIX;
        try {
            restClient.post()
                    .uri(url)
                    .body(new RoutineQualityReprocessRequest(AUTO_QUALITY_REPROCESS_TRIGGER))
                    .retrieve()
                    .toBodilessEntity();
            log.info(
                    "Reprocessamento automático OPRM nichocnae solicitado pelo coletor (endpoint={}, researchCycleId={}, qualityStatus={})",
                    url,
                    output.researchCycleId(),
                    output.qualityStatus());
        } catch (RestClientException ex) {
            log.error(
                    "Erro ao solicitar reprocessamento automático OPRM nichocnae pelo coletor (endpoint={}, researchCycleId={}, qualityStatus={})",
                    url,
                    output.researchCycleId(),
                    output.qualityStatus(),
                    ex);
            throw ex;
        }
    }

    /** Notifica o backend que o gate de qualidade falhou na etapa sete. */
    public void failStageExecution(RoutineQualityGatePending pending, RuntimeException error) {
        String url = collectorProperties.backendBaseUrl() + COMPLETE_PATH_PREFIX + pending.researchCycleId() + FAIL_PATH_SUFFIX;
        String message = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
        try {
            restClient.post().uri(url).body(new RoutineQualityGateFailureRequest(message)).retrieve().toBodilessEntity();
        } catch (RestClientException ex) {
            log.error(
                    "Erro ao notificar falha da etapa sete OPRM nichocnae no backend (endpoint={}, researchCycleId={}, routineCardId={})",
                    url,
                    pending.researchCycleId(),
                    pending.routineCardId(),
                    ex);
            throw ex;
        }
    }

    /** Converte a decisão do gate para o contrato esperado pelo endpoint complete da etapa sete. */
    RoutineQualityGateCompletionRequest toCompletionRequest(RoutineQualityGatePending pending, RoutineQualityDecision decision) {
        return new RoutineQualityGateCompletionRequest(
                pending.researchCycleId(),
                pending.routineCardId(),
                decision.qualityStatus(),
                decision.readyForHypothesis(),
                decision.specificityScore(),
                decision.confidenceScore(),
                decision.duplicationScore(),
                decision.qualityNotes(),
                CHECKED_BY);
    }
}

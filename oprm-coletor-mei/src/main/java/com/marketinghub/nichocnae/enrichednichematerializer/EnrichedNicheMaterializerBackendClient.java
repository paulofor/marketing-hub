package com.marketinghub.nichocnae.enrichednichematerializer;

import com.marketinghub.oprmcoletormei.marketimport.config.OprmMarketImportCollectorProperties;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Chama endpoints OPRM do backend para executar a etapa final de materialização pelo coletor. */
@Component
public class EnrichedNicheMaterializerBackendClient {
    private static final Logger log = LoggerFactory.getLogger(EnrichedNicheMaterializerBackendClient.class);
    private static final String PENDING_PATH = "/api/internal/oprm/nichocnae/enriched-niche-materializer/stage-executions/pending";
    private static final String COMPLETE_PATH_PREFIX = "/api/internal/oprm/nichocnae/enriched-niche-materializer/stage-executions/";
    private static final String COMPLETE_PATH_SUFFIX = "/complete";
    private static final String FAIL_PATH_SUFFIX = "/fail";
    private static final String MATERIALIZED_BY = "oprmEnrichedNicheMaterializer";

    private final OprmMarketImportCollectorProperties collectorProperties;
    private final RestClient restClient;

    /** Inicializa o cliente com a URL base do backend e RestClient compartilhado. */
    public EnrichedNicheMaterializerBackendClient(OprmMarketImportCollectorProperties collectorProperties, RestClient restClient) {
        this.collectorProperties = collectorProperties;
        this.restClient = restClient;
    }

    /** Lista cartões aprovados que ainda precisam alimentar nicho e nicho enriquecido. */
    public List<EnrichedNicheMaterializerPending> listPendingCards() {
        String url = collectorProperties.backendBaseUrl() + PENDING_PATH;
        try {
            EnrichedNicheMaterializerPending[] response = restClient.get().uri(url).retrieve().body(EnrichedNicheMaterializerPending[].class);
            List<EnrichedNicheMaterializerPending> pendingCards = response == null ? List.of() : Arrays.asList(response);
            log.info("Pendências da etapa final OPRM nichocnae carregadas (endpoint={}, pendingCount={})", url, pendingCards.size());
            return pendingCards;
        } catch (RestClientException ex) {
            log.error("Erro ao listar pendências da etapa final OPRM nichocnae (endpoint={})", url, ex);
            throw ex;
        }
    }

    /** Envia o perfil complementar para o backend concluir a materialização final. */
    public EnrichedNicheMaterializerOutput completeStageExecution(EnrichedNicheMaterializerPending pending, EnrichedNicheProfileDraft draft) {
        String url = collectorProperties.backendBaseUrl() + COMPLETE_PATH_PREFIX + pending.researchCycleId() + COMPLETE_PATH_SUFFIX;
        EnrichedNicheMaterializerCompletionRequest request = toCompletionRequest(pending, draft);
        try {
            EnrichedNicheMaterializerCompletionResponse response = restClient.post()
                    .uri(url)
                    .body(request)
                    .retrieve()
                    .body(EnrichedNicheMaterializerCompletionResponse.class);
            if (response == null) {
                throw new IllegalStateException("Backend OPRM nichocnae retornou corpo vazio ao concluir etapa final.");
            }
            return new EnrichedNicheMaterializerOutput(
                    response.researchCycleId(),
                    response.routineCardId(),
                    response.marketNicheId(),
                    response.enrichedNicheProfileId(),
                    response.cycleStatus(),
                    response.materializedAt());
        } catch (RestClientException | IllegalStateException ex) {
            log.error("Erro ao concluir etapa final OPRM nichocnae (endpoint={}, researchCycleId={}, routineCardId={})",
                    url, pending.researchCycleId(), pending.routineCardId(), ex);
            throw ex;
        }
    }

    /** Notifica o backend sobre falha operacional da etapa final. */
    public void failStageExecution(EnrichedNicheMaterializerPending pending, RuntimeException error) {
        String url = collectorProperties.backendBaseUrl() + COMPLETE_PATH_PREFIX + pending.researchCycleId() + FAIL_PATH_SUFFIX;
        String message = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
        try {
            restClient.post().uri(url).body(new EnrichedNicheMaterializerFailureRequest(message)).retrieve().toBodilessEntity();
        } catch (RestClientException ex) {
            log.error("Erro ao notificar falha da etapa final OPRM nichocnae (endpoint={}, researchCycleId={}, routineCardId={})",
                    url, pending.researchCycleId(), pending.routineCardId(), ex);
            throw ex;
        }
    }

    /** Converte o draft no contrato de conclusão aceito pelo backend. */
    EnrichedNicheMaterializerCompletionRequest toCompletionRequest(EnrichedNicheMaterializerPending pending, EnrichedNicheProfileDraft draft) {
        return new EnrichedNicheMaterializerCompletionRequest(
                pending.researchCycleId(),
                pending.routineCardId(),
                draft.personaSummary(),
                draft.languagePatterns(),
                draft.commercialTriggers(),
                draft.objections(),
                MATERIALIZED_BY);
    }
}

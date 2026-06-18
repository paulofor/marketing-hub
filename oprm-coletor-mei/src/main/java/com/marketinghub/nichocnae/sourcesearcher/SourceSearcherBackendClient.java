package com.marketinghub.nichocnae.sourcesearcher;

import com.marketinghub.oprmcoletormei.marketimport.config.OprmMarketImportCollectorProperties;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Chama exclusivamente endpoints OPRM nichocnae do backend para executar a etapa três pelo coletor. */
@Component
public class SourceSearcherBackendClient {
    private static final Logger log = LoggerFactory.getLogger(SourceSearcherBackendClient.class);
    private static final String PENDING_PATH = "/api/internal/oprm/nichocnae/source-searcher/stage-executions/pending";
    private static final String COMPLETE_PATH_PREFIX = "/api/internal/oprm/nichocnae/source-searcher/stage-executions/";
    private static final String COMPLETE_PATH_SUFFIX = "/complete";
    private static final String FAIL_PATH_SUFFIX = "/fail";

    private final OprmMarketImportCollectorProperties collectorProperties;
    private final RestClient restClient;

    /** Inicializa o cliente com a URL base do backend e RestClient compartilhado do coletor. */
    public SourceSearcherBackendClient(OprmMarketImportCollectorProperties collectorProperties, RestClient restClient) {
        this.collectorProperties = collectorProperties;
        this.restClient = restClient;
    }

    /** Lista queries pendentes que a etapa três deve executar em provedor de busca pública. */
    public List<SourceSearcherPending> listPendingQueries() {
        String url = collectorProperties.backendBaseUrl() + PENDING_PATH;
        try {
            SourceSearcherPending[] response = restClient.get().uri(url).retrieve().body(SourceSearcherPending[].class);
            List<SourceSearcherPending> pendingQueries = response == null ? List.of() : Arrays.asList(response);
            log.info(
                    "Pendências da etapa três OPRM nichocnae carregadas (endpoint={}, pendingCount={}, firstResearchCycleId={}, firstResearchQueryId={})",
                    url,
                    pendingQueries.size(),
                    pendingQueries.isEmpty() ? null : pendingQueries.get(0).researchCycleId(),
                    pendingQueries.isEmpty() ? null : pendingQueries.get(0).researchQueryId());
            return pendingQueries;
        } catch (RestClientException ex) {
            log.error("Erro ao listar pendências da etapa três OPRM nichocnae (endpoint={})", url, ex);
            throw ex;
        }
    }

    /** Envia ao backend os resultados normalizados da busca pública para concluir uma query. */
    public SourceSearcherOutput completeStageExecution(
            SourceSearcherPending pending, String searchProvider, List<SourceSearchResult> searchResults) {
        String url = collectorProperties.backendBaseUrl()
                + COMPLETE_PATH_PREFIX
                + pending.researchQueryId()
                + COMPLETE_PATH_SUFFIX;
        SourceSearcherCompletionRequest request = toCompletionRequest(pending, searchProvider, searchResults);
        try {
            log.info(
                    "Enviando conclusão da etapa três OPRM nichocnae ao backend (endpoint={}, researchQueryId={}, researchCycleId={}, searchProvider={}, resultCount={})",
                    url,
                    pending.researchQueryId(),
                    pending.researchCycleId(),
                    searchProvider,
                    searchResults == null ? 0 : searchResults.size());
            SourceSearcherCompletionResponse response = restClient.post()
                    .uri(url)
                    .body(request)
                    .retrieve()
                    .body(SourceSearcherCompletionResponse.class);
            if (response == null) {
                throw new IllegalStateException("Backend OPRM nichocnae retornou corpo vazio ao concluir etapa três.");
            }
            SourceSearcherOutput output = toOutput(response);
            log.info(
                    "Conclusão da etapa três OPRM nichocnae confirmada pelo backend (researchQueryId={}, researchCycleId={}, queryStatus={}, resultCount={}, cycleTotalSourceCandidates={})",
                    output.researchQueryId(),
                    output.researchCycleId(),
                    output.queryStatus(),
                    output.resultCount(),
                    output.cycleTotalSourceCandidates());
            return output;
        } catch (RestClientException | IllegalStateException ex) {
            log.error(
                    "Erro ao concluir etapa três OPRM nichocnae no backend (endpoint={}, researchQueryId={}, researchCycleId={}, resultCount={})",
                    url,
                    pending.researchQueryId(),
                    pending.researchCycleId(),
                    searchResults == null ? null : searchResults.size(),
                    ex);
            throw ex;
        }
    }

    /** Notifica o backend que a execução da query falhou na etapa três. */
    public void failStageExecution(SourceSearcherPending pending, RuntimeException error) {
        String url = collectorProperties.backendBaseUrl()
                + COMPLETE_PATH_PREFIX
                + pending.researchQueryId()
                + FAIL_PATH_SUFFIX;
        String message = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
        try {
            log.info(
                    "Notificando falha da etapa três OPRM nichocnae ao backend (endpoint={}, researchQueryId={}, researchCycleId={}, errorMessage={})",
                    url,
                    pending.researchQueryId(),
                    pending.researchCycleId(),
                    message);
            restClient.post().uri(url).body(new SourceSearcherFailureRequest(message)).retrieve().toBodilessEntity();
        } catch (RestClientException ex) {
            log.error(
                    "Erro ao notificar falha da etapa três OPRM nichocnae no backend (endpoint={}, researchQueryId={}, researchCycleId={})",
                    url,
                    pending.researchQueryId(),
                    pending.researchCycleId(),
                    ex);
            throw ex;
        }
    }

    /** Converte resultados de busca para o contrato esperado pelo endpoint complete da etapa três. */
    SourceSearcherCompletionRequest toCompletionRequest(
            SourceSearcherPending pending, String searchProvider, List<SourceSearchResult> searchResults) {
        List<SourceCandidateRequest> candidates = searchResults == null
                ? List.of()
                : searchResults.stream()
                        .map(result -> new SourceCandidateRequest(
                                result.sourceUrl(),
                                result.sourceTitle(),
                                result.sourceSnippet(),
                                result.sourceDomain(),
                                result.sourceIntent(),
                                result.searchPosition(),
                                Boolean.TRUE.equals(result.commercialPageRisk()) ? "CONTAMINATION_RISK" : "FOUND",
                                result.sourceIntent(),
                                result.routineEvidenceScore(),
                                result.commercialPageRisk(),
                                result.solutionLanguageRisk(),
                                result.sourceClassificationType(),
                                result.sourceFreshnessScore(),
                                result.outdatedSourceRisk(),
                                result.brazilRelevanceScore(),
                                result.autonomousProfessionalEvidenceScore(),
                                result.structuredBusinessDriftRisk(),
                                result.publishedAt()))
                        .toList();
        return new SourceSearcherCompletionRequest(searchProvider, candidates);
    }

    /** Converte a resposta do backend para a saída interna do worker da etapa três. */
    private SourceSearcherOutput toOutput(SourceSearcherCompletionResponse response) {
        return new SourceSearcherOutput(
                response.researchQueryId(),
                response.researchCycleId(),
                response.queryText(),
                response.queryStatus(),
                response.resultCount(),
                response.cycleTotalSourceCandidates(),
                response.candidates() == null ? List.of() : response.candidates());
    }
}

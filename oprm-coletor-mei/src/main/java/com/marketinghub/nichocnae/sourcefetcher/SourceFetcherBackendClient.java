package com.marketinghub.nichocnae.sourcefetcher;

import com.marketinghub.oprmcoletormei.marketimport.config.OprmMarketImportCollectorProperties;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Chama exclusivamente endpoints OPRM nichocnae do backend para executar a etapa quatro pelo coletor. */
@Component
public class SourceFetcherBackendClient {
    private static final Logger log = LoggerFactory.getLogger(SourceFetcherBackendClient.class);
    private static final String PENDING_PATH = "/api/internal/oprm/nichocnae/source-fetcher/stage-executions/pending";
    private static final String COMPLETE_PATH_PREFIX = "/api/internal/oprm/nichocnae/source-fetcher/stage-executions/";
    private static final String COMPLETE_PATH_SUFFIX = "/complete";
    private static final String FAIL_PATH_SUFFIX = "/fail";

    private final OprmMarketImportCollectorProperties collectorProperties;
    private final RestClient restClient;

    /** Inicializa o cliente com a URL base do backend e RestClient compartilhado do coletor. */
    public SourceFetcherBackendClient(OprmMarketImportCollectorProperties collectorProperties, RestClient restClient) {
        this.collectorProperties = collectorProperties;
        this.restClient = restClient;
    }

    /** Lista fontes candidatas pendentes que a etapa quatro deve coletar de forma curta. */
    public List<SourceFetcherPending> listPendingSources() {
        String url = collectorProperties.backendBaseUrl() + PENDING_PATH;
        try {
            SourceFetcherPending[] response = restClient.get().uri(url).retrieve().body(SourceFetcherPending[].class);
            List<SourceFetcherPending> pendingSources = response == null ? List.of() : Arrays.asList(response);
            log.info("Pendências da etapa quatro OPRM nichocnae carregadas (endpoint={}, pendingCount={})", url, pendingSources.size());
            return pendingSources;
        } catch (RestClientException ex) {
            log.error("Erro ao listar pendências da etapa quatro OPRM nichocnae (endpoint={})", url, ex);
            throw ex;
        }
    }

    /** Envia ao backend o snapshot curto coletado para concluir uma fonte candidata. */
    public SourceFetcherOutput completeStageExecution(SourceFetcherPending pending, FetchedSourceSnapshot snapshot) {
        String url = collectorProperties.backendBaseUrl()
                + COMPLETE_PATH_PREFIX
                + pending.sourceCandidateId()
                + COMPLETE_PATH_SUFFIX;
        SourceFetcherCompletionRequest request = toCompletionRequest(snapshot);
        try {
            SourceFetcherCompletionResponse response = restClient.post()
                    .uri(url)
                    .body(request)
                    .retrieve()
                    .body(SourceFetcherCompletionResponse.class);
            if (response == null) {
                throw new IllegalStateException("Backend OPRM nichocnae retornou corpo vazio ao concluir etapa quatro.");
            }
            return toOutput(response);
        } catch (RestClientException | IllegalStateException ex) {
            log.error(
                    "Erro ao concluir etapa quatro OPRM nichocnae no backend (endpoint={}, sourceCandidateId={}, researchCycleId={}, httpStatus={})",
                    url,
                    pending.sourceCandidateId(),
                    pending.researchCycleId(),
                    snapshot == null ? null : snapshot.httpStatus(),
                    ex);
            throw ex;
        }
    }

    /** Notifica o backend que a coleta da fonte falhou na etapa quatro. */
    public void failStageExecution(SourceFetcherPending pending, RuntimeException error) {
        String url = collectorProperties.backendBaseUrl()
                + COMPLETE_PATH_PREFIX
                + pending.sourceCandidateId()
                + FAIL_PATH_SUFFIX;
        String message = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
        try {
            restClient.post().uri(url).body(new SourceFetcherFailureRequest(message, 0)).retrieve().toBodilessEntity();
        } catch (RestClientException ex) {
            log.error(
                    "Erro ao notificar falha da etapa quatro OPRM nichocnae no backend (endpoint={}, sourceCandidateId={}, researchCycleId={})",
                    url,
                    pending.sourceCandidateId(),
                    pending.researchCycleId(),
                    ex);
            throw ex;
        }
    }

    /** Converte o snapshot coletado para o contrato esperado pelo endpoint complete da etapa quatro. */
    SourceFetcherCompletionRequest toCompletionRequest(FetchedSourceSnapshot snapshot) {
        return new SourceFetcherCompletionRequest(
                snapshot.sourceUrl(),
                snapshot.sourceDomain(),
                snapshot.sourceTitle(),
                snapshot.sourceType(),
                snapshot.sourceIntent(),
                snapshot.routineEvidenceScore(),
                snapshot.commercialPageRisk(),
                snapshot.solutionLanguageRisk(),
                snapshot.sourceClassificationType(),
                snapshot.sourceFreshnessScore(),
                snapshot.outdatedSourceRisk(),
                snapshot.brazilRelevanceScore(),
                snapshot.autonomousProfessionalEvidenceScore(),
                snapshot.structuredBusinessDriftRisk(),
                snapshot.publishedAt(),
                snapshot.snippet(),
                snapshot.shortExcerpt(),
                snapshot.fetchStatus(),
                snapshot.httpStatus(),
                snapshot.storagePolicy(),
                snapshot.licenseState(),
                snapshot.relevanceScore());
    }

    /** Converte a resposta do backend para a saída interna do worker da etapa quatro. */
    private SourceFetcherOutput toOutput(SourceFetcherCompletionResponse response) {
        return new SourceFetcherOutput(
                response.sourceCandidateId(),
                response.researchCycleId(),
                response.selectedForFetch(),
                response.relevanceScore(),
                response.cycleTotalSourceSnapshots(),
                response.snapshot());
    }
}

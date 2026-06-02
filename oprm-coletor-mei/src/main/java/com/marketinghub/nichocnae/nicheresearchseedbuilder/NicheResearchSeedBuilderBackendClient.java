package com.marketinghub.nichocnae.nicheresearchseedbuilder;

import com.marketinghub.oprmcoletormei.marketimport.config.OprmMarketImportCollectorProperties;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Chama exclusivamente endpoints OPRM nichocnae do backend para persistir a etapa dois do pipeline. */
@Component
public class NicheResearchSeedBuilderBackendClient {
    private static final Logger log = LoggerFactory.getLogger(NicheResearchSeedBuilderBackendClient.class);
    private static final String PENDING_PATH = "/api/internal/oprm/nichocnae/niche-research-seed-builder/stage-executions/pending";
    private static final String DETAIL_PATH_PREFIX = "/api/oprm/nichocnae/niche-research-seed-builder/stage-executions/";
    private static final String COMPLETE_PATH_PREFIX = "/api/internal/oprm/nichocnae/niche-research-seed-builder/stage-executions/";
    private static final String COMPLETE_PATH_SUFFIX = "/complete";
    private static final String FAIL_PATH_SUFFIX = "/fail";

    private final OprmMarketImportCollectorProperties collectorProperties;
    private final RestClient restClient;

    /** Inicializa o cliente com a URL base do backend e o RestClient compartilhado do coletor. */
    public NicheResearchSeedBuilderBackendClient(
            OprmMarketImportCollectorProperties collectorProperties,
            RestClient restClient) {
        this.collectorProperties = collectorProperties;
        this.restClient = restClient;
    }

    /** Lista ciclos em execução que ainda precisam de seed e queries de pesquisa na etapa dois. */
    public List<NicheResearchSeedBuilderPending> listPendingSeeds() {
        String url = collectorProperties.backendBaseUrl() + PENDING_PATH;
        try {
            NicheResearchSeedBuilderPending[] response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(NicheResearchSeedBuilderPending[].class);
            return response == null ? List.of() : Arrays.asList(response);
        } catch (RestClientException ex) {
            log.error("Erro ao listar pendências da etapa dois OPRM nichocnae (endpoint={})", url, ex);
            throw ex;
        }
    }

    /** Detalha o seed e as queries já persistidos para um ciclo de pesquisa de rotina. */
    public NicheResearchSeedBuilderOutput detailStageExecution(Long researchCycleId) {
        String url = collectorProperties.backendBaseUrl() + DETAIL_PATH_PREFIX + researchCycleId;
        try {
            NicheResearchSeedBuilderOutput response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(NicheResearchSeedBuilderOutput.class);
            if (response == null) {
                throw new IllegalStateException("Backend OPRM nichocnae retornou corpo vazio ao detalhar etapa dois.");
            }
            return response;
        } catch (RestClientException | IllegalStateException ex) {
            log.error(
                    "Erro ao detalhar etapa dois OPRM nichocnae (endpoint={}, researchCycleId={})",
                    url,
                    researchCycleId,
                    ex);
            throw ex;
        }
    }

    /** Envia ao backend a saída validada da IA para gravar oprm_niche_research_seed e oprm_research_query. */
    public NicheResearchSeedBuilderOutput completeStageExecution(OpenAiSeedBuilderResult result) {
        Long researchCycleId = result.output().researchCycleId();
        String url = collectorProperties.backendBaseUrl() + COMPLETE_PATH_PREFIX + researchCycleId + COMPLETE_PATH_SUFFIX;
        NicheResearchSeedBuilderCompletionRequest request = new NicheResearchSeedBuilderCompletionRequest(
                result.output(),
                result.model(),
                result.rawModelResponse(),
                result.inputTokens(),
                result.outputTokens(),
                result.openAiResponseId());
        try {
            NicheResearchSeedBuilderOutput response = restClient.post()
                    .uri(url)
                    .body(request)
                    .retrieve()
                    .body(NicheResearchSeedBuilderOutput.class);
            if (response == null) {
                throw new IllegalStateException("Backend OPRM nichocnae retornou corpo vazio ao concluir etapa dois.");
            }
            return response;
        } catch (RestClientException | IllegalStateException ex) {
            log.error(
                    "Erro ao concluir etapa dois OPRM nichocnae no backend (endpoint={}, researchCycleId={}, queryCount={})",
                    url,
                    researchCycleId,
                    result.output().queries() == null ? null : result.output().queries().size(),
                    ex);
            throw ex;
        }
    }

    /** Notifica o backend que a etapa dois falhou para preservar rastreabilidade do ciclo. */
    public void failStageExecution(Long researchCycleId, RuntimeException error) {
        String url = collectorProperties.backendBaseUrl() + COMPLETE_PATH_PREFIX + researchCycleId + FAIL_PATH_SUFFIX;
        NicheResearchSeedBuilderFailureRequest request = new NicheResearchSeedBuilderFailureRequest(
                researchCycleId,
                "oprmNicheResearchSeedBuilder",
                error.getMessage(),
                error.toString());
        try {
            restClient.post().uri(url).body(request).retrieve().toBodilessEntity();
        } catch (RestClientException ex) {
            log.error(
                    "Erro ao notificar falha da etapa dois OPRM nichocnae no backend (endpoint={}, researchCycleId={})",
                    url,
                    researchCycleId,
                    ex);
            throw ex;
        }
    }
}

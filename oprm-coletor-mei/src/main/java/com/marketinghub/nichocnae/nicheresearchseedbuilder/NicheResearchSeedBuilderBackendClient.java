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
        NicheResearchSeedBuilderBackendCompletionRequest request = toBackendCompletionRequest(result);
        try {
            NicheResearchSeedBuilderBackendCompletionResponse response = restClient.post()
                    .uri(url)
                    .body(request)
                    .retrieve()
                    .body(NicheResearchSeedBuilderBackendCompletionResponse.class);
            if (response == null) {
                throw new IllegalStateException("Backend OPRM nichocnae retornou corpo vazio ao concluir etapa dois.");
            }
            return toOutput(response);
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

    /** Converte a saída validada da IA e telemetria para o DTO achatado esperado pelo backend. */
    NicheResearchSeedBuilderBackendCompletionRequest toBackendCompletionRequest(OpenAiSeedBuilderResult result) {
        NicheResearchSeedBuilderOutput output = result == null ? null : result.output();
        if (output == null || output.seed() == null) {
            throw new IllegalArgumentException("Seed builder output is required to complete stage two.");
        }
        NicheResearchSeed seed = output.seed();
        return new NicheResearchSeedBuilderBackendCompletionRequest(
                seed.nicheName(),
                seed.businessType(),
                seed.operationType(),
                seed.customerType(),
                seed.commercialObjects(),
                seed.initialAssumptions(),
                seed.confidenceLevel(),
                seed.createdBy(),
                result.model(),
                result.rawModelResponse(),
                result.inputTokens(),
                result.outputTokens(),
                result.openAiResponseId(),
                output.queries());
    }

    /** Converte a resposta achatada do backend para a saída interna do worker da etapa dois. */
    NicheResearchSeedBuilderOutput toOutput(NicheResearchSeedBuilderBackendCompletionResponse response) {
        NicheResearchSeed seed = new NicheResearchSeed(
                response.researchCycleId(),
                response.cnaeCode(),
                response.cnaeDescription(),
                response.nicheName(),
                response.businessType(),
                response.operationType(),
                response.customerType(),
                response.commercialObjects(),
                response.initialAssumptions(),
                response.confidenceLevel(),
                response.createdBy());
        List<ResearchQuery> queries = response.queries() == null
                ? List.of()
                : response.queries().stream()
                        .map(query -> new ResearchQuery(
                                query.researchCycleId(),
                                query.queryText(),
                                query.queryGoal(),
                                query.sourceGroup(),
                                query.priority(),
                                query.status(),
                                query.createdBy()))
                        .toList();
        return new NicheResearchSeedBuilderOutput(response.researchCycleId(), seed, queries);
    }

    /** Notifica o backend que a etapa dois falhou para preservar rastreabilidade do ciclo. */
    public void failStageExecution(Long researchCycleId, RuntimeException error) {
        String url = collectorProperties.backendBaseUrl() + COMPLETE_PATH_PREFIX + researchCycleId + FAIL_PATH_SUFFIX;
        NicheResearchSeedBuilderFailureRequest request = new NicheResearchSeedBuilderFailureRequest(
                researchCycleId,
                "oprmNicheResearchSeedBuilder",
                buildFailureMessage(error),
                buildFailureDetail(error));
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

    /** Monta a mensagem principal usando a causa raiz quando a exceção encapsular uma falha técnica relevante. */
    private String buildFailureMessage(RuntimeException error) {
        Throwable rootCause = rootCause(error);
        String rootMessage = rootCause.getMessage();
        if (rootMessage == null || rootMessage.isBlank() || rootCause == error) {
            return error.getMessage();
        }
        return error.getMessage() + " | Causa raiz: " + rootMessage;
    }

    /** Monta o detalhe técnico da falha para o backend preservar diagnóstico suficiente no ciclo. */
    private String buildFailureDetail(RuntimeException error) {
        Throwable rootCause = rootCause(error);
        if (rootCause == error) {
            return error.toString();
        }
        return error + " | rootCause=" + rootCause;
    }

    /** Encontra a causa raiz sem depender de bibliotecas externas para manter a borda simples. */
    private Throwable rootCause(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }
}

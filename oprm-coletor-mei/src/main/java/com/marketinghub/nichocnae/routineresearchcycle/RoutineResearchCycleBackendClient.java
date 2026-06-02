package com.marketinghub.nichocnae.routineresearchcycle;

import com.marketinghub.oprmcoletormei.marketimport.config.OprmMarketImportCollectorProperties;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Chama exclusivamente endpoints OPRM nichocnae do backend para consultar e controlar a etapa um do pipeline. */
@Component
public class RoutineResearchCycleBackendClient {
    private static final Logger log = LoggerFactory.getLogger(RoutineResearchCycleBackendClient.class);
    private static final String PENDING_PATH = "/api/internal/oprm/nichocnae/routine-research-cycle/stage-executions/pending";
    private static final String LIST_BY_SOURCE_PATH_PREFIX = "/api/oprm/nichocnae/";
    private static final String LIST_BY_SOURCE_PATH_SUFFIX = "/routine-research-cycle/stage-executions";
    private static final String DETAIL_PATH_PREFIX = "/api/oprm/nichocnae/routine-research-cycle/stage-executions/";

    private final OprmMarketImportCollectorProperties collectorProperties;
    private final RestClient restClient;

    /** Inicializa o cliente com a URL base do backend e o RestClient compartilhado do coletor. */
    public RoutineResearchCycleBackendClient(
            OprmMarketImportCollectorProperties collectorProperties,
            RestClient restClient) {
        this.collectorProperties = collectorProperties;
        this.restClient = restClient;
    }

    /** Lista ciclos em execução que estão prontos para a etapa um controlar e encaminhar no pipeline. */
    public List<RoutineResearchCyclePending> listPendingCycles() {
        String url = collectorProperties.backendBaseUrl() + PENDING_PATH;
        try {
            RoutineResearchCyclePending[] response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(RoutineResearchCyclePending[].class);
            return response == null ? List.of() : Arrays.asList(response);
        } catch (RestClientException ex) {
            log.error("Erro ao listar pendências da etapa um OPRM nichocnae (endpoint={})", url, ex);
            throw ex;
        }
    }

    /** Lista os ciclos de pesquisa de rotina vinculados ao nicho CNAE de origem informado. */
    public List<RoutineResearchCycleSummary> listBySourceNicheId(Long sourceNicheId) {
        String url = collectorProperties.backendBaseUrl()
                + LIST_BY_SOURCE_PATH_PREFIX
                + sourceNicheId
                + LIST_BY_SOURCE_PATH_SUFFIX;
        try {
            RoutineResearchCycleSummary[] response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(RoutineResearchCycleSummary[].class);
            return response == null ? List.of() : Arrays.asList(response);
        } catch (RestClientException ex) {
            log.error(
                    "Erro ao listar ciclos da etapa um OPRM nichocnae por nicho (endpoint={}, sourceNicheId={})",
                    url,
                    sourceNicheId,
                    ex);
            throw ex;
        }
    }

    /** Detalha uma execução específica da etapa um para validar o estado canônico do ciclo no backend. */
    public RoutineResearchCycleDetail detailStageExecution(Long researchCycleId) {
        String url = collectorProperties.backendBaseUrl() + DETAIL_PATH_PREFIX + researchCycleId;
        try {
            RoutineResearchCycleDetail response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(RoutineResearchCycleDetail.class);
            if (response == null) {
                throw new IllegalStateException("Backend OPRM nichocnae retornou corpo vazio ao detalhar etapa um.");
            }
            return response;
        } catch (RestClientException | IllegalStateException ex) {
            log.error(
                    "Erro ao detalhar ciclo da etapa um OPRM nichocnae (endpoint={}, researchCycleId={})",
                    url,
                    researchCycleId,
                    ex);
            throw ex;
        }
    }
}

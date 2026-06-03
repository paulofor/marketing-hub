package com.marketinghub.nichocnae.routineresearchorchestrator;

import com.marketinghub.oprmcoletormei.marketimport.config.OprmMarketImportCollectorProperties;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Chama exclusivamente endpoints internos OPRM nichocnae do backend para a etapa zero do pipeline. */
@Component
public class RoutineResearchOrchestratorBackendClient {
    private static final Logger log = LoggerFactory.getLogger(RoutineResearchOrchestratorBackendClient.class);
    private static final String PENDING_PATH = "/api/internal/oprm/nichocnae/routine-research-orchestrator/stage-executions/pending";
    private static final String RUN_NEXT_PATH = "/api/internal/oprm/nichocnae/routine-research-orchestrator/run-next";

    private final OprmMarketImportCollectorProperties collectorProperties;
    private final RestClient restClient;

    /** Inicializa o cliente com a URL base do backend e o RestClient compartilhado do coletor. */
    public RoutineResearchOrchestratorBackendClient(
            OprmMarketImportCollectorProperties collectorProperties,
            RestClient restClient) {
        this.collectorProperties = collectorProperties;
        this.restClient = restClient;
    }

    /** Lista o próximo candidato pendente apenas para visibilidade e confirmação antes do disparo atômico. */
    public List<RoutineResearchOrchestratorPending> listPendingCandidates() {
        String url = collectorProperties.backendBaseUrl() + PENDING_PATH;
        try {
            log.info("Chamando backend para listar pendências da etapa zero OPRM nichocnae (method=GET, endpoint={})", url);
            RoutineResearchOrchestratorPending[] response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(RoutineResearchOrchestratorPending[].class);
            List<RoutineResearchOrchestratorPending> pendingCandidates = response == null ? List.of() : Arrays.asList(response);
            log.info(
                    "Backend retornou pendências da etapa zero OPRM nichocnae (endpoint={}, count={})",
                    url,
                    pendingCandidates.size());
            return pendingCandidates;
        } catch (RestClientException ex) {
            log.error("Erro ao listar pendências da etapa zero OPRM nichocnae (endpoint={})", url, ex);
            throw ex;
        }
    }

    /** Solicita ao backend a execução atômica da etapa zero para abrir o próximo ciclo de rotina. */
    public RoutineResearchOrchestratorOutput runNext() {
        String url = collectorProperties.backendBaseUrl() + RUN_NEXT_PATH;
        try {
            log.info("Chamando backend para executar etapa zero OPRM nichocnae (method=POST, endpoint={})", url);
            RoutineResearchOrchestratorOutput response = restClient.post()
                    .uri(url)
                    .retrieve()
                    .body(RoutineResearchOrchestratorOutput.class);
            if (response == null) {
                throw new IllegalStateException("Backend OPRM nichocnae retornou corpo vazio na etapa zero.");
            }
            log.info(
                    "Backend concluiu execução da etapa zero OPRM nichocnae (endpoint={}, started={}, researchCycleId={}, sourceNicheId={}, routineStatus={}, cycleStatus={}, message={})",
                    url,
                    response.started(),
                    response.researchCycleId(),
                    response.sourceNicheId(),
                    response.routineResearchStatus(),
                    response.cycleStatus(),
                    response.message());
            return response;
        } catch (RestClientException | IllegalStateException ex) {
            log.error("Erro ao executar etapa zero OPRM nichocnae (endpoint={})", url, ex);
            throw ex;
        }
    }
}

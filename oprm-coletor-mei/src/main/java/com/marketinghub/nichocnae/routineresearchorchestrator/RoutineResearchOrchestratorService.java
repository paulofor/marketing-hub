package com.marketinghub.nichocnae.routineresearchorchestrator;

import com.marketinghub.nichocnae.pipeline.ArtifactStore;
import com.marketinghub.nichocnae.pipeline.PipelineWorker;
import com.marketinghub.nichocnae.pipeline.StageExecution;
import com.marketinghub.nichocnae.pipeline.StageResult;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Orquestra manualmente a etapa zero do submódulo nichocnae sem criar agendamento automático. */
@Service
public class RoutineResearchOrchestratorService {
    private static final Logger log = LoggerFactory.getLogger(RoutineResearchOrchestratorService.class);

    private final RoutineResearchOrchestratorBackendClient backendClient;
    private final RoutineResearchOrchestratorProcessor processor;
    private final ArtifactStore artifactStore;

    /** Inicializa o serviço com a borda backend, processor concreto e armazenamento genérico de artefatos. */
    public RoutineResearchOrchestratorService(
            RoutineResearchOrchestratorBackendClient backendClient,
            RoutineResearchOrchestratorProcessor processor,
            ArtifactStore artifactStore) {
        this.backendClient = backendClient;
        this.processor = processor;
        this.artifactStore = artifactStore;
    }

    /** Lista o candidato que o backend selecionaria na próxima execução da etapa zero. */
    public List<RoutineResearchOrchestratorPending> listPendingCandidates() {
        log.info("Listando pendências da etapa zero OPRM nichocnae pelo coletor.");
        List<RoutineResearchOrchestratorPending> pendingCandidates = backendClient.listPendingCandidates();
        log.info("Pendências da etapa zero OPRM nichocnae recebidas (count={})", pendingCandidates.size());
        return pendingCandidates;
    }

    /** Executa a etapa zero sob demanda, sem scheduler, e retorna o ciclo criado ou a ausência de pendências. */
    public RoutineResearchOrchestratorOutput runNext(String requestedBy) {
        StageExecution<RoutineResearchOrchestratorInput> execution = new StageExecution<>(
                "oprm-routine-research-orchestrator-run-next",
                new RoutineResearchOrchestratorInput(requestedBy),
                Map.of("stage", "oprmRoutineResearchOrchestrator"));
        try {
            log.info(
                    "Iniciando execução da etapa zero OPRM nichocnae no coletor (executionId={}, requestedBy={}, metadata={})",
                    execution.idJob(),
                    requestedBy,
                    execution.config());
            PipelineWorker<RoutineResearchOrchestratorInput, RoutineResearchOrchestratorOutput> worker = new PipelineWorker<>(
                    processor, artifactStore);
            StageResult<RoutineResearchOrchestratorOutput> result = worker.processResult(execution);
            RoutineResearchOrchestratorOutput output = result.output();
            log.info(
                    "Etapa zero OPRM nichocnae finalizada no coletor (executionId={}, requestedBy={}, started={}, researchCycleId={}, sourceNicheId={}, routineStatus={}, cycleStatus={}, metrics={})",
                    execution.idJob(),
                    requestedBy,
                    output.started(),
                    output.researchCycleId(),
                    output.sourceNicheId(),
                    output.routineResearchStatus(),
                    output.cycleStatus(),
                    result.metrics());
            return output;
        } catch (RuntimeException ex) {
            log.error("Erro ao executar sob demanda a etapa zero OPRM nichocnae (requestedBy={})", requestedBy, ex);
            throw ex;
        }
    }
}

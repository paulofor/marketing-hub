package com.marketinghub.nichocnae.routineresearchcycle;

import com.marketinghub.nichocnae.pipeline.ArtifactStore;
import com.marketinghub.nichocnae.pipeline.PipelineWorker;
import com.marketinghub.nichocnae.pipeline.StageExecution;
import com.marketinghub.nichocnae.pipeline.StageResult;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Orquestra manualmente a etapa um do submódulo nichocnae sem criar agendamento automático. */
@Service
public class RoutineResearchCycleService {
    private static final Logger log = LoggerFactory.getLogger(RoutineResearchCycleService.class);

    private final RoutineResearchCycleBackendClient backendClient;
    private final RoutineResearchCycleProcessor processor;
    private final ArtifactStore artifactStore;

    /** Inicializa o serviço com a borda backend, processor concreto e armazenamento genérico de artefatos. */
    public RoutineResearchCycleService(
            RoutineResearchCycleBackendClient backendClient,
            RoutineResearchCycleProcessor processor,
            ArtifactStore artifactStore) {
        this.backendClient = backendClient;
        this.processor = processor;
        this.artifactStore = artifactStore;
    }

    /** Lista ciclos em execução disponíveis para continuidade do pipeline de pesquisa de rotina. */
    public List<RoutineResearchCyclePending> listPendingCycles() {
        return backendClient.listPendingCycles();
    }

    /** Lista o histórico operacional de ciclos associados ao nicho CNAE de origem. */
    public List<RoutineResearchCycleSummary> listBySourceNicheId(Long sourceNicheId) {
        return backendClient.listBySourceNicheId(sourceNicheId);
    }

    /** Detalha diretamente um ciclo de pesquisa de rotina mantido como fonte de verdade no backend. */
    public RoutineResearchCycleDetail detailStageExecution(Long researchCycleId) {
        return backendClient.detailStageExecution(researchCycleId);
    }

    /** Processa sob demanda os ciclos pendentes pela etapa um e retorna detalhes auditáveis de cada ciclo. */
    public List<RoutineResearchCycleDetail> processPending(String requestedBy) {
        List<RoutineResearchCyclePending> pendingCycles = backendClient.listPendingCycles();
        return pendingCycles.stream()
                .map(pending -> processOne(pending, requestedBy))
                .toList();
    }

    /** Executa o worker genérico para uma unidade de trabalho da etapa um e preserva log contextual em falha. */
    private RoutineResearchCycleDetail processOne(RoutineResearchCyclePending pending, String requestedBy) {
        StageExecution<RoutineResearchCyclePending> execution = new StageExecution<>(
                "oprm-routine-research-cycle-" + pending.researchCycleId(),
                pending,
                Map.of("stage", "oprmRoutineResearchCycle", "requestedBy", requestedBy));
        try {
            PipelineWorker<RoutineResearchCyclePending, RoutineResearchCycleDetail> worker = new PipelineWorker<>(
                    processor, artifactStore);
            StageResult<RoutineResearchCycleDetail> result = worker.processResult(execution);
            return result.output();
        } catch (RuntimeException ex) {
            log.error(
                    "Erro ao executar sob demanda a etapa um OPRM nichocnae (researchCycleId={}, sourceNicheId={}, requestedBy={})",
                    pending.researchCycleId(),
                    pending.sourceNicheId(),
                    requestedBy,
                    ex);
            throw ex;
        }
    }
}

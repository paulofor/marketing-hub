package com.marketinghub.nichocnae.routinesynthesizer;

import com.marketinghub.nichocnae.pipeline.ArtifactStore;
import com.marketinghub.nichocnae.pipeline.PipelineWorker;
import com.marketinghub.nichocnae.pipeline.StageExecution;
import com.marketinghub.nichocnae.pipeline.StageResult;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Orquestra a etapa seis do pipeline nichocnae para sintetizar cartões de rotina no coletor OPRM. */
@Service
public class RoutineSynthesizerService {
    private static final Logger log = LoggerFactory.getLogger(RoutineSynthesizerService.class);

    private final RoutineSynthesizerBackendClient backendClient;
    private final RoutineSynthesizerProcessor processor;
    private final ArtifactStore artifactStore;

    /** Inicializa o serviço com a borda backend, processor concreto e armazenamento genérico de artefatos. */
    public RoutineSynthesizerService(
            RoutineSynthesizerBackendClient backendClient, RoutineSynthesizerProcessor processor, ArtifactStore artifactStore) {
        this.backendClient = backendClient;
        this.processor = processor;
        this.artifactStore = artifactStore;
    }

    /** Lista ciclos pendentes que ainda precisam passar pela síntese de rotina. */
    public List<RoutineSynthesizerPending> listPendingCycles() {
        return backendClient.listPendingCycles();
    }

    /** Processa os ciclos pendentes pela etapa seis e retorna as saídas gravadas no backend. */
    public List<RoutineSynthesizerOutput> processPending(String requestedBy) {
        List<RoutineSynthesizerPending> pendingCycles = backendClient.listPendingCycles();
        return pendingCycles.stream()
                .map(pending -> processOne(pending, requestedBy))
                .toList();
    }

    /** Executa o worker genérico para um ciclo da etapa seis e registra falha contextual no backend. */
    private RoutineSynthesizerOutput processOne(RoutineSynthesizerPending pending, String requestedBy) {
        StageExecution<RoutineSynthesizerPending> execution = new StageExecution<>(
                "oprm-routine-synthesizer-" + pending.researchCycleId(),
                pending,
                Map.of("stage", "oprmRoutineSynthesizer", "requestedBy", requestedBy));
        try {
            PipelineWorker<RoutineSynthesizerPending, RoutineSynthesizerOutput> worker = new PipelineWorker<>(processor, artifactStore);
            StageResult<RoutineSynthesizerOutput> result = worker.processResult(execution);
            return result.output();
        } catch (RuntimeException ex) {
            log.error(
                    "Erro ao executar etapa seis OPRM nichocnae (researchCycleId={}, sourceNicheId={}, requestedBy={})",
                    pending.researchCycleId(),
                    pending.sourceNicheId(),
                    requestedBy,
                    ex);
            backendClient.failStageExecution(pending, ex);
            throw ex;
        }
    }
}

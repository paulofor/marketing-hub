package com.marketinghub.nichocnae.routinequalitygate;

import com.marketinghub.nichocnae.pipeline.ArtifactStore;
import com.marketinghub.nichocnae.pipeline.PipelineWorker;
import com.marketinghub.nichocnae.pipeline.StageExecution;
import com.marketinghub.nichocnae.pipeline.StageResult;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Orquestra a etapa sete do pipeline NichoCNAE para avaliar qualidade dos cartões de rotina. */
@Service
public class RoutineQualityGateService {
    private static final Logger log = LoggerFactory.getLogger(RoutineQualityGateService.class);

    private final RoutineQualityGateBackendClient backendClient;
    private final RoutineQualityGateProcessor processor;
    private final ArtifactStore artifactStore;

    /** Inicializa o serviço com a borda backend, processor concreto e armazenamento genérico de artefatos. */
    public RoutineQualityGateService(
            RoutineQualityGateBackendClient backendClient, RoutineQualityGateProcessor processor, ArtifactStore artifactStore) {
        this.backendClient = backendClient;
        this.processor = processor;
        this.artifactStore = artifactStore;
    }

    /** Lista cartões pendentes que ainda precisam passar pelo gate de qualidade. */
    public List<RoutineQualityGatePending> listPendingCards() {
        return backendClient.listPendingCards();
    }

    /** Processa os cartões pendentes pela etapa sete e retorna as decisões gravadas no backend. */
    public List<RoutineQualityGateOutput> processPending(String requestedBy) {
        List<RoutineQualityGatePending> pendingCards = backendClient.listPendingCards();
        return pendingCards.stream()
                .map(pending -> processOne(pending, requestedBy))
                .toList();
    }

    /** Executa o worker genérico para um cartão da etapa sete e registra falha contextual no backend. */
    private RoutineQualityGateOutput processOne(RoutineQualityGatePending pending, String requestedBy) {
        StageExecution<RoutineQualityGatePending> execution = new StageExecution<>(
                "oprm-routine-quality-gate-" + pending.researchCycleId(),
                pending,
                Map.of("stage", "oprmRoutineQualityGate", "requestedBy", requestedBy));
        try {
            PipelineWorker<RoutineQualityGatePending, RoutineQualityGateOutput> worker = new PipelineWorker<>(processor, artifactStore);
            StageResult<RoutineQualityGateOutput> result = worker.processResult(execution);
            return result.output();
        } catch (RuntimeException ex) {
            log.error(
                    "Erro ao executar etapa sete OPRM nichocnae (researchCycleId={}, routineCardId={}, requestedBy={})",
                    pending.researchCycleId(),
                    pending.routineCardId(),
                    requestedBy,
                    ex);
            backendClient.failStageExecution(pending, ex);
            throw ex;
        }
    }
}

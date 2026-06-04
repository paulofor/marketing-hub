package com.marketinghub.nichocnae.signalextractor;

import com.marketinghub.nichocnae.pipeline.ArtifactStore;
import com.marketinghub.nichocnae.pipeline.PipelineWorker;
import com.marketinghub.nichocnae.pipeline.StageExecution;
import com.marketinghub.nichocnae.pipeline.StageResult;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Orquestra a etapa cinco do pipeline nichocnae para extrair sinais estruturados no coletor OPRM. */
@Service
public class SignalExtractorService {
    private static final Logger log = LoggerFactory.getLogger(SignalExtractorService.class);

    private final SignalExtractorBackendClient backendClient;
    private final SignalExtractorProcessor processor;
    private final ArtifactStore artifactStore;

    /** Inicializa o serviço com a borda backend, processor concreto e armazenamento genérico de artefatos. */
    public SignalExtractorService(
            SignalExtractorBackendClient backendClient, SignalExtractorProcessor processor, ArtifactStore artifactStore) {
        this.backendClient = backendClient;
        this.processor = processor;
        this.artifactStore = artifactStore;
    }

    /** Lista snapshots pendentes que ainda precisam passar pela extração de sinais. */
    public List<SignalExtractorPending> listPendingSnapshots() {
        return backendClient.listPendingSnapshots();
    }

    /** Processa os snapshots pendentes pela etapa cinco e retorna as saídas gravadas no backend. */
    public List<SignalExtractorOutput> processPending(String requestedBy) {
        List<SignalExtractorPending> pendingSnapshots = backendClient.listPendingSnapshots();
        return pendingSnapshots.stream()
                .map(pending -> processOne(pending, requestedBy))
                .toList();
    }

    /** Executa o worker genérico para um snapshot da etapa cinco e registra falha contextual no backend. */
    private SignalExtractorOutput processOne(SignalExtractorPending pending, String requestedBy) {
        StageExecution<SignalExtractorPending> execution = new StageExecution<>(
                "oprm-signal-extractor-" + pending.sourceSnapshotId(),
                pending,
                Map.of("stage", "oprmSignalExtractor", "requestedBy", requestedBy));
        try {
            PipelineWorker<SignalExtractorPending, SignalExtractorOutput> worker = new PipelineWorker<>(processor, artifactStore);
            StageResult<SignalExtractorOutput> result = worker.processResult(execution);
            return result.output();
        } catch (RuntimeException ex) {
            log.error(
                    "Erro ao executar etapa cinco OPRM nichocnae (sourceSnapshotId={}, sourceCandidateId={}, researchCycleId={}, requestedBy={})",
                    pending.sourceSnapshotId(),
                    pending.sourceCandidateId(),
                    pending.researchCycleId(),
                    requestedBy,
                    ex);
            backendClient.failStageExecution(pending, ex);
            throw ex;
        }
    }
}

package com.marketinghub.nichocnae.meiaudiencesegmenter;

import com.marketinghub.nichocnae.pipeline.ArtifactStore;
import com.marketinghub.nichocnae.pipeline.PipelineWorker;
import com.marketinghub.nichocnae.pipeline.StageExecution;
import com.marketinghub.nichocnae.pipeline.StageResult;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Orquestra a etapa de segmentação comportamental MEI/autônomo no coletor OPRM. */
@Service
public class MeiAudienceSegmenterService {
    private static final Logger log = LoggerFactory.getLogger(MeiAudienceSegmenterService.class);

    private final MeiAudienceSegmenterBackendClient backendClient;
    private final MeiAudienceSegmenterProcessor processor;
    private final ArtifactStore artifactStore;

    /** Inicializa o serviço com borda backend, processor concreto e armazenamento genérico de artefatos. */
    public MeiAudienceSegmenterService(
            MeiAudienceSegmenterBackendClient backendClient, MeiAudienceSegmenterProcessor processor, ArtifactStore artifactStore) {
        this.backendClient = backendClient;
        this.processor = processor;
        this.artifactStore = artifactStore;
    }

    /** Lista ciclos pendentes que ainda precisam de segmentação comportamental. */
    public List<MeiAudienceSegmenterPending> listPendingCycles() {
        return backendClient.listPendingCycles();
    }

    /** Processa ciclos pendentes pela etapa de segmentação e retorna perfis gravados no backend. */
    public List<MeiAudienceSegmenterOutput> processPending(String requestedBy) {
        List<MeiAudienceSegmenterPending> pendingCycles = backendClient.listPendingCycles();
        return pendingCycles.stream()
                .map(pending -> processOne(pending, requestedBy))
                .toList();
    }

    /** Executa o worker genérico para um ciclo e registra falha contextual no backend. */
    private MeiAudienceSegmenterOutput processOne(MeiAudienceSegmenterPending pending, String requestedBy) {
        StageExecution<MeiAudienceSegmenterPending> execution = new StageExecution<>(
                "oprm-mei-audience-segmenter-" + pending.researchCycleId(),
                pending,
                Map.of("stage", "oprmMeiAudienceSegmenter", "requestedBy", requestedBy));
        try {
            PipelineWorker<MeiAudienceSegmenterPending, MeiAudienceSegmenterOutput> worker = new PipelineWorker<>(processor, artifactStore);
            StageResult<MeiAudienceSegmenterOutput> result = worker.processResult(execution);
            return result.output();
        } catch (RuntimeException ex) {
            log.error(
                    "Erro ao executar segmentação MEI/autônomo OPRM nichocnae (researchCycleId={}, routineCardId={}, requestedBy={})",
                    pending.researchCycleId(),
                    pending.routineCardId(),
                    requestedBy,
                    ex);
            backendClient.failStageExecution(pending, ex);
            throw ex;
        }
    }
}

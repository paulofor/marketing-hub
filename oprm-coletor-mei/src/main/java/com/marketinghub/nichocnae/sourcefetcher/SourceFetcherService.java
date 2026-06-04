package com.marketinghub.nichocnae.sourcefetcher;

import com.marketinghub.nichocnae.pipeline.ArtifactStore;
import com.marketinghub.nichocnae.pipeline.PipelineWorker;
import com.marketinghub.nichocnae.pipeline.StageExecution;
import com.marketinghub.nichocnae.pipeline.StageResult;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Orquestra a etapa quatro do pipeline nichocnae para coletar snapshots curtos no coletor OPRM. */
@Service
public class SourceFetcherService {
    private static final Logger log = LoggerFactory.getLogger(SourceFetcherService.class);

    private final SourceFetcherBackendClient backendClient;
    private final SourceFetcherProcessor processor;
    private final ArtifactStore artifactStore;

    /** Inicializa o serviço com a borda backend, processor concreto e armazenamento genérico de artefatos. */
    public SourceFetcherService(
            SourceFetcherBackendClient backendClient, SourceFetcherProcessor processor, ArtifactStore artifactStore) {
        this.backendClient = backendClient;
        this.processor = processor;
        this.artifactStore = artifactStore;
    }

    /** Lista fontes candidatas pendentes que ainda precisam ser coletadas. */
    public List<SourceFetcherPending> listPendingSources() {
        return backendClient.listPendingSources();
    }

    /** Processa as fontes pendentes pela etapa quatro e retorna as saídas gravadas no backend. */
    public List<SourceFetcherOutput> processPending(String requestedBy) {
        List<SourceFetcherPending> pendingSources = backendClient.listPendingSources();
        return pendingSources.stream()
                .map(pending -> processOne(pending, requestedBy))
                .toList();
    }

    /** Executa o worker genérico para uma fonte da etapa quatro e registra falha contextual no backend. */
    private SourceFetcherOutput processOne(SourceFetcherPending pending, String requestedBy) {
        StageExecution<SourceFetcherPending> execution = new StageExecution<>(
                "oprm-source-fetcher-" + pending.sourceCandidateId(),
                pending,
                Map.of("stage", "oprmSourceFetcher", "requestedBy", requestedBy));
        try {
            PipelineWorker<SourceFetcherPending, SourceFetcherOutput> worker = new PipelineWorker<>(processor, artifactStore);
            StageResult<SourceFetcherOutput> result = worker.processResult(execution);
            return result.output();
        } catch (RuntimeException ex) {
            log.error(
                    "Erro ao executar etapa quatro OPRM nichocnae (sourceCandidateId={}, researchQueryId={}, researchCycleId={}, requestedBy={})",
                    pending.sourceCandidateId(),
                    pending.researchQueryId(),
                    pending.researchCycleId(),
                    requestedBy,
                    ex);
            backendClient.failStageExecution(pending, ex);
            throw ex;
        }
    }
}

package com.marketinghub.nichocnae.sourcesearcher;

import com.marketinghub.nichocnae.pipeline.ArtifactStore;
import com.marketinghub.nichocnae.pipeline.PipelineWorker;
import com.marketinghub.nichocnae.pipeline.StageExecution;
import com.marketinghub.nichocnae.pipeline.StageResult;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Orquestra a etapa três do pipeline nichocnae para buscar fontes públicas no coletor OPRM. */
@Service
public class SourceSearcherService {
    private static final Logger log = LoggerFactory.getLogger(SourceSearcherService.class);

    private final SourceSearcherBackendClient backendClient;
    private final SourceSearcherProcessor processor;
    private final ArtifactStore artifactStore;

    /** Inicializa o serviço com a borda backend, processor concreto e armazenamento genérico de artefatos. */
    public SourceSearcherService(
            SourceSearcherBackendClient backendClient, SourceSearcherProcessor processor, ArtifactStore artifactStore) {
        this.backendClient = backendClient;
        this.processor = processor;
        this.artifactStore = artifactStore;
    }

    /** Lista queries pendentes que ainda precisam passar por busca pública. */
    public List<SourceSearcherPending> listPendingQueries() {
        return backendClient.listPendingQueries();
    }

    /** Processa as queries pendentes pela etapa três e retorna as saídas gravadas no backend. */
    public List<SourceSearcherOutput> processPending(String requestedBy) {
        List<SourceSearcherPending> pendingQueries = backendClient.listPendingQueries();
        log.info(
                "Lote da etapa três OPRM nichocnae preparado para processamento (pendingCount={}, requestedBy={})",
                pendingQueries.size(),
                requestedBy);
        List<SourceSearcherOutput> outputs = pendingQueries.stream()
                .map(pending -> processOne(pending, requestedBy))
                .toList();
        log.info(
                "Lote da etapa três OPRM nichocnae processado (processedCount={}, requestedBy={})",
                outputs.size(),
                requestedBy);
        return outputs;
    }

    /** Executa o worker genérico para uma query da etapa três e registra falha contextual no backend. */
    private SourceSearcherOutput processOne(SourceSearcherPending pending, String requestedBy) {
        StageExecution<SourceSearcherPending> execution = new StageExecution<>(
                "oprm-source-searcher-" + pending.researchQueryId(),
                pending,
                Map.of("stage", "oprmSourceSearcher", "requestedBy", requestedBy));
        try {
            log.info(
                    "Iniciando query da etapa três OPRM nichocnae (researchQueryId={}, researchCycleId={}, queryText={}, requestedBy={})",
                    pending.researchQueryId(),
                    pending.researchCycleId(),
                    pending.queryText(),
                    requestedBy);
            PipelineWorker<SourceSearcherPending, SourceSearcherOutput> worker = new PipelineWorker<>(processor, artifactStore);
            StageResult<SourceSearcherOutput> result = worker.processResult(execution);
            SourceSearcherOutput output = result.output();
            log.info(
                    "Query da etapa três OPRM nichocnae concluída (researchQueryId={}, researchCycleId={}, resultCount={}, cycleTotalSourceCandidates={}, requestedBy={})",
                    output.researchQueryId(),
                    output.researchCycleId(),
                    output.resultCount(),
                    output.cycleTotalSourceCandidates(),
                    requestedBy);
            return output;
        } catch (RuntimeException ex) {
            log.error(
                    "Erro ao executar etapa três OPRM nichocnae (researchQueryId={}, researchCycleId={}, requestedBy={})",
                    pending.researchQueryId(),
                    pending.researchCycleId(),
                    requestedBy,
                    ex);
            backendClient.failStageExecution(pending, ex);
            throw ex;
        }
    }
}

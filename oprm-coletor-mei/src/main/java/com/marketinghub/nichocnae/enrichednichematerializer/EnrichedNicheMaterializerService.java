package com.marketinghub.nichocnae.enrichednichematerializer;

import com.marketinghub.nichocnae.pipeline.ArtifactStore;
import com.marketinghub.nichocnae.pipeline.PipelineWorker;
import com.marketinghub.nichocnae.pipeline.StageExecution;
import com.marketinghub.nichocnae.pipeline.StageResult;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Orquestra a etapa final do NichoCNAE para alimentar as tabelas de nicho e nicho enriquecido. */
@Service
public class EnrichedNicheMaterializerService {
    private static final Logger log = LoggerFactory.getLogger(EnrichedNicheMaterializerService.class);

    private final EnrichedNicheMaterializerBackendClient backendClient;
    private final EnrichedNicheMaterializerProcessor processor;
    private final ArtifactStore artifactStore;

    /** Inicializa o serviço com backend, processor e armazenamento genérico de artefatos. */
    public EnrichedNicheMaterializerService(
            EnrichedNicheMaterializerBackendClient backendClient,
            EnrichedNicheMaterializerProcessor processor,
            ArtifactStore artifactStore) {
        this.backendClient = backendClient;
        this.processor = processor;
        this.artifactStore = artifactStore;
    }

    /** Lista cartões aprovados aguardando materialização final. */
    public List<EnrichedNicheMaterializerPending> listPendingCards() {
        return backendClient.listPendingCards();
    }

    /** Processa todas as pendências disponíveis da etapa final. */
    public List<EnrichedNicheMaterializerOutput> processPending(String requestedBy) {
        List<EnrichedNicheMaterializerPending> pendingCards = backendClient.listPendingCards();
        return pendingCards.stream()
                .map(pending -> processOne(pending, requestedBy))
                .toList();
    }

    /** Executa o worker genérico para um cartão aprovado e registra falha contextual no backend. */
    private EnrichedNicheMaterializerOutput processOne(EnrichedNicheMaterializerPending pending, String requestedBy) {
        StageExecution<EnrichedNicheMaterializerPending> execution = new StageExecution<>(
                "oprm-enriched-niche-materializer-" + pending.researchCycleId(),
                pending,
                Map.of("stage", "oprmEnrichedNicheMaterializer", "requestedBy", requestedBy));
        try {
            PipelineWorker<EnrichedNicheMaterializerPending, EnrichedNicheMaterializerOutput> worker = new PipelineWorker<>(processor, artifactStore);
            StageResult<EnrichedNicheMaterializerOutput> result = worker.processResult(execution);
            return result.output();
        } catch (RuntimeException ex) {
            log.error("Erro ao executar etapa final OPRM nichocnae (researchCycleId={}, routineCardId={}, requestedBy={})",
                    pending.researchCycleId(), pending.routineCardId(), requestedBy, ex);
            backendClient.failStageExecution(pending, ex);
            throw ex;
        }
    }
}

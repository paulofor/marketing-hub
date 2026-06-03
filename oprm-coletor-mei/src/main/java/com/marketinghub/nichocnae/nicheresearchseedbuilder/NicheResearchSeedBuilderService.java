package com.marketinghub.nichocnae.nicheresearchseedbuilder;

import com.marketinghub.nichocnae.pipeline.ArtifactStore;
import com.marketinghub.nichocnae.pipeline.PipelineWorker;
import com.marketinghub.nichocnae.pipeline.StageExecution;
import com.marketinghub.nichocnae.pipeline.StageResult;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Orquestra a etapa dois do submódulo nichocnae para gerar seed e queries por IA. */
@Service
public class NicheResearchSeedBuilderService {
    private static final Logger log = LoggerFactory.getLogger(NicheResearchSeedBuilderService.class);

    private final NicheResearchSeedBuilderBackendClient backendClient;
    private final NicheResearchSeedBuilderProcessor processor;
    private final ArtifactStore artifactStore;

    /** Inicializa o serviço com a borda backend, processor concreto e armazenamento genérico de artefatos. */
    public NicheResearchSeedBuilderService(
            NicheResearchSeedBuilderBackendClient backendClient,
            NicheResearchSeedBuilderProcessor processor,
            ArtifactStore artifactStore) {
        this.backendClient = backendClient;
        this.processor = processor;
        this.artifactStore = artifactStore;
    }

    /** Lista ciclos em execução que ainda não possuem seed de pesquisa gerado pela etapa dois. */
    public List<NicheResearchSeedBuilderPending> listPendingSeeds() {
        return backendClient.listPendingSeeds();
    }

    /** Detalha o seed e as queries já gravados no backend para o ciclo informado. */
    public NicheResearchSeedBuilderOutput detailStageExecution(Long researchCycleId) {
        return backendClient.detailStageExecution(researchCycleId);
    }

    /** Processa os ciclos pendentes pela etapa dois e retorna os seeds gerados. */
    public List<NicheResearchSeedBuilderOutput> processPending(String requestedBy) {
        List<NicheResearchSeedBuilderPending> pendingSeeds = backendClient.listPendingSeeds();
        return pendingSeeds.stream()
                .map(pending -> processOne(pending, requestedBy))
                .toList();
    }

    /** Executa o worker genérico para uma unidade da etapa dois e registra falha contextual no backend. */
    private NicheResearchSeedBuilderOutput processOne(NicheResearchSeedBuilderPending pending, String requestedBy) {
        StageExecution<NicheResearchSeedBuilderPending> execution = new StageExecution<>(
                "oprm-niche-research-seed-builder-" + pending.researchCycleId(),
                pending,
                Map.of("stage", "oprmNicheResearchSeedBuilder", "requestedBy", requestedBy));
        try {
            PipelineWorker<NicheResearchSeedBuilderPending, NicheResearchSeedBuilderOutput> worker = new PipelineWorker<>(
                    processor, artifactStore);
            StageResult<NicheResearchSeedBuilderOutput> result = worker.processResult(execution);
            return result.output();
        } catch (RuntimeException ex) {
            log.error(
                    "Erro ao executar sob demanda a etapa dois OPRM nichocnae (researchCycleId={}, sourceNicheId={}, requestedBy={})",
                    pending.researchCycleId(),
                    pending.sourceNicheId(),
                    requestedBy,
                    ex);
            backendClient.failStageExecution(pending.researchCycleId(), ex);
            throw ex;
        }
    }
}

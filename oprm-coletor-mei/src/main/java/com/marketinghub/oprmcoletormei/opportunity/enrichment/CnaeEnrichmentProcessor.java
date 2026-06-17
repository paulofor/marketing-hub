package com.marketinghub.oprmcoletormei.opportunity.enrichment;

import com.marketinghub.oprmcoletormei.opportunity.dto.OprmCnaeEnrichmentRequestDto;
import com.marketinghub.oprmcoletormei.opportunity.dto.OprmCnaeOpportunityScoreResponseDto;
import com.marketinghub.oprmcoletormei.opportunity.pipeline.StageArtifact;
import com.marketinghub.oprmcoletormei.opportunity.pipeline.StageContext;
import com.marketinghub.oprmcoletormei.opportunity.pipeline.StageProcessor;
import com.marketinghub.oprmcoletormei.opportunity.pipeline.StageResult;
import com.marketinghub.oprmcoletormei.opportunity.service.OprmCnaeRoutineSignalBuilder;
import java.util.List;
import java.util.Map;

/** Etapa concreta responsável por enriquecer CNAE priorizado em sinais comerciais e candidato de nicho. */
public class CnaeEnrichmentProcessor implements StageProcessor<CnaeEnrichmentInput, CnaeEnrichmentOutput> {
    private final OprmCnaeRoutineSignalBuilder routineSignalBuilder;

    /** Inicializa o processor com o builder determinístico de sinais de rotina. */
    public CnaeEnrichmentProcessor(OprmCnaeRoutineSignalBuilder routineSignalBuilder) {
        this.routineSignalBuilder = routineSignalBuilder;
    }

    /** Converte o score priorizado em enriquecimento auditável para gravação no backend. */
    @Override
    public StageResult<CnaeEnrichmentOutput> process(StageContext<CnaeEnrichmentInput> context) {
        OprmCnaeOpportunityScoreResponseDto score = context.input().score();
        OprmCnaeEnrichmentRequestDto enrichment = routineSignalBuilder.buildEnrichment(score, context.cycleId());
        StageArtifact artifact = new StageArtifact(
                "NORMALIZED_JSON",
                "cnae-enrichment-" + score.cnaeCode(),
                "application/json",
                "opportunity/enrichment/" + context.cycleId() + "/" + score.cnaeCode(),
                null,
                Map.of("cnaeCode", score.cnaeCode(), "scoreCycleId", score.cycleId()));
        return new StageResult<>(
                new CnaeEnrichmentOutput(score.cnaeCode(), enrichment),
                List.of(context.artifactStore().store(artifact)),
                Map.of("candidateCount", enrichment.candidates().size(), "stageName", context.stageName()));
    }
}

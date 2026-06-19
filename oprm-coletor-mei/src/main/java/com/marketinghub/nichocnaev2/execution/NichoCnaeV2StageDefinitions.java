package com.marketinghub.nichocnaev2.execution;

import com.marketinghub.nichocnaev2.pipeline.adaptivequeryplanner.AdaptiveQueryPlannerProcessor;
import com.marketinghub.nichocnaev2.pipeline.candidategenerator.CandidateGeneratorProcessor;
import com.marketinghub.nichocnaev2.pipeline.candidatetournament.CandidateTournamentProcessor;
import com.marketinghub.nichocnaev2.pipeline.commercialevidencegate.CommercialEvidenceGateProcessor;
import com.marketinghub.nichocnaev2.pipeline.enrichednichematerializer.EnrichedNicheMaterializerProcessor;
import com.marketinghub.nichocnaev2.pipeline.knowledgeaccumulator.KnowledgeAccumulatorProcessor;
import com.marketinghub.nichocnaev2.pipeline.reprocesscontroller.ReprocessControllerProcessor;
import com.marketinghub.nichocnaev2.pipeline.sourcefetcherreranker.SourceFetcherRerankerProcessor;
import com.marketinghub.nichocnaev2.pipeline.sourcesafetyfilter.SourceSafetyFilterProcessor;
import java.util.List;
import org.springframework.stereotype.Component;

/** Fornece a lista canônica de etapas NichoCNAE v2 que devem consultar pendências no backend. */
@Component
public class NichoCnaeV2StageDefinitions {
    private final List<NichoCnaeV2StageDefinition> stages = List.of(
            stage("candidate-generator", new CandidateGeneratorProcessor()),
            stage("source-safety-filter", new SourceSafetyFilterProcessor()),
            stage("adaptive-query-planner", new AdaptiveQueryPlannerProcessor()),
            stage("candidate-tournament", new CandidateTournamentProcessor()),
            stage("source-fetcher-reranker", new SourceFetcherRerankerProcessor()),
            stage("knowledge-accumulator", new KnowledgeAccumulatorProcessor()),
            stage("commercial-evidence-gate", new CommercialEvidenceGateProcessor()),
            stage("reprocess-controller", new ReprocessControllerProcessor()),
            stage("enriched-niche-materializer", new EnrichedNicheMaterializerProcessor()));

    /** Retorna todas as etapas registradas para varredura periódica de pendências. */
    public List<NichoCnaeV2StageDefinition> all() {
        return stages;
    }

    /** Cria a definição padronizada com o endpoint interno de stage-executions da etapa. */
    private static NichoCnaeV2StageDefinition stage(String stageCode, com.marketinghub.nichocnaev2.pipeline.StageProcessor processor) {
        return new NichoCnaeV2StageDefinition(
                stageCode,
                "/api/internal/oprm/nichocnae/v2/" + stageCode + "/stage-executions",
                processor);
    }
}

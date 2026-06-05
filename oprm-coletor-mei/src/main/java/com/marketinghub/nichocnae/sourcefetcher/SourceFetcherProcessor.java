package com.marketinghub.nichocnae.sourcefetcher;

import com.marketinghub.nichocnae.pipeline.StageContext;
import com.marketinghub.nichocnae.pipeline.StageProcessor;
import com.marketinghub.nichocnae.pipeline.StageResult;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Processa a etapa quatro coletando snapshots curtos de fontes candidatas e persistindo no backend. */
@Component
public class SourceFetcherProcessor implements StageProcessor<SourceFetcherPending, SourceFetcherOutput> {
    private final PublicSourceFetcher sourceFetcher;
    private final SourceFetcherBackendClient backendClient;

    /** Inicializa o processor com o coletor de fontes públicas e a borda backend da etapa quatro. */
    public SourceFetcherProcessor(PublicSourceFetcher sourceFetcher, SourceFetcherBackendClient backendClient) {
        this.sourceFetcher = sourceFetcher;
        this.backendClient = backendClient;
    }

    /** Coleta uma fonte pendente, monta snapshot curto e conclui a etapa quatro no backend. */
    @Override
    public StageResult<SourceFetcherOutput> process(StageContext<SourceFetcherPending> context) {
        SourceFetcherPending input = context.input();
        FetchedSourceSnapshot snapshot = sourceFetcher.fetch(input);
        SourceFetcherOutput output = backendClient.completeStageExecution(input, snapshot);
        Map<String, Object> metrics = Map.of(
                "sourceCandidateId", output.sourceCandidateId(),
                "researchCycleId", output.researchCycleId(),
                "sourceIntent", snapshot.sourceIntent(),
                "routineEvidenceScore", snapshot.routineEvidenceScore() == null ? 0 : snapshot.routineEvidenceScore(),
                "commercialPageRisk", Boolean.TRUE.equals(snapshot.commercialPageRisk()),
                "solutionLanguageRisk", Boolean.TRUE.equals(snapshot.solutionLanguageRisk()),
                "httpStatus", snapshot.httpStatus() == null ? 0 : snapshot.httpStatus(),
                "cycleTotalSourceSnapshots", output.cycleTotalSourceSnapshots() == null ? 0 : output.cycleTotalSourceSnapshots());
        return new StageResult<>(output, List.of(), metrics);
    }
}

package com.marketinghub.nichocnae.routineresearchcycle;

import com.marketinghub.nichocnae.pipeline.StageContext;
import com.marketinghub.nichocnae.pipeline.StageProcessor;
import com.marketinghub.nichocnae.pipeline.StageResult;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Processa a etapa um confirmando no backend o ciclo de pesquisa de rotina que seguirá para a próxima etapa. */
@Component
public class RoutineResearchCycleProcessor implements StageProcessor<RoutineResearchCyclePending, RoutineResearchCycleDetail> {
    private final RoutineResearchCycleBackendClient backendClient;

    /** Inicializa o processor com o cliente backend específico da etapa um. */
    public RoutineResearchCycleProcessor(RoutineResearchCycleBackendClient backendClient) {
        this.backendClient = backendClient;
    }

    /** Detalha o ciclo em execução e devolve métricas estruturadas para auditoria da etapa um. */
    @Override
    public StageResult<RoutineResearchCycleDetail> process(StageContext<RoutineResearchCyclePending> context) {
        RoutineResearchCyclePending input = context.input();
        RoutineResearchCycleDetail output = backendClient.detailStageExecution(input.researchCycleId());
        Map<String, Object> metrics = Map.of(
                "researchCycleId", output.researchCycleId(),
                "status", output.status(),
                "originalNicheName", output.originalNicheName(),
                "neutralNicheName", output.neutralNicheName(),
                "researchMode", output.researchMode(),
                "solutionLanguageRiskScore", output.solutionLanguageRiskScore(),
                "totalQueries", output.totalQueries(),
                "totalSourceCandidates", output.totalSourceCandidates(),
                "totalSourceSnapshots", output.totalSourceSnapshots(),
                "totalExtractedSignals", output.totalExtractedSignals());
        return new StageResult<>(output, List.of(), metrics);
    }
}

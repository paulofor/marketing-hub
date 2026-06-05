package com.marketinghub.nichocnae.routinequalitygate;

import com.marketinghub.nichocnae.pipeline.StageContext;
import com.marketinghub.nichocnae.pipeline.StageProcessor;
import com.marketinghub.nichocnae.pipeline.StageResult;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Processa a etapa sete avaliando qualidade do cartão e persistindo a decisão no backend. */
@Component
public class RoutineQualityGateProcessor implements StageProcessor<RoutineQualityGatePending, RoutineQualityGateOutput> {
    private final RoutineQualityGateEngine engine;
    private final RoutineQualityGateBackendClient backendClient;

    /** Inicializa o processor com o avaliador determinístico e a borda backend da etapa sete. */
    public RoutineQualityGateProcessor(RoutineQualityGateEngine engine, RoutineQualityGateBackendClient backendClient) {
        this.engine = engine;
        this.backendClient = backendClient;
    }

    /** Avalia um cartão pendente e conclui a etapa sete no backend. */
    @Override
    public StageResult<RoutineQualityGateOutput> process(StageContext<RoutineQualityGatePending> context) {
        RoutineQualityGatePending input = context.input();
        RoutineQualityDecision decision = engine.evaluate(input);
        RoutineQualityGateOutput output = backendClient.completeStageExecution(input, decision);
        Map<String, Object> metrics = Map.of(
                "routineCardId", output.routineCardId(),
                "researchCycleId", output.researchCycleId(),
                "qualityStatus", output.qualityStatus(),
                "readyForHypothesis", Boolean.TRUE.equals(output.readyForHypothesis()));
        return new StageResult<>(output, List.of(), metrics);
    }
}

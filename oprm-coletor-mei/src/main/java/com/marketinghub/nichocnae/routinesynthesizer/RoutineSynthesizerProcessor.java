package com.marketinghub.nichocnae.routinesynthesizer;

import com.marketinghub.nichocnae.pipeline.StageContext;
import com.marketinghub.nichocnae.pipeline.StageProcessor;
import com.marketinghub.nichocnae.pipeline.StageResult;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Processa a etapa seis sintetizando o cartão de rotina e persistindo o resultado no backend. */
@Component
public class RoutineSynthesizerProcessor implements StageProcessor<RoutineSynthesizerPending, RoutineSynthesizerOutput> {
    private final RoutineSynthesizerEngine engine;
    private final RoutineSynthesizerBackendClient backendClient;

    /** Inicializa o processor com o sintetizador local e a borda backend da etapa seis. */
    public RoutineSynthesizerProcessor(RoutineSynthesizerEngine engine, RoutineSynthesizerBackendClient backendClient) {
        this.engine = engine;
        this.backendClient = backendClient;
    }

    /** Sintetiza o cartão de rotina de um ciclo pendente e conclui a etapa seis no backend. */
    @Override
    public StageResult<RoutineSynthesizerOutput> process(StageContext<RoutineSynthesizerPending> context) {
        RoutineSynthesizerPending input = context.input();
        RoutineCardDraft draft = engine.synthesize(input);
        RoutineSynthesizerOutput output = backendClient.completeStageExecution(input, draft);
        Map<String, Object> metrics = Map.of(
                "routineCardId", output.routineCardId(),
                "researchCycleId", output.researchCycleId(),
                "confidenceScore", output.confidenceScore() == null ? 0 : output.confidenceScore());
        return new StageResult<>(output, List.of(), metrics);
    }
}

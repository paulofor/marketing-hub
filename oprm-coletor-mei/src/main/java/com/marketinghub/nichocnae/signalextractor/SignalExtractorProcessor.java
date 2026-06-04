package com.marketinghub.nichocnae.signalextractor;

import com.marketinghub.nichocnae.pipeline.StageContext;
import com.marketinghub.nichocnae.pipeline.StageProcessor;
import com.marketinghub.nichocnae.pipeline.StageResult;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Processa a etapa cinco extraindo sinais estruturados de snapshots curtos e persistindo no backend. */
@Component
public class SignalExtractorProcessor implements StageProcessor<SignalExtractorPending, SignalExtractorOutput> {
    private final SignalExtractorEngine signalExtractorEngine;
    private final SignalExtractorBackendClient backendClient;

    /** Inicializa o processor com o extrator local de sinais e a borda backend da etapa cinco. */
    public SignalExtractorProcessor(SignalExtractorEngine signalExtractorEngine, SignalExtractorBackendClient backendClient) {
        this.signalExtractorEngine = signalExtractorEngine;
        this.backendClient = backendClient;
    }

    /** Extrai sinais de um snapshot pendente e conclui a etapa cinco no backend. */
    @Override
    public StageResult<SignalExtractorOutput> process(StageContext<SignalExtractorPending> context) {
        SignalExtractorPending input = context.input();
        List<ExtractedSignal> signals = signalExtractorEngine.extract(input);
        SignalExtractorOutput output = backendClient.completeStageExecution(input, signals);
        Map<String, Object> metrics = Map.of(
                "sourceSnapshotId", output.sourceSnapshotId(),
                "researchCycleId", output.researchCycleId(),
                "extractedSignalCount", output.extractedSignalCount() == null ? 0 : output.extractedSignalCount(),
                "cycleTotalExtractedSignals", output.cycleTotalExtractedSignals() == null ? 0 : output.cycleTotalExtractedSignals());
        return new StageResult<>(output, List.of(), metrics);
    }
}

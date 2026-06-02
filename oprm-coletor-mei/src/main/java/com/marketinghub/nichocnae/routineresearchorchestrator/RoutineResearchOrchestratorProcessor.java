package com.marketinghub.nichocnae.routineresearchorchestrator;

import com.marketinghub.nichocnae.pipeline.StageContext;
import com.marketinghub.nichocnae.pipeline.StageProcessor;
import com.marketinghub.nichocnae.pipeline.StageResult;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Processa a etapa zero acionando o backend para selecionar o próximo nicho e criar o ciclo de rotina. */
@Component
public class RoutineResearchOrchestratorProcessor implements StageProcessor<RoutineResearchOrchestratorInput, RoutineResearchOrchestratorOutput> {
    private final RoutineResearchOrchestratorBackendClient backendClient;

    /** Inicializa o processor com o cliente backend específico da etapa zero. */
    public RoutineResearchOrchestratorProcessor(RoutineResearchOrchestratorBackendClient backendClient) {
        this.backendClient = backendClient;
    }

    /** Executa o disparo atômico no backend e devolve métricas simples de início do ciclo. */
    @Override
    public StageResult<RoutineResearchOrchestratorOutput> process(StageContext<RoutineResearchOrchestratorInput> context) {
        RoutineResearchOrchestratorOutput output = backendClient.runNext();
        Map<String, Object> metrics = Map.of("started", output.started());
        return new StageResult<>(output, List.of(), metrics);
    }
}

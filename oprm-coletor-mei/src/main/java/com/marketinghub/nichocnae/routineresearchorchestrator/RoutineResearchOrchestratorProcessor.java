package com.marketinghub.nichocnae.routineresearchorchestrator;

import com.marketinghub.nichocnae.pipeline.StageContext;
import com.marketinghub.nichocnae.pipeline.StageProcessor;
import com.marketinghub.nichocnae.pipeline.StageResult;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Processa a etapa zero acionando o backend para selecionar o próximo nicho e criar o ciclo de rotina. */
@Component
public class RoutineResearchOrchestratorProcessor implements StageProcessor<RoutineResearchOrchestratorInput, RoutineResearchOrchestratorOutput> {
    private static final Logger log = LoggerFactory.getLogger(RoutineResearchOrchestratorProcessor.class);

    private final RoutineResearchOrchestratorBackendClient backendClient;

    /** Inicializa o processor com o cliente backend específico da etapa zero. */
    public RoutineResearchOrchestratorProcessor(RoutineResearchOrchestratorBackendClient backendClient) {
        this.backendClient = backendClient;
    }

    /** Executa o disparo atômico no backend e devolve métricas simples de início do ciclo. */
    @Override
    public StageResult<RoutineResearchOrchestratorOutput> process(StageContext<RoutineResearchOrchestratorInput> context) {
        log.info(
                "Processor da etapa zero OPRM nichocnae chamando backend (executionId={}, requestedBy={})",
                context.execution().idJob(),
                context.input().requestedBy());
        RoutineResearchOrchestratorOutput output = backendClient.runNext();
        Map<String, Object> metrics = Map.of("started", output.started());
        log.info(
                "Processor da etapa zero OPRM nichocnae recebeu resposta do backend (executionId={}, requestedBy={}, started={}, researchCycleId={}, sourceNicheId={}, routineStatus={}, message={})",
                context.execution().idJob(),
                context.input().requestedBy(),
                output.started(),
                output.researchCycleId(),
                output.sourceNicheId(),
                output.routineResearchStatus(),
                output.message());
        return new StageResult<>(output, List.of(), metrics);
    }
}

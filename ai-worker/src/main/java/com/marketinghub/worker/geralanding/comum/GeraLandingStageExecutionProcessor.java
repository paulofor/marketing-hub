package com.marketinghub.worker.geralanding.comum;

import java.util.List;

/** Define o contrato de processamento de execuções de etapas do GeraLanding. */
public interface GeraLandingStageExecutionProcessor {
    /** Processa as execuções recebidas. */
    void processExecutions(List<GeraLandingStageExecutionRef> executions);
}

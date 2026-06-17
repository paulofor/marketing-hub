package com.marketinghub.nichocnae.routinequalitygate;

import java.util.Set;
import org.springframework.stereotype.Component;

/** Decide no módulo externo quando uma reprovação de qualidade deve abrir novo ciclo de pesquisa. */
@Component
public class RoutineQualityReprocessPolicy {
    private static final Set<String> RECOVERABLE_STATUSES = Set.of(
            "NEEDS_MORE_RESEARCH",
            "NEEDS_MORE_MEI_RESEARCH",
            "OUTDATED_SOURCES",
            "TOO_CORPORATE",
            "SOLUTION_CONTAMINATED",
            "GENERIC",
            "NEEDS_EXECUTOR_ROUTINE_EVIDENCE");

    /** Informa se a decisão do gate deve pedir ao backend a gravação de um novo ciclo automático. */
    public boolean shouldReprocess(RoutineQualityGateOutput output) {
        return output != null
                && !Boolean.TRUE.equals(output.readyForHypothesis())
                && RECOVERABLE_STATUSES.contains(output.qualityStatus());
    }
}

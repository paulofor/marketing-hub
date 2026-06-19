package com.marketinghub.nichocnaev2.pipeline.routinesynthesizer;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.nichocnaev2.pipeline.StageContext;
import com.marketinghub.nichocnaev2.pipeline.StageResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Valida a etapa 10 de síntese de rotina do pipeline NichoCNAE v2. */
class RoutineSynthesizerProcessorTest {
    /** Garante que a síntese usa somente claims aceitos com trecho exato e mantém IDs de evidência. */
    @Test
    void shouldSynthesizeRoutineOnlyFromAcceptedClaimsWithExactEvidenceSpan() {
        RoutineSynthesizerProcessor processor = new RoutineSynthesizerProcessor();

        StageResult result = processor.process(new StageContext("job-10", "stage-10", Map.of(
                "executor", "Motoristas autônomos de transfer aeroportuário",
                "jobContext", "Corridas previamente agendadas",
                "claims", List.of(
                        claim(101, "ROUTINE_TASK", "Confirma horários de chegada dos passageiros", "a.com"),
                        claim(102, "OPERATIONAL_FAILURE", "Cancelamento tardio deixa horário sem reposição", "b.com"),
                        claim(103, "DIRECT_COST", "Deslocamento até aeroporto pode virar custo perdido", "c.com"),
                        Map.of(
                                "claimId", 104,
                                "claimType", "PURCHASE_SIGNAL",
                                "claimText", "Texto sem trecho não deve entrar",
                                "status", "ACCEPTED",
                                "canonicalDomain", "d.com")))));

        assertThat(result.status()).isEqualTo("ROUTINE_SYNTHESIS_WITH_GAPS");
        Map<String, Object> synthesis = (Map<String, Object>) result.output().get("synthesis");
        assertThat((List<?>) synthesis.get("routineTasks")).hasSize(1);
        assertThat((List<?>) synthesis.get("practicalPains")).hasSize(1);
        assertThat((List<?>) synthesis.get("economicSignals")).hasSize(1);
        assertThat((List<?>) synthesis.get("acquisitionSignals")).isEmpty();
        assertThat((List<Object>) synthesis.get("supportingClaimIds")).containsExactly(101, 102, 103);
        assertThat(result.output()).containsEntry("nextStageCode", "commercial-evidence-gate");
    }

    /** Garante que falta de evidência vira limite explícito em vez de texto genérico inventado. */
    @Test
    void shouldDeclareEvidenceLimitsInsteadOfInventingSynthesis() {
        RoutineSynthesizerProcessor processor = new RoutineSynthesizerProcessor();

        StageResult result = processor.process(new StageContext("job-11", "stage-10", Map.of(
                "claims", List.of(Map.of(
                        "claimId", 201,
                        "claimType", "ROUTINE_TASK",
                        "claimText", "Texto sem trecho exato",
                        "status", "ACCEPTED")))));

        assertThat(result.status()).isEqualTo("ROUTINE_SYNTHESIS_WITH_GAPS");
        Map<String, Object> synthesis = (Map<String, Object>) result.output().get("synthesis");
        assertThat((List<?>) synthesis.get("routineTasks")).isEmpty();
        assertThat((List<String>) synthesis.get("evidenceLimits"))
                .contains("Sem tarefas concretas validadas do executor para sintetizar rotina.");
    }

    private Map<String, Object> claim(int id, String type, String text, String domain) {
        return Map.of(
                "claimId", id,
                "claimType", type,
                "claimText", text,
                "status", "ACCEPTED",
                "epistemicState", "VALIDATED",
                "exactEvidenceSpan", text,
                "sourceUrl", "https://" + domain + "/fonte",
                "canonicalDomain", domain);
    }
}

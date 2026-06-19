package com.marketinghub.nichocnaev2.pipeline.reprocesscontroller;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.nichocnaev2.pipeline.StageContext;
import com.marketinghub.nichocnaev2.pipeline.StageResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Valida a etapa 9 de controle de reprocessamento do pipeline NichoCNAE v2. */
class ReprocessControllerProcessorTest {
    /** Garante que falha de infraestrutura vira retry técnico sem mudar versão de conhecimento. */
    @Test
    void shouldPlanTechnicalRetryWithoutChangingKnowledgeVersion() {
        ReprocessControllerProcessor processor = new ReprocessControllerProcessor();
        StageResult result = processor.process(new StageContext("job-72", "stage-9", Map.of(
                "researchCycleId", 72,
                "stageCode", "commercial-evidence-gate",
                "failureType", "INFRASTRUCTURE",
                "reasonCode", "HTTP_503",
                "attemptNumber", 2,
                "technicalRetryNumber", 1,
                "knowledgeVersion", 4)));

        assertThat(result.status()).isEqualTo("TECHNICAL_RETRY_PLANNED");
        Map<String, Object> plan = (Map<String, Object>) result.output().get("reprocessPlan");
        assertThat(plan).containsEntry("executionMode", "TECHNICAL_RETRY");
        assertThat(plan).containsEntry("rewindToStage", "commercial-evidence-gate");
        assertThat(plan).containsEntry("knowledgeVersion", 4);
        assertThat(plan).containsEntry("technicalRetryNumber", 2);
    }

    /** Garante que falta cognitiva volta ao planejador adaptativo com gaps e nova versão de conhecimento. */
    @Test
    void shouldPlanCognitiveReprocessToAdaptivePlannerWithNewKnowledgeVersion() {
        ReprocessControllerProcessor processor = new ReprocessControllerProcessor();
        StageResult result = processor.process(new StageContext("job-72", "stage-9", Map.of(
                "researchCycleId", 72,
                "candidateId", 31,
                "gateDecision", "NEEDS_MORE_RESEARCH",
                "reasonCode", "MISSING_EXECUTOR_ROUTINE_EVIDENCE",
                "evidenceGaps", List.of("CONCRETE_EXECUTOR_TASKS"),
                "attemptNumber", 1,
                "knowledgeVersion", 4,
                "informationGain", 0.25)));

        assertThat(result.status()).isEqualTo("COGNITIVE_REPROCESS_PLANNED");
        Map<String, Object> plan = (Map<String, Object>) result.output().get("reprocessPlan");
        assertThat(plan).containsEntry("executionMode", "COGNITIVE_REPROCESS");
        assertThat(plan).containsEntry("rewindToStage", "adaptive-query-planner");
        assertThat(plan).containsEntry("knowledgeVersionTo", 5);
        assertThat((List<String>) plan.get("newEvidenceGaps")).contains("CONCRETE_EXECUTOR_TASKS");
    }

    /** Garante encerramento seguro quando não há ganho informacional após o limite de tentativas. */
    @Test
    void shouldStopWhenCognitiveAttemptsHaveNoInformationGain() {
        ReprocessControllerProcessor processor = new ReprocessControllerProcessor();
        StageResult result = processor.process(new StageContext("job-72", "stage-9", Map.of(
                "gateDecision", "NO_PUBLIC_EVIDENCE",
                "attemptNumber", 3,
                "maxCognitiveAttempts", 3,
                "knowledgeVersion", 6,
                "informationGain", 0.02)));

        assertThat(result.status()).isEqualTo("NO_INFORMATION_GAIN");
        assertThat(result.output()).containsEntry("nextStageCode", "end");
    }
}

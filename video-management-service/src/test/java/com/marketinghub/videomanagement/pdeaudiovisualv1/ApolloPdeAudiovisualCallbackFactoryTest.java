package com.marketinghub.videomanagement.pdeaudiovisualv1;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar auditoria, custo zero e orientação dos callbacks audiovisuais. */
class ApolloPdeAudiovisualCallbackFactoryTest {
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final ApolloPdeAudiovisualCallbackFactory factory =
            new ApolloPdeAudiovisualCallbackFactory(objectMapper);

    /** Persiste ausência de audiovisual sem artefato, modelo, provider ou próxima etapa local. */
    @Test
    void shouldBuildDeterministicZeroCostCompletion() throws Exception {
        ApolloPdeAudiovisualTask task = task(false);
        ApolloPdeAudiovisualDecision decision =
                new ApolloPdeAudiovisualRequirementEvaluator().evaluate(task);

        Map<String, Object> payload = factory.complete(task, decision);
        JsonNode result = objectMapper.readTree((String) payload.get("resultJson"));
        JsonNode evidence = objectMapper.readTree((String) payload.get("evidenceJson"));
        @SuppressWarnings("unchecked")
        Map<String, Object> audit = (Map<String, Object>) payload.get("executionAudit");

        assertThat(result.path("decision").asText()).isEqualTo("READY");
        assertThat(result.path("audiovisualRequirement").asText()).isEqualTo("NOT_REQUIRED");
        assertThat(result.path("artifactIds")).isEmpty();
        assertThat(result.path("providerCalls").asInt()).isZero();
        assertThat(result.path("creditsConsumed").asInt()).isZero();
        assertThat(result.has("nextStageCode")).isFalse();
        assertThat(evidence.path("contractValue").asBoolean()).isFalse();
        assertThat(evidence.path("productId").asLong()).isEqualTo(10L);
        assertThat(payload.get("modelUsages")).isEqualTo(List.of());
        assertThat(audit)
                .containsEntry("executionMode", "DETERMINISTIC")
                .containsEntry("reasoningEffort", "NOT_APPLICABLE")
                .containsEntry("modelCode", ApolloPdeAudiovisualCallbackFactory.RULE_VERSION);
        assertThat((String) audit.get("promptSent"))
                .contains("\"taskId\":336", "\"audiovisualRequired\":false");
    }

    /** Preserva ação e link de ajuda quando vídeo obrigatório ainda não possui autorização. */
    @Test
    void shouldBuildGovernedBlockWithoutModelUsage() throws Exception {
        ApolloPdeAudiovisualTask task = task(true);
        ApolloPdeAudiovisualDecision decision =
                new ApolloPdeAudiovisualRequirementEvaluator().evaluate(task);

        Map<String, Object> payload = factory.block(task, decision);
        JsonNode result = objectMapper.readTree((String) payload.get("resultJson"));
        @SuppressWarnings("unchecked")
        Map<String, Object> blocker = (Map<String, Object>) payload.get("blockerGuidance");

        assertThat(result.path("decision").asText()).isEqualTo("BLOCKED");
        assertThat(result.path("providerCalls").asInt()).isZero();
        assertThat(blocker)
                .containsEntry("category", "AUTHORIZATION_REQUIRED")
                .containsKey("recommendedAction");
        assertThat(blocker.get("helpLinks").toString()).contains("/products/10/value-chain-history");
        assertThat(payload.get("modelUsages")).isEqualTo(List.of());
    }

    /** Não afirma necessidade de vídeo quando o próprio contrato canônico está ausente. */
    @Test
    void shouldKeepMissingRequirementAsNull() throws Exception {
        ApolloPdeAudiovisualTask complete = task(false);
        ApolloPdeAudiovisualTask missing = new ApolloPdeAudiovisualTask(
                complete.taskId(),
                complete.agentKey(),
                complete.processCode(),
                complete.processVersion(),
                complete.activityId(),
                complete.activityName(),
                complete.title(),
                complete.description(),
                complete.sourceReference(),
                complete.receivedAt(),
                complete.executionResource(),
                new ApolloPdeAudiovisualTask.TaskTarget(
                        complete.taskTarget().sourceReference(),
                        null,
                        10L,
                        "mira",
                        "Mira",
                        "Mira",
                        "private-validation-v1",
                        null,
                        objectMapper.createObjectNode()),
                complete.processContextJson());
        ApolloPdeAudiovisualDecision decision =
                new ApolloPdeAudiovisualRequirementEvaluator().evaluate(missing);

        JsonNode result = objectMapper.readTree((String) factory.block(missing, decision).get("resultJson"));

        assertThat(result.path("audiovisualRequired").isNull()).isTrue();
    }

    /** Cria a tarefa usada para provar os dois valores do contrato. */
    private ApolloPdeAudiovisualTask task(boolean required) {
        JsonNode context = objectMapper.createObjectNode()
                .set("harness", objectMapper.createObjectNode().put("audiovisualRequired", required));
        return new ApolloPdeAudiovisualTask(
                336L,
                "videomaker",
                "pde-construction-approval",
                6,
                "audiovisual",
                "Produzir audiovisual quando previsto",
                "Audiovisual",
                "Contrato opcional",
                "product:10@private-validation-v1",
                null,
                new ApolloPdeAudiovisualTask.ExecutionResource(
                        "video-management-service", "Estúdio", "MODULE", null, null),
                new ApolloPdeAudiovisualTask.TaskTarget(
                        "product:10@private-validation-v1",
                        null,
                        10L,
                        "mira",
                        "Mira",
                        "Mira",
                        "private-validation-v1",
                        null,
                        context),
                "{}");
    }
}

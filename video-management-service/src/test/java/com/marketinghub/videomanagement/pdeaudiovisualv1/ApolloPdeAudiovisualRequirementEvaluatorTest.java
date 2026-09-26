package com.marketinghub.videomanagement.pdeaudiovisualv1;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger a decisão audiovisual contra inferência e consumo desnecessário. */
class ApolloPdeAudiovisualRequirementEvaluatorTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ApolloPdeAudiovisualRequirementEvaluator evaluator =
            new ApolloPdeAudiovisualRequirementEvaluator();

    /** Faz o booleano falso prevalecer mesmo diante de componente técnico classificado como áudio. */
    @Test
    void shouldCompleteWithoutAudiovisualWhenCanonicalContractIsFalse() throws Exception {
        JsonNode context = objectMapper.readTree("""
                {
                  "harness": {"audiovisualRequired": false},
                  "deliveryPackage": {"assets": [{"format": "AUDIO"}]}
                }
                """);

        ApolloPdeAudiovisualDecision decision = evaluator.evaluate(task(context));

        assertThat(decision.outcome()).isEqualTo(ApolloPdeAudiovisualDecision.Outcome.NOT_REQUIRED);
        assertThat(decision.canComplete()).isTrue();
        assertThat(decision.blockerCategory()).isNull();
    }

    /** Exige autorização governada quando o contrato determina produção audiovisual. */
    @Test
    void shouldBlockRequiredAudiovisualBeforeAnyPaidProduction() throws Exception {
        JsonNode context = objectMapper.readTree("""
                {"harness": {"audiovisualRequired": true}}
                """);

        ApolloPdeAudiovisualDecision decision = evaluator.evaluate(task(context));

        assertThat(decision.outcome())
                .isEqualTo(ApolloPdeAudiovisualDecision.Outcome.REQUIRES_AUTHORIZATION);
        assertThat(decision.canComplete()).isFalse();
        assertThat(decision.blockerCategory()).isEqualTo("AUTHORIZATION_REQUIRED");
        assertThat(decision.recommendedAction()).contains("Estúdio", "Plutus", "autorização humana");
    }

    /** Usa a decisão de formatos da comunicação sem confundi-la com o harness do produto. */
    @Test
    void shouldUseCommunicationDecisionForCreativeProduction() throws Exception {
        JsonNode context = objectMapper.readTree("""
                {
                  "harness": {"audiovisualRequired": false},
                  "communicationMaterialization": {"audiovisualRequired": true}
                }
                """);
        ApolloPdeAudiovisualTask base = task(context);
        ApolloPdeAudiovisualTask creative = new ApolloPdeAudiovisualTask(
                504L,
                base.agentKey(),
                "creative-production-approval",
                8,
                base.activityId(),
                base.activityName(),
                base.title(),
                base.description(),
                "experiment:93",
                base.receivedAt(),
                base.executionResource(),
                base.taskTarget(),
                null);

        ApolloPdeAudiovisualDecision decision = evaluator.evaluate(creative);

        assertThat(decision.outcome())
                .isEqualTo(ApolloPdeAudiovisualDecision.Outcome.REQUIRES_AUTHORIZATION);
    }

    /** Conclui a atividade após o Estúdio devolver o ativo autorizado dentro do teto. */
    @Test
    void shouldCompleteRequiredAudiovisualAfterGovernedMaterialization() throws Exception {
        JsonNode context = objectMapper.readTree("""
                {
                  "communicationMaterialization": {"audiovisualRequired": true},
                  "audiovisualMaterialization": {
                    "contractVersion":"APOLLO_COMMUNICATION_AUDIOVISUAL_MATERIALIZATION_V1",
                    "sourceReference":"experiment:93",
                    "videoProductionCycleId":51,
                    "videoProjectId":61,
                    "salesVideoJobId":71,
                    "status":"VIDEO_READY_FOR_REVIEW",
                    "financialDecision":"APPROVED",
                    "authorizedBy":"time@marketinghub.io",
                    "budgetLimitUsd":15.38,
                    "actualCostUsd":0.42,
                    "publicationAuthorized":false,
                    "spendAuthorized":true,
                    "artifactIds":[81,82]
                  }
                }
                """);
        ApolloPdeAudiovisualTask base = task(context);
        ApolloPdeAudiovisualTask creative = new ApolloPdeAudiovisualTask(
                505L, base.agentKey(), "creative-production-approval", 8,
                base.activityId(), base.activityName(), base.title(), base.description(),
                "experiment:93", base.receivedAt(), base.executionResource(),
                new ApolloPdeAudiovisualTask.TaskTarget(
                        "experiment:93", 93L, 10L, "mira", "Mira", "Mira",
                        "mira-private-v3", null, context),
                null);

        ApolloPdeAudiovisualDecision decision = evaluator.evaluate(creative);

        assertThat(decision.outcome()).isEqualTo(ApolloPdeAudiovisualDecision.Outcome.MATERIALIZED);
        assertThat(decision.canComplete()).isTrue();
    }

    /** Rejeita campo ausente ou textual sem converter ambiguidade em decisão. */
    @Test
    void shouldBlockMissingOrNonBooleanContract() throws Exception {
        ApolloPdeAudiovisualDecision missing = evaluator.evaluate(task(objectMapper.readTree("{}")));
        ApolloPdeAudiovisualDecision textual = evaluator.evaluate(task(objectMapper.readTree("""
                {"harness": {"audiovisualRequired": "false"}}
                """)));

        assertThat(missing.outcome()).isEqualTo(ApolloPdeAudiovisualDecision.Outcome.MISSING_CONTRACT);
        assertThat(textual.outcome()).isEqualTo(ApolloPdeAudiovisualDecision.Outcome.MISSING_CONTRACT);
        assertThat(missing.blockerCategory()).isEqualTo("MISSING_EVIDENCE");
    }

    /** Cria uma tarefa especializada com o contexto informado pelo teste. */
    private ApolloPdeAudiovisualTask task(JsonNode context) {
        return new ApolloPdeAudiovisualTask(
                336L,
                "videomaker",
                "pde-construction-approval",
                6,
                "audiovisual",
                "Produzir audiovisual quando previsto",
                "Produzir audiovisual quando previsto",
                "Produz somente o audiovisual exigido pela arquitetura.",
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

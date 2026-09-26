package com.marketinghub.videomanagement.pdeaudiovisualv1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Responsabilidade: montar callbacks auditáveis e sem custo para a decisão audiovisual de Apolo. */
@Component
public class ApolloPdeAudiovisualCallbackFactory {
    static final String RULE_VERSION = "apollo-pde-audiovisual-contract-v1";
    private final ObjectMapper objectMapper;

    /** Configura a serialização da entrada integral e das evidências funcionais. */
    public ApolloPdeAudiovisualCallbackFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** Monta a conclusão determinística que confirma ausência intencional de artefato. */
    public Map<String, Object> complete(
            ApolloPdeAudiovisualTask task, ApolloPdeAudiovisualDecision decision) {
        return common(task, decision);
    }

    /** Monta o bloqueio funcional com orientação acionável e sem iniciar provider. */
    public Map<String, Object> block(
            ApolloPdeAudiovisualTask task, ApolloPdeAudiovisualDecision decision) {
        Map<String, Object> payload = common(task, decision);
        payload.put("error", decision.rationale());
        payload.put(
                "blockerGuidance",
                Map.of(
                        "category", decision.blockerCategory(),
                        "recommendedAction", decision.recommendedAction(),
                        "helpLinks", List.of(helpLink(task))));
        return payload;
    }

    /** Monta bloqueio técnico quando a fila devolver contrato incompatível antes do callback funcional. */
    public Map<String, Object> technicalFailure(ApolloPdeAudiovisualTask task, RuntimeException cause) {
        return block(
                task,
                new ApolloPdeAudiovisualDecision(
                        ApolloPdeAudiovisualDecision.Outcome.TECHNICAL_FAILURE,
                        cause.toString(),
                        "Corrija o contrato especializado da fila audiovisual e reinicie a mesma atividade de Apolo.",
                        "TECHNICAL_FAILURE"));
    }

    /** Compõe resultado, evidência, custo zero e auditoria do comando sem modelo. */
    private Map<String, Object> common(
            ApolloPdeAudiovisualTask task, ApolloPdeAudiovisualDecision decision) {
        String input = objectMapper.valueToTree(task).toString();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("resultJson", result(task, decision).toString());
        payload.put("evidenceJson", evidence(task, decision).toString());
        payload.put("modelUsages", List.of());
        payload.put("executionAudit", audit(input));
        return payload;
    }

    /** Registra o desfecho sem declarar uma próxima etapa local. */
    private ObjectNode result(
            ApolloPdeAudiovisualTask task, ApolloPdeAudiovisualDecision decision) {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("decision", decision.canComplete() ? "READY" : "BLOCKED");
        result.put("audiovisualRequirement", decision.outcome().name());
        if (ApolloPdeAudiovisualDecision.Outcome.NOT_REQUIRED.equals(decision.outcome())) {
            result.put("audiovisualRequired", false);
        } else if (ApolloPdeAudiovisualDecision.Outcome.REQUIRES_AUTHORIZATION.equals(decision.outcome())
                || ApolloPdeAudiovisualDecision.Outcome.MATERIALIZED.equals(decision.outcome())) {
            result.put("audiovisualRequired", true);
        } else {
            result.putNull("audiovisualRequired");
        }
        result.put("rationale", decision.rationale());
        result.put("recommendedAction", decision.recommendedAction());
        var artifactIds = result.putArray("artifactIds");
        JsonNode materialization = materialization(task);
        if (ApolloPdeAudiovisualDecision.Outcome.MATERIALIZED.equals(decision.outcome())) {
            materialization.path("artifactIds").forEach(value -> artifactIds.add(value.asLong()));
            result.put("videoProductionCycleId", materialization.path("videoProductionCycleId").asLong());
            result.put("videoProjectId", materialization.path("videoProjectId").asLong());
            result.put("salesVideoJobId", materialization.path("salesVideoJobId").asLong());
            result.put("authorizedBy", materialization.path("authorizedBy").asText());
            result.put("budgetLimitUsd", materialization.path("budgetLimitUsd").decimalValue());
        }
        result.put("providerCalls", 0);
        result.put("creditsConsumed", 0);
        result.put(
                "providerCostUsd",
                ApolloPdeAudiovisualDecision.Outcome.MATERIALIZED.equals(decision.outcome())
                        ? materialization.path("actualCostUsd").decimalValue()
                        : java.math.BigDecimal.ZERO);
        result.put("externalSideEffects", false);
        return result;
    }

    /** Preserva a origem exata do booleano e a ausência de efeitos externos. */
    private ObjectNode evidence(
            ApolloPdeAudiovisualTask task, ApolloPdeAudiovisualDecision decision) {
        ObjectNode evidence = objectMapper.createObjectNode();
        evidence.put("agent", "Apolo");
        evidence.put("ruleVersion", RULE_VERSION);
        evidence.put("sourceReference", task.sourceReference());
        evidence.put("processCode", task.processCode());
        evidence.put("processVersion", task.processVersion());
        evidence.put("activityId", task.activityId());
        evidence.put("executionResourceCode", "video-management-service");
        String contractField = contractField(task);
        evidence.put("contractField", contractField);
        if (task.taskTarget() != null && task.taskTarget().pdeContext() != null) {
            putBooleanOrNull(
                    evidence,
                    "contractValue",
                    contractValue(task));
            evidence.put("productId", task.taskTarget().productId());
        } else {
            evidence.putNull("contractValue");
        }
        boolean materialized =
                ApolloPdeAudiovisualDecision.Outcome.MATERIALIZED.equals(decision.outcome());
        evidence.put("artifactCreated", materialized);
        if (materialized) {
            JsonNode materialization = materialization(task);
            evidence.put(
                    "videoProductionCycleId",
                    materialization.path("videoProductionCycleId").asLong());
            evidence.set("artifactIds", materialization.path("artifactIds").deepCopy());
        }
        evidence.put("providerCalls", 0);
        evidence.put("creditsConsumed", 0);
        evidence.put("externalSideEffects", false);
        return evidence;
    }

    /** Lê o recibo mínimo já validado pelo avaliador sem buscar estado fora da tarefa. */
    private JsonNode materialization(ApolloPdeAudiovisualTask task) {
        if (task == null || task.taskTarget() == null || task.taskTarget().pdeContext() == null) {
            return objectMapper.missingNode();
        }
        return task.taskTarget().pdeContext().path("audiovisualMaterialization");
    }

    /** Informa o caminho auditável conforme a fronteira entre produto e comunicação comercial. */
    private String contractField(ApolloPdeAudiovisualTask task) {
        return "creative-production-approval".equals(task.processCode())
                ? "taskTarget.pdeContext.communicationMaterialization.audiovisualRequired"
                : "taskTarget.pdeContext.harness.audiovisualRequired";
    }

    /** Lê o mesmo valor usado pelo avaliador sem converter texto ou campo ausente. */
    private JsonNode contractValue(ApolloPdeAudiovisualTask task) {
        return "creative-production-approval".equals(task.processCode())
                ? task.taskTarget()
                        .pdeContext()
                        .path("communicationMaterialization")
                        .path("audiovisualRequired")
                : task.taskTarget().pdeContext().path("harness").path("audiovisualRequired");
    }

    /** Declara execução determinística com a entrada integral usada na decisão. */
    private Map<String, Object> audit(String input) {
        Map<String, Object> audit = new LinkedHashMap<>();
        audit.put("executionMode", "DETERMINISTIC");
        audit.put("modelCode", RULE_VERSION);
        audit.put("reasoningEffort", "NOT_APPLICABLE");
        audit.put("promptSent", input);
        audit.put("agentPromptPart", null);
        audit.put("activityPromptPart", input);
        audit.put("accessedUrls", List.of());
        return audit;
    }

    /** Direciona bloqueios à tela do produto sem depender de logs técnicos. */
    private Map<String, Object> helpLink(ApolloPdeAudiovisualTask task) {
        Long productId = task.taskTarget() == null ? null : task.taskTarget().productId();
        String url = productId == null ? "/agent-tasks" : "/products/" + productId + "/value-chain-history";
        return Map.of("label", "Abrir cadeia de valor do produto", "url", url);
    }

    /** Copia somente booleano real e preserva qualquer outro tipo como nulo. */
    private void putBooleanOrNull(ObjectNode target, String field, JsonNode value) {
        if (value != null && value.isBoolean()) {
            target.put(field, value.booleanValue());
        } else {
            target.putNull(field);
        }
    }
}

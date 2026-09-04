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
        payload.put("resultJson", result(decision).toString());
        payload.put("evidenceJson", evidence(task).toString());
        payload.put("modelUsages", List.of());
        payload.put("executionAudit", audit(input));
        return payload;
    }

    /** Registra o desfecho sem declarar uma próxima etapa local. */
    private ObjectNode result(ApolloPdeAudiovisualDecision decision) {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("decision", decision.canComplete() ? "READY" : "BLOCKED");
        result.put("audiovisualRequirement", decision.outcome().name());
        if (ApolloPdeAudiovisualDecision.Outcome.NOT_REQUIRED.equals(decision.outcome())) {
            result.put("audiovisualRequired", false);
        } else if (ApolloPdeAudiovisualDecision.Outcome.REQUIRES_AUTHORIZATION.equals(decision.outcome())) {
            result.put("audiovisualRequired", true);
        } else {
            result.putNull("audiovisualRequired");
        }
        result.put("rationale", decision.rationale());
        result.put("recommendedAction", decision.recommendedAction());
        result.putArray("artifactIds");
        result.put("providerCalls", 0);
        result.put("creditsConsumed", 0);
        result.put("providerCostUsd", 0);
        result.put("externalSideEffects", false);
        return result;
    }

    /** Preserva a origem exata do booleano e a ausência de efeitos externos. */
    private ObjectNode evidence(ApolloPdeAudiovisualTask task) {
        ObjectNode evidence = objectMapper.createObjectNode();
        evidence.put("agent", "Apolo");
        evidence.put("ruleVersion", RULE_VERSION);
        evidence.put("sourceReference", task.sourceReference());
        evidence.put("processCode", task.processCode());
        evidence.put("processVersion", task.processVersion());
        evidence.put("activityId", task.activityId());
        evidence.put("executionResourceCode", "video-management-service");
        evidence.put("contractField", "taskTarget.pdeContext.harness.audiovisualRequired");
        if (task.taskTarget() != null && task.taskTarget().pdeContext() != null) {
            putBooleanOrNull(
                    evidence,
                    "contractValue",
                    task.taskTarget().pdeContext().path("harness").path("audiovisualRequired"));
            evidence.put("productId", task.taskTarget().productId());
        } else {
            evidence.putNull("contractValue");
        }
        evidence.put("artifactCreated", false);
        evidence.put("providerCalls", 0);
        evidence.put("creditsConsumed", 0);
        evidence.put("externalSideEffects", false);
        return evidence;
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

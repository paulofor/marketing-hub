package com.marketinghub.product.valuedelivery.v1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.agenttask.*;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.product.service.agentvalidation.PdeOperationalControlEvidence;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Governa a continuidade da proposta de Turmalina pelas atividades existentes do Processo 3. */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductValueDeliveryService implements AgentTaskCompletionHook {
  public static final String PROCESS = "pde-construction-approval";
  public static final Set<String> REVIEWS =
      Set.of("psiqueAdherent", "psiqueRecovery", "psiqueSafety", "commercialIntegrityReview");
  private final ProductRepository products;
  private final AgentTaskRepository tasks;
  private final PdeOperationalControlEvidence evidence;
  private final ObjectMapper json;

  /** Limita o contrato às novas definições, preservando tarefas e execuções históricas. */
  public boolean enabled(BusinessProcessDefinition process) {
    return process != null
        && PROCESS.equals(process.getProcessCode())
        && ProductValueContract.VERSION.equals(
            read(process.getDiagramJson()).path("productValueDeliveryContract").asText());
  }

  /**
   * Fixa catálogo e proposta uma única vez por execução, inclusive a não aplicabilidade do tipo.
   */
  public void pin(AgentTask task, ObjectNode idea, AgentTaskTargetResponse target) {
    if (!enabled(task.getProcessDefinition()) || target == null || target.productId() == null)
      return;
    for (AgentTask previous : history(task.getProcessDefinition(), task.getSourceReference())) {
      JsonNode fixed = read(previous.getProductBigIdeaJson()).path("valueDeliveryBaseline");
      if (fixed.isObject()) {
        idea.set("valueDeliveryBaseline", fixed.deepCopy());
        return;
      }
    }
    var baseline = json.createObjectNode();
    baseline.put("contractVersion", ProductValueContract.VERSION);
    baseline.put("productId", target.productId());
    baseline.put("sourceReference", task.getSourceReference());
    var product = products.findById(target.productId()).orElseThrow();
    var type = product.getProductTypeDefinition();
    baseline.put("applicable", type != null && "AI_PWA_CONSULTANT_PRODUCT".equals(type.getCode()));
    if (baseline.path("applicable").asBoolean()) {
      baseline.put("productTypeCode", type.getCode());
      baseline.put("blueprintVersion", type.getBlueprintVersion());
      baseline.set("bigIdea", idea.path("revision").deepCopy());
      var requirements = baseline.putObject("requirements");
      for (String field : List.of("promise", "mechanism", "limits"))
        requirements.set(
            "bigIdea." + field, idea.path("revision").path("content").path(field).deepCopy());
      requirements.put("blueprint.customerJob", type.getCustomerJob());
      requirements.put("blueprint.valueMechanism", type.getValueMechanism());
      requirements.put("blueprint.experienceFlow", type.getExperienceFlow());
      requirements.put("blueprint.requiredInputs", type.getRequiredInputs());
      requirements.put("blueprint.expectedOutputs", type.getExpectedOutputs());
      requirements.put("blueprint.memoryStrategy", type.getMemoryStrategy());
      requirements.put("blueprint.integrationRequirements", type.getIntegrationRequirements());
      requirements.put("blueprint.safetyGuardrails", type.getSafetyGuardrails());
      requirements.put("blueprint.successMetrics", type.getSuccessMetrics());
      baseline.put("backendSdkModule", type.getBackendSdkModule());
      baseline.put("frontendSdkModule", type.getFrontendSdkModule());
    }
    idea.set("valueDeliveryBaseline", baseline);
  }

  /**
   * Expõe plano e provas exatas aos agentes, sem iniciar tarefa nem alterar a fotografia original.
   */
  public JsonNode context(AgentTask task, AgentTaskTargetResponse target) {
    JsonNode baseline = read(task.getProductBigIdeaJson()).path("valueDeliveryBaseline");
    if (!baseline.path("applicable").asBoolean()) return json.createObjectNode();
    var result = json.createObjectNode();
    result.set("baseline", baseline);
    result.put("baselineSha256", ProductValueContract.hash(baseline));
    AgentTask specification =
        latest(history(task.getProcessDefinition(), task.getSourceReference()), "journey");
    if (specification != null && "COMPLETED".equals(specification.getStatus())) {
      JsonNode spec = read(specification.getResultJson()).path("valueSpecification");
      result.set("specification", spec);
      result.put("specificationTaskId", specification.getId());
      result.put(
          "specificationReference", "VALUE_SPECIFICATION:" + ProductValueContract.hash(spec));
      if (target != null)
        evidence
            .resolve(target.productSlug(), target.experienceVersion())
            .ifPresent(
                report -> {
                  result.set("implementationEvidence", report);
                  result.put("reviewReference", reviewReference(spec, report));
                });
    }
    return result;
  }

  /**
   * Confere especificação e revisões sem disputar o callback de acesso com a reconciliação de
   * runtime.
   */
  @Override
  public boolean supports(AgentTask task) {
    return enabled(task.getProcessDefinition())
        && read(task.getProductBigIdeaJson())
            .path("valueDeliveryBaseline")
            .path("applicable")
            .asBoolean()
        && (Set.of("journey", "deliverables").contains(task.getProcessActivityId())
            || REVIEWS.contains(task.getProcessActivityId()));
  }

  /** Recusa perda de proposta e parecer sobre artefato diferente antes de aceitar o callback. */
  @Override
  public CompletionDisposition apply(AgentTask task, CompleteAgentTaskRequest request) {
    try {
      JsonNode result = read(request.resultJson());
      JsonNode baseline = read(task.getProductBigIdeaJson()).path("valueDeliveryBaseline");
      if ("journey".equals(task.getProcessActivityId())) {
        completeBaseline(baseline);
        ProductValueContract.specification(baseline, result.path("valueSpecification"));
      } else {
        var history = history(task.getProcessDefinition(), task.getSourceReference());
        JsonNode spec = specification(history);
        String reference = "VALUE_SPECIFICATION:" + ProductValueContract.hash(spec);
        if (REVIEWS.contains(task.getProcessActivityId())) {
          JsonNode report = report(history, baseline, spec);
          reference = reviewReference(spec, report);
        }
        if ("deliverables".equals(task.getProcessActivityId())) {
          completeBaseline(baseline);
          ProductValueContract.specification(baseline, spec);
          ProductValueReference.require(result, reference);
        } else if (!contains(result, reference)) {
          throw new IllegalArgumentException(
              "O resultado não identifica a especificação/prova revisada; preserve a saída e corrija a referência.");
        }
      }
      return CompletionDisposition.COMPLETE;
    } catch (IllegalArgumentException ex) {
      log.warn(
          "Continuidade de valor recusada taskId={} source={} activity={}",
          task.getId(),
          task.getSourceReference(),
          task.getProcessActivityId(),
          ex);
      throw new AgentTaskResultRejectedException(ex);
    }
  }

  /** Verifica a prova antes de novas revisões e novamente no gate, sem confiar só no score. */
  public void verify(BusinessProcessDefinition process, String source, boolean includeReviews) {
    if (!enabled(process)) return;
    var history = history(process, source);
    AgentTask journey = latest(history, "journey");
    if (journey == null)
      throw new IllegalArgumentException(
          "Dédalo precisa especificar as histórias e provas da entrega antes da homologação.");
    JsonNode baseline = read(journey.getProductBigIdeaJson()).path("valueDeliveryBaseline");
    if (!baseline.isObject())
      throw new IllegalArgumentException(
          "A nova execução não fixou o tipo e a proposta. Corrija a origem antes de revisar.");
    if (!baseline.path("applicable").asBoolean()) return;
    JsonNode spec = specification(history);
    JsonNode report = report(history, baseline, spec);
    if (includeReviews)
      for (String activity : REVIEWS) {
        AgentTask review = latest(history, activity);
        if (review == null
            || !"COMPLETED".equals(review.getStatus())
            || !contains(read(review.getResultJson()), reviewReference(spec, report)))
          throw new IllegalArgumentException(
              "Falta revisão independente da mesma proposta e implementação: " + activity);
      }
  }

  /** Consulta o catálogo de provas já existente e exige a identidade da homologação real. */
  private JsonNode report(List<AgentTask> history, JsonNode baseline, JsonNode spec) {
    AgentTask technical = latest(history, "technicalHomologation");
    if (technical == null || !"COMPLETED".equals(technical.getStatus()))
      throw new IllegalArgumentException("A implementação ainda precisa da homologação técnica.");
    JsonNode tested = read(technical.getResultJson());
    String version = tested.path("prototypeVersion").asText();
    JsonNode report =
        evidence
            .resolve(tested.path("productSlug").asText(), version)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Falta prova executada da entrega; preservar o protótipo e encaminhar as histórias pendentes à construção."));
    if (!baseline.path("productId").equals(tested.path("productId"))
        || !baseline.path("sourceReference").equals(tested.path("sourceReference")))
      throw new IllegalArgumentException("Homologação pertence a outro produto ou ciclo.");
    completeBaseline(baseline);
    ProductValueContract.evidence(baseline, spec, report, version);
    return report;
  }

  /** Impede aceitar base vazia ou descrição de um tipo sem a proposta própria do produto. */
  private void completeBaseline(JsonNode baseline) {
    if (baseline.path("bigIdea").path("id").asLong() <= 0
        || baseline.path("blueprintVersion").asText().isBlank())
      throw new IllegalArgumentException("Falta Big Idea fixada ou base versionada de Turmalina.");
    for (JsonNode requirement : baseline.path("requirements"))
      if (!requirement.isTextual() || requirement.asText().isBlank())
        throw new IllegalArgumentException(
            "A base de construção está incompleta; corrigir sua origem.");
  }

  /** Seleciona o último contrato aceito, sem ressuscitar plano anterior a uma rejeição. */
  private JsonNode specification(List<AgentTask> history) {
    AgentTask task = latest(history, "journey");
    if (task == null || !"COMPLETED".equals(task.getStatus()))
      throw new IllegalArgumentException("Falta especificação atual de Dédalo.");
    return read(task.getResultJson()).path("valueSpecification");
  }

  /** Consulta somente o contexto exato do processo para impedir mistura entre produtos e ciclos. */
  private List<AgentTask> history(BusinessProcessDefinition process, String source) {
    return tasks.findByProcessDefinitionIdAndSourceReferenceOrderByCreatedAtAscIdAsc(
        process.getId(), source);
  }

  /** Usa a ordem persistida para reconhecer inclusive uma tentativa posterior bloqueada. */
  private AgentTask latest(List<AgentTask> history, String activity) {
    return history.stream()
        .filter(t -> activity.equals(t.getProcessActivityId()))
        .reduce((a, b) -> b)
        .orElse(null);
  }

  /** Identifica simultaneamente a especificação e os bytes da prova examinada. */
  private String reviewReference(JsonNode spec, JsonNode report) {
    return "VALUE_DELIVERY:"
        + ProductValueContract.hash(spec)
        + ":"
        + report.path("reportSha256").asText();
  }

  /** Confere referência literal nos campos do contrato existente, sem aceitar simples substring. */
  private boolean contains(JsonNode node, String reference) {
    if (node.isTextual()) return reference.equals(node.asText());
    for (JsonNode child : node) if (contains(child, reference)) return true;
    return false;
  }

  /**
   * Lê contratos persistidos, registrando falha sem reinterpretar conteúdo inválido como legado.
   */
  private JsonNode read(String raw) {
    if (raw == null || raw.isBlank()) return json.createObjectNode();
    try {
      return json.readTree(raw);
    } catch (Exception ex) {
      log.error("Contrato de continuidade de valor contém JSON inválido", ex);
      throw new IllegalArgumentException("Contrato de continuidade inválido.", ex);
    }
  }
}

package com.marketinghub.product.service.agentvalidation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.agentactivity.AgentProductProcessActivityReadiness;
import com.marketinghub.businessprocess.execution.service.agentactivity.AgentProductProcessActivityReadinessProvider;
import com.marketinghub.businessprocess.execution.service.predecessor.ProductProcessActivityPredecessorReadiness;
import com.marketinghub.businessprocess.execution.service.predecessor.ProductProcessActivityPredecessorService;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Responsabilidade: encaminhar rejeições funcionais do PDE para correção antes da revalidação. */
@Service
@Slf4j
public class PdeAgentValidationReworkReadinessProvider
    implements AgentProductProcessActivityReadinessProvider {
  static final int PROCESS_VERSION = 8;
  static final String CORRECTION_ACTIVITY = "prototypeCorrection";
  private static final Set<String> REVIEW_ACTIVITIES =
      Set.of(
          "technicalHomologation",
          "psiqueAdherent",
          "psiqueRecovery",
          "psiqueSafety",
          "commercialIntegrityReview");
  private static final Map<String, String> REQUIRED_APPROVAL =
      Map.of(
          "psiqueAdherent", "technicalHomologation",
          "psiqueRecovery", "psiqueAdherent",
          "psiqueSafety", "psiqueRecovery",
          "commercialIntegrityReview", "psiqueSafety");
  private static final Map<String, String> EXPECTED_DECISION =
      Map.of(
          "technicalHomologation", "APPROVED",
          "psiqueAdherent", "APPROVED",
          "psiqueRecovery", "APPROVED",
          "psiqueSafety", "APPROVED");

  private final ProductProcessActivityPredecessorService predecessors;
  private final AgentTaskRepository tasks;
  private final ObjectMapper json;

  /** Configura a ordem do grafo, as tentativas auditadas e o leitor dos contratos. */
  public PdeAgentValidationReworkReadinessProvider(
      ProductProcessActivityPredecessorService predecessors,
      AgentTaskRepository tasks,
      ObjectMapper json) {
    this.predecessors = predecessors;
    this.tasks = tasks;
    this.json = json;
  }

  /** Reconhece somente as atividades executadas da versão com retrabalho explícito. */
  @Override
  public boolean supports(
      BusinessProcessDefinition process, BusinessProcessActivityDefinition activityDefinition) {
    return process != null
        && activityDefinition != null
        && PdeAgentValidationGateActivityExecutor.PROCESS_CODE.equals(process.getProcessCode())
        && Integer.valueOf(PROCESS_VERSION).equals(process.getVersionNumber())
        && (REVIEW_ACTIVITIES.contains(activityDefinition.getActivityId())
            || CORRECTION_ACTIVITY.equals(activityDefinition.getActivityId()));
  }

  /** Libera somente a correção pendente ou a próxima validação aprovada da mesma versão. */
  @Override
  public AgentProductProcessActivityReadiness readiness(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activityDefinition,
      Product product,
      String sourceReference) {
    if (!supports(process, activityDefinition)) {
      return blocked("A atividade não pertence ao contrato de retrabalho da validação PDE.");
    }
    List<AgentTask> history = processHistory(sourceReference);
    String expectedVersion = expectedPrototypeVersion(product);
    Optional<AgentTask> rejection = unresolvedFunctionalRejection(history, expectedVersion);
    if (CORRECTION_ACTIVITY.equals(activityDefinition.getActivityId())) {
      return correctionReadiness(rejection);
    }
    if ("technicalHomologation".equals(activityDefinition.getActivityId())
        && rejection.isPresent()) {
      return blocked(correctionRequiredReason(rejection.orElseThrow()));
    }
    ProductProcessActivityPredecessorReadiness predecessor =
        predecessors.readiness(process, activityDefinition, sourceReference);
    if (!predecessor.ready()) {
      return blocked(predecessor.reason());
    }
    String requiredActivity = REQUIRED_APPROVAL.get(activityDefinition.getActivityId());
    if (requiredActivity == null) {
      return ready("A correção está resolvida e a homologação técnica pode validar a nova versão.");
    }
    if (expectedVersion == null) {
      return blocked("A versão aceita do protótipo ainda não foi persistida no produto.");
    }
    Optional<AgentTask> approved =
        latestCurrentProcessTask(history, process, requiredActivity)
            .filter(task -> approvedForVersion(task, requiredActivity, expectedVersion));
    if (approved.isEmpty()) {
      return blocked(
          "Conclua primeiro "
              + activityLabel(requiredActivity)
              + " para a versão "
              + expectedVersion
              + ".");
    }
    return ready(
        activityLabel(requiredActivity)
            + " aprovou a mesma versão; esta atividade pode ser executada.");
  }

  /** Expõe o diagnóstico exato que originará a tarefa condicional de correção. */
  private AgentProductProcessActivityReadiness correctionReadiness(Optional<AgentTask> rejection) {
    if (rejection.isEmpty()) {
      return blocked("Nenhuma rejeição funcional pendente exige correção do protótipo.");
    }
    AgentTask task = rejection.orElseThrow();
    return ready(correctionRequiredReason(task));
  }

  /** Monta uma orientação curta com causa, ação e retorno obrigatório ao harness. */
  private String correctionRequiredReason(AgentTask rejection) {
    String rootCause = rootCause(rejection);
    String action = text(rejection.getBlockerAction());
    if (action == null) action = "Aplique a menor correção funcional descrita no parecer.";
    return limit(
        "A tarefa #"
            + rejection.getId()
            + " ("
            + activityLabel(rejection.getProcessActivityId())
            + ") rejeitou a versão. Causa-raiz: "
            + rootCause
            + " Próxima ação: "
            + action
            + " Depois da correção, publique uma nova versão e execute novamente a homologação técnica.",
        1800);
  }

  /** Localiza a rejeição funcional mais recente ainda sem correção válida posterior. */
  private Optional<AgentTask> unresolvedFunctionalRejection(
      List<AgentTask> history, String expectedVersion) {
    Optional<AgentTask> rejection =
        history.stream()
            .filter(task -> REVIEW_ACTIVITIES.contains(task.getProcessActivityId()))
            .filter(task -> "BLOCKED".equals(task.getStatus()))
            .filter(task -> "FUNCTIONAL_ADJUSTMENT".equals(task.getBlockerCategory()))
            .max(Comparator.comparing(AgentTask::getId));
    if (rejection.isEmpty()) return Optional.empty();
    Optional<AgentTask> correction =
        history.stream()
            .filter(task -> CORRECTION_ACTIVITY.equals(task.getProcessActivityId()))
            .filter(task -> "COMPLETED".equals(task.getStatus()))
            .filter(task -> validCorrection(task, expectedVersion))
            .max(Comparator.comparing(AgentTask::getId));
    return correction.filter(task -> task.getId() > rejection.orElseThrow().getId()).isPresent()
        ? Optional.empty()
        : rejection;
  }

  /** Confirma que Dédalo registrou versão nova e retorno obrigatório à homologação técnica. */
  private boolean validCorrection(AgentTask task, String expectedVersion) {
    if (expectedVersion == null || task.getResultJson() == null) return false;
    try {
      JsonNode result = json.readTree(task.getResultJson());
      JsonNode plan = result.path("correctionPlan");
      return "READY".equals(result.path("decision").asText())
          && expectedVersion.equals(plan.path("correctedPrototypeVersion").asText())
          && !expectedVersion.equals(plan.path("previousPrototypeVersion").asText())
          && "technicalHomologation".equals(plan.path("nextActivityId").asText())
          && plan.path("verification").path("technicalRevalidationRequired").asBoolean(false)
          && plan.path("verification").path("noExternalSideEffects").asBoolean(false);
    } catch (Exception ex) {
      log.error("Falha ao ler correção PDE. taskId={}", task.getId(), ex);
      return false;
    }
  }

  /** Exige uma conclusão da própria versão publicada do processo de revalidação. */
  private Optional<AgentTask> latestCurrentProcessTask(
      List<AgentTask> history, BusinessProcessDefinition process, String activityId) {
    return history.stream()
        .filter(task -> task.getProcessDefinition() != null)
        .filter(task -> process.getId().equals(task.getProcessDefinition().getId()))
        .filter(task -> activityId.equals(task.getProcessActivityId()))
        .filter(task -> "COMPLETED".equals(task.getStatus()))
        .max(Comparator.comparing(AgentTask::getId));
  }

  /** Valida decisão e versão da prova predecessora sem confiar somente no status técnico. */
  private boolean approvedForVersion(
      AgentTask task, String activityId, String expectedPrototypeVersion) {
    try {
      JsonNode result = json.readTree(task.getResultJson());
      return expectedPrototypeVersion.equals(result.path("prototypeVersion").asText())
          && EXPECTED_DECISION.get(activityId).equals(result.path("decision").asText());
    } catch (Exception ex) {
      log.error(
          "Falha ao ler prova predecessora PDE. taskId={} activityId={}",
          task.getId(),
          activityId,
          ex);
      return false;
    }
  }

  /** Lista tarefas de todas as versões do mesmo processo sem misturar outra origem. */
  private List<AgentTask> processHistory(String sourceReference) {
    if (sourceReference == null || sourceReference.isBlank()) return List.of();
    return tasks.findBySourceReferenceOrderByCreatedAtAscIdAsc(sourceReference).stream()
        .filter(task -> task.getProcessDefinition() != null)
        .filter(
            task ->
                PdeAgentValidationGateActivityExecutor.PROCESS_CODE.equals(
                    task.getProcessDefinition().getProcessCode()))
        .toList();
  }

  /** Lê a versão aceita do contrato do produto e falha fechada diante de JSON inválido. */
  private String expectedPrototypeVersion(Product product) {
    if (product == null || product.getValidationDefinitionJson() == null) return null;
    try {
      return text(
          json.readTree(product.getValidationDefinitionJson())
              .path("privatePrototypeAcceptance")
              .path("prototypeVersion")
              .asText(null));
    } catch (Exception ex) {
      log.error("Falha ao ler versão aceita do protótipo. productId={}", product.getId(), ex);
      return null;
    }
  }

  /** Usa primeiro a causa funcional estruturada e mantém o erro auditado como fallback. */
  private String rootCause(AgentTask task) {
    if (task.getResultJson() != null) {
      try {
        String value = text(json.readTree(task.getResultJson()).path("rootCause").asText(null));
        if (value != null) return value;
      } catch (Exception ex) {
        log.error("Falha ao ler causa-raiz do parecer PDE. taskId={}", task.getId(), ex);
      }
    }
    String error = text(task.getExecutionError());
    return error == null ? "O parecer funcional não atingiu os critérios publicados." : error;
  }

  /** Traduz identificadores estáveis para orientação operacional legível. */
  private String activityLabel(String activityId) {
    return switch (activityId) {
      case "technicalHomologation" -> "a homologação técnica";
      case "psiqueAdherent" -> "Psique · cenário aderente";
      case "psiqueRecovery" -> "Psique · fricção e recuperação";
      case "psiqueSafety" -> "Psique · limite e segurança";
      case "commercialIntegrityReview" -> "a revisão de integridade de Têmis";
      default -> activityId == null ? "a validação" : activityId;
    };
  }

  /** Cria uma resposta liberada com motivo persistível e legível. */
  private AgentProductProcessActivityReadiness ready(String reason) {
    return new AgentProductProcessActivityReadiness(true, reason);
  }

  /** Cria uma resposta bloqueada sem depender de exceção ou log técnico. */
  private AgentProductProcessActivityReadiness blocked(String reason) {
    return new AgentProductProcessActivityReadiness(false, reason);
  }

  /** Normaliza texto opcional sem promover valor vazio a evidência. */
  private String text(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  /** Limita orientação trazida do modelo para manter o contrato de tela controlado. */
  private String limit(String value, int maxLength) {
    return value.length() <= maxLength ? value : value.substring(0, maxLength - 1) + "…";
  }
}

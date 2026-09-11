package com.marketinghub.product.service.agentvalidation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

/**
 * Responsabilidade: orientar o retrabalho de rejeições funcionais e falhas de homologação do PDE.
 */
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
          "psiqueSafety", "APPROVED",
          "commercialIntegrityReview", "APPROVED");

  private final ProductProcessActivityPredecessorService predecessors;
  private final AgentTaskRepository tasks;
  private final ObjectMapper json;

  @org.springframework.beans.factory.annotation.Autowired(required = false)
  private com.marketinghub.agenttask.AgentTaskTargetContextProvider taskTargets;

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

  /** Oferece correção dos bloqueios e exige as aprovações sequenciais da versão corrente. */
  @Override
  public AgentProductProcessActivityReadiness readiness(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activityDefinition,
      Product product,
      String sourceReference) {
    if (!supports(process, activityDefinition)) {
      return blocked("A atividade não pertence ao contrato de retrabalho da validação PDE.");
    }
    List<PdeValidationTaskSnapshot> history = processHistory(sourceReference);
    String expectedVersion = expectedPrototypeVersion(product, sourceReference);
    Optional<PdeValidationTaskSnapshot> rejection =
        unresolvedFunctionalRejection(history, expectedVersion);
    if (CORRECTION_ACTIVITY.equals(activityDefinition.getActivityId())) {
      return correctionReadiness(correctionSource(history, expectedVersion));
    }
    if (rejection.isPresent()) {
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
    if (!hasCurrentApproval(history, process, requiredActivity, expectedVersion)) {
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

  /** Exige nova ocorrência para histórico superado, preservando bloqueios ainda atuais. */
  @Override
  public boolean requiresFreshExecution(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activityDefinition,
      Product product,
      String sourceReference) {
    if (!supports(process, activityDefinition)) return false;
    List<PdeValidationTaskSnapshot> history = processHistory(sourceReference);
    String version = expectedPrototypeVersion(product, sourceReference);
    String activityId = activityDefinition.getActivityId();
    boolean currentBlock =
        latestCurrentProcessTask(history, process, activityId)
            .filter(task -> "BLOCKED".equals(task.status()))
            .filter(
                task ->
                    CORRECTION_ACTIVITY.equals(activityId)
                        || task.id() > latestCorrectionId(history, version))
            .isPresent();
    if (currentBlock) return false;
    if (CORRECTION_ACTIVITY.equals(activityId)) {
      return correctionSource(history, version).isPresent();
    }
    return version == null
        || !hasCurrentApproval(history, process, activityDefinition.getActivityId(), version);
  }

  /** Aceita somente prova da versão atual produzida depois da última correção aplicável. */
  private boolean hasCurrentApproval(
      List<PdeValidationTaskSnapshot> history,
      BusinessProcessDefinition process,
      String activityId,
      String version) {
    long correctionId = latestCorrectionId(history, version);
    return latestCurrentProcessTask(history, process, activityId)
        .filter(task -> "COMPLETED".equals(task.status()))
        .filter(task -> task.id() > correctionId)
        .filter(task -> approvedForVersion(task, activityId, version))
        .isPresent();
  }

  /** Identifica a última correção válida para separar pendências atuais de pareceres superados. */
  private long latestCorrectionId(List<PdeValidationTaskSnapshot> history, String version) {
    return history.stream()
        .filter(task -> CORRECTION_ACTIVITY.equals(task.processActivityId()))
        .filter(task -> "COMPLETED".equals(task.status()))
        .filter(task -> validCorrection(task, version))
        .mapToLong(PdeValidationTaskSnapshot::id)
        .max()
        .orElse(0L);
  }

  /** Expõe o diagnóstico funcional ou técnico que originará a tarefa condicional de correção. */
  private AgentProductProcessActivityReadiness correctionReadiness(
      Optional<PdeValidationTaskSnapshot> rejection) {
    if (rejection.isEmpty()) {
      return blocked(
          "Nenhuma rejeição funcional ou falha de homologação pendente exige correção do protótipo.");
    }
    PdeValidationTaskSnapshot task = rejection.orElseThrow();
    return ready(correctionRequiredReason(task));
  }

  /**
   * Distingue falha técnica de rejeição funcional e preserva causa, origem e retorno aos testes.
   */
  private String correctionRequiredReason(PdeValidationTaskSnapshot rejection) {
    String rootCause = rootCause(rejection);
    String action = text(rejection.blockerAction());
    if (action == null) action = "Aplique a menor correção funcional descrita no parecer.";
    return limit(
        "A tarefa #"
            + rejection.id()
            + " ("
            + activityLabel(rejection.processActivityId())
            + ("TECHNICAL_FAILURE".equals(rejection.blockerCategory())
                ? ") não conseguiu homologar o protótipo. Causa registrada: "
                : ") rejeitou a versão. Causa-raiz: ")
            + rootCause
            + " Próxima ação: "
            + action
            + " Depois da correção, publique uma nova versão e execute novamente a homologação técnica.",
        1800);
  }

  /** Seleciona a origem mais recente sem confundir falha técnica com reprovação funcional. */
  private Optional<PdeValidationTaskSnapshot> correctionSource(
      List<PdeValidationTaskSnapshot> history, String version) {
    Optional<PdeValidationTaskSnapshot> functional =
        unresolvedFunctionalRejection(history, version);
    long correctionId = latestCorrectionId(history, version);
    Optional<PdeValidationTaskSnapshot> technical =
        history.stream()
            .filter(task -> "technicalHomologation".equals(task.processActivityId()))
            .max(Comparator.comparing(PdeValidationTaskSnapshot::id))
            .filter(task -> "BLOCKED".equals(task.status()))
            .filter(task -> "TECHNICAL_FAILURE".equals(task.blockerCategory()))
            .filter(task -> task.id() > correctionId);
    return java.util.stream.Stream.concat(functional.stream(), technical.stream())
        .max(Comparator.comparing(PdeValidationTaskSnapshot::id));
  }

  /** Localiza a rejeição funcional mais recente ainda sem correção válida posterior. */
  private Optional<PdeValidationTaskSnapshot> unresolvedFunctionalRejection(
      List<PdeValidationTaskSnapshot> history, String expectedVersion) {
    Optional<PdeValidationTaskSnapshot> rejection =
        history.stream()
            .filter(task -> REVIEW_ACTIVITIES.contains(task.processActivityId()))
            .filter(task -> "BLOCKED".equals(task.status()))
            .filter(task -> "FUNCTIONAL_ADJUSTMENT".equals(task.blockerCategory()))
            .max(Comparator.comparing(PdeValidationTaskSnapshot::id));
    if (rejection.isEmpty()) return Optional.empty();
    Optional<PdeValidationTaskSnapshot> correction =
        history.stream()
            .filter(task -> CORRECTION_ACTIVITY.equals(task.processActivityId()))
            .filter(task -> "COMPLETED".equals(task.status()))
            .filter(task -> validCorrection(task, expectedVersion))
            .max(Comparator.comparing(PdeValidationTaskSnapshot::id));
    return correction.filter(task -> task.id() > rejection.orElseThrow().id()).isPresent()
        ? Optional.empty()
        : rejection;
  }

  /** Confirma que Dédalo registrou versão nova e retorno obrigatório à homologação técnica. */
  private boolean validCorrection(PdeValidationTaskSnapshot task, String expectedVersion) {
    if (expectedVersion == null || task.resultJson() == null) return false;
    try {
      JsonNode result = json.readTree(task.resultJson());
      JsonNode plan = result.path("correctionPlan");
      return "READY".equals(result.path("decision").asText())
          && expectedVersion.equals(plan.path("correctedPrototypeVersion").asText())
          && !expectedVersion.equals(plan.path("previousPrototypeVersion").asText())
          && "technicalHomologation".equals(plan.path("nextActivityId").asText())
          && plan.path("verification").path("technicalRevalidationRequired").asBoolean(false)
          && plan.path("verification").path("noExternalSideEffects").asBoolean(false);
    } catch (Exception ex) {
      log.error("Falha ao ler correção PDE. taskId={}", task.id(), ex);
      return false;
    }
  }

  /** Localiza a tentativa mais recente da versão do processo, inclusive falhas ainda atuais. */
  private Optional<PdeValidationTaskSnapshot> latestCurrentProcessTask(
      List<PdeValidationTaskSnapshot> history,
      BusinessProcessDefinition process,
      String activityId) {
    return history.stream()
        .filter(task -> process.getId().equals(task.processDefinitionId()))
        .filter(task -> activityId.equals(task.processActivityId()))
        .max(Comparator.comparing(PdeValidationTaskSnapshot::id));
  }

  /** Valida decisão e versão da prova predecessora sem confiar somente no status técnico. */
  private boolean approvedForVersion(
      PdeValidationTaskSnapshot task, String activityId, String expectedPrototypeVersion) {
    try {
      JsonNode result = json.readTree(task.resultJson());
      return expectedPrototypeVersion.equals(result.path("prototypeVersion").asText())
          && EXPECTED_DECISION.get(activityId).equals(result.path("decision").asText());
    } catch (Exception ex) {
      log.error(
          "Falha ao ler prova predecessora PDE. taskId={} activityId={}",
          task.id(),
          activityId,
          ex);
      return false;
    }
  }

  /**
   * Consulta decisões e bloqueios de todas as versões do processo, sem retransmitir auditorias
   * extensas.
   */
  private List<PdeValidationTaskSnapshot> processHistory(String sourceReference) {
    if (sourceReference == null || sourceReference.isBlank()) return List.of();
    return tasks.findPdeValidationTaskSnapshots(
        sourceReference, PdeAgentValidationGateActivityExecutor.PROCESS_CODE);
  }

  /** Lê a versão do ciclo explícito ou a aceitação privada original sem misturar passagens. */
  private String expectedPrototypeVersion(Product product, String sourceReference) {
    if (product == null) return null;
    if (taskTargets != null
        && sourceReference != null
        && sourceReference.startsWith("experiment:")) {
      return taskTargets
          .resolve(sourceReference, "pde-construction-approval")
          .filter(target -> java.util.Objects.equals(product.getId(), target.productId()))
          .map(com.marketinghub.agenttask.AgentTaskTargetResponse::experienceVersion)
          .orElse(null);
    }
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
  private String rootCause(PdeValidationTaskSnapshot task) {
    if (task.resultJson() != null) {
      try {
        String value = text(json.readTree(task.resultJson()).path("rootCause").asText(null));
        if (value != null) return value;
      } catch (Exception ex) {
        log.error("Falha ao ler causa-raiz do parecer PDE. taskId={}", task.id(), ex);
      }
    }
    String error = text(task.executionError());
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

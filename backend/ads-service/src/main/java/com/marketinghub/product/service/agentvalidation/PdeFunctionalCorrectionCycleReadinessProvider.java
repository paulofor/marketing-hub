package com.marketinghub.product.service.agentvalidation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.agentactivity.AgentProductProcessActivityReadiness;
import com.marketinghub.businessprocess.execution.service.agentactivity.AgentProductProcessActivityReadinessProvider;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Responsabilidade: exigir sucessor para mudança funcional, preservando suplementos de prova. */
@Service
@Slf4j
public class PdeFunctionalCorrectionCycleReadinessProvider
    implements AgentProductProcessActivityReadinessProvider {
  private static final Set<String> REVIEW_ACTIVITIES =
      Set.of(
          "technicalHomologation",
          "psiqueAdherent",
          "psiqueRecovery",
          "psiqueSafety",
          "commercialIntegrityReview");
  private final AgentTaskRepository tasks;
  private final ObjectMapper json;

  @org.springframework.beans.factory.annotation.Autowired(required = false)
  private PdeOperationalControlEvidence operationalEvidence;

  /** Recebe a projeção das provas existentes sem executar modelo ou alterar o histórico. */
  public PdeFunctionalCorrectionCycleReadinessProvider(
      AgentTaskRepository tasks, ObjectMapper json) {
    this.tasks = tasks;
    this.json = json;
  }

  /** Compõe somente a atividade condicional de correção no contrato de validação PDE. */
  @Override
  public boolean supports(
      BusinessProcessDefinition process, BusinessProcessActivityDefinition activity) {
    return activity != null
        && PdeAgentValidationReworkReadinessProvider.CORRECTION_ACTIVITY.equals(
            activity.getActivityId())
        && PdeAgentValidationProcessContract.supports(process, json);
  }

  /** Bloqueia mudança funcional; prova nova vinculada libera somente a revisão independente. */
  @Override
  public AgentProductProcessActivityReadiness readiness(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activity,
      Product product,
      String sourceReference) {
    if (!supports(process, activity)
        || sourceReference == null
        || !sourceReference.matches("^experiment:[1-9][0-9]*$")) {
      return new AgentProductProcessActivityReadiness(
          true, "A regra de sucessor não altera este contexto.");
    }
    var history =
        tasks.findPdeValidationTaskSnapshots(
            sourceReference, PdeAgentValidationGateActivityExecutor.PROCESS_CODE);
    var latest =
        history.stream()
            .filter(
                task ->
                    "BLOCKED".equals(task.status())
                        && REVIEW_ACTIVITIES.contains(task.processActivityId()))
            .max(Comparator.comparing(PdeValidationTaskSnapshot::id));
    if (latest
        .filter(
            task ->
                "BLOCKED".equals(task.status())
                    && "FUNCTIONAL_ADJUSTMENT".equals(task.blockerCategory()))
        .isPresent()) {
      if (supersededByApproval(history, latest.orElseThrow(), product, sourceReference))
        return new AgentProductProcessActivityReadiness(
            true,
            "O novo parecer independente da mesma atividade resolveu o bloqueio; preserve o histórico.");
      if (hasBoundEvidenceSupplement(latest.orElseThrow(), product, sourceReference)) {
        return new AgentProductProcessActivityReadiness(
            true,
            "A prova complementar preserva a implementação deste ciclo. Submeta o conjunto à revisão independente afetada, sem criar sucessor ou tarefa de construção.");
      }
      return new AgentProductProcessActivityReadiness(
          false,
          "Este ciclo preserva a versão rejeitada no parecer #"
              + latest.orElseThrow().id()
              + ". Prepare um ciclo e experimento sucessores vinculados para corrigir a experiência. A implementação e suas revisões devem ocorrer na nova candidata; não abra outra chamada de correção na referência atual.");
    }
    return new AgentProductProcessActivityReadiness(
        true,
        "A ausência de rejeição funcional atual não dispensa os demais pré-requisitos da correção.");
  }

  /** Somente aprovação posterior da mesma atividade, processo e identidade supera a rejeição. */
  private boolean supersededByApproval(
      List<PdeValidationTaskSnapshot> history,
      PdeValidationTaskSnapshot rejected,
      Product product,
      String reference) {
    var approved =
        history.stream()
            .filter(task -> "COMPLETED".equals(task.status()))
            .filter(
                task ->
                    task.id() > rejected.id()
                        && java.util.Objects.equals(
                            task.processDefinitionId(), rejected.processDefinitionId())
                        && java.util.Objects.equals(
                            task.processActivityId(), rejected.processActivityId()))
            .max(Comparator.comparing(PdeValidationTaskSnapshot::id));
    if (approved.isEmpty()
        || rejected.resultJson() == null
        || approved.get().resultJson() == null
        || product == null) return false;
    try {
      var before = json.readTree(rejected.resultJson());
      var after = json.readTree(approved.get().resultJson());
      return "APPROVED".equals(after.path("decision").asText())
          && !before.path("prototypeVersion").asText().isBlank()
          && before.path("prototypeVersion").equals(after.path("prototypeVersion"))
          && product.getId() == after.path("productId").asLong(-1)
          && product.getId() == before.path("productId").asLong(-1)
          && reference.equals(after.path("sourceReference").asText())
          && reference.equals(before.path("sourceReference").asText());
    } catch (Exception ex) {
      log.warn(
          "Parecer posterior incompatível productId={} sourceReference={} taskId={}",
          product.getId(),
          reference,
          approved.get().id(),
          ex);
      return false;
    }
  }

  /** Confere o suplemento executado, posterior e vinculado aos bytes do parecer exato. */
  private boolean hasBoundEvidenceSupplement(
      PdeValidationTaskSnapshot rejected, Product product, String sourceReference) {
    if (operationalEvidence == null
        || product == null
        || rejected.updatedAt() == null
        || rejected.resultJson() == null) return false;
    try {
      var result = json.readTree(rejected.resultJson());
      String version = result.path("prototypeVersion").asText();
      if (version.isBlank()
          || product.getId() != result.path("productId").asLong(-1)
          || !sourceReference.equals(result.path("sourceReference").asText())) return false;
      var report = operationalEvidence.resolve(product.getSlug(), version);
      return report.isPresent()
          && PdeReviewEvidenceReevaluation.matches(
              report.get(), rejected.reviewProof(sourceReference), product.getId(), sourceReference)
          && java.time.Instant.parse(report.get().path("generatedAt").asText())
              .isAfter(rejected.updatedAt());
    } catch (Exception ex) {
      log.warn(
          "Suplemento de correção indisponível productId={} sourceReference={} taskId={}",
          product.getId(),
          sourceReference,
          rejected.id(),
          ex);
      return false;
    }
  }
}

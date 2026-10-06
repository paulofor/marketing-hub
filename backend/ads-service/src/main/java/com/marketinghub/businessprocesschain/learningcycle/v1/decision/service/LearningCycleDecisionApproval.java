package com.marketinghub.businessprocesschain.learningcycle.v1.decision.service;

import static com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleRules.require;

import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.businessprocesschain.learningcycle.v1.decision.LearningCycleDecisionProposal;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.command.LearningCycleCommand;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningCycleDecisionProposalRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Responsabilidade: validar a autoridade humana ou preparatória da decisão, preservando a proposta
 * vigente.
 */
@Component
@RequiredArgsConstructor
public class LearningCycleDecisionApproval {
  private final LearningCycleDecisionProposalRepository proposals;
  private final BusinessProcessActivityInstanceRepository instances;

  /** Recusa aprovação automática, proposta alheia, substituída ou vinculada a outra revisão. */
  public LearningCycleDecisionProposal validate(
      LearningSalesCycle cycle, LearningCycleCommand command) {
    return validate(cycle, command, false);
  }

  /** Aceita a política interna apenas para preparação sem gasto, nunca pelo comando público. */
  public LearningCycleDecisionProposal validate(
      LearningSalesCycle cycle, LearningCycleCommand command, boolean preparation) {
    if (!"DECISION".equals(cycle.getStage())) return null;
    var proposal =
        proposals
            .findFirstByCycleIdAndCycleRevisionOrderByAttemptDesc(
                cycle.getId(), cycle.getRevision())
            .orElse(null);
    require(
        proposal != null && "READY".equals(proposal.getStatus()),
        "Aguarde uma proposta válida de Atena antes de aprovar a decisão.");
    if (preparation) {
      require(
          "ADJUST".equals(command.action().name())
              && LearningCyclePreparationPolicy.eligible(command.evidence())
              && command.evidence().path("decisionProposalId").asLong(-1) == proposal.getId()
              && !command.evidence().path("humanApproved").asBoolean(false),
          "A preparação automática exige proposta vigente de ajuste no mesmo foco, sem autorização humana simulada.");
      return proposal;
    }
    require(
        command.evidence().path("decisionProposalId").isIntegralNumber()
            && command.evidence().path("decisionProposalId").asLong(-1) == proposal.getId()
            && command.evidence().path("humanApproved").isBoolean()
            && command.evidence().path("humanApproved").asBoolean(),
        "Aprovação humana explícita e referência à proposta vigente são obrigatórias.");
    require(
        !command.operatorName().trim().equalsIgnoreCase("Atena"),
        "Informe o responsável humano declarado pela aprovação, distinto da autora Atena.");
    return proposal;
  }

  /**
   * Liga a decisão editada à proposta original e conclui a ocorrência assistida na mesma transação.
   */
  public void record(
      LearningCycleDecisionProposal proposal, Long eventId, String finalDecision, Instant now) {
    record(proposal, eventId, finalDecision, now, false);
  }

  /** Registra separadamente a política automática e a aprovação humana, sem apagar o parecer. */
  public void record(
      LearningCycleDecisionProposal proposal,
      Long eventId,
      String finalDecision,
      Instant now,
      boolean preparation) {
    if (proposal == null) return;
    proposal.setStatus("APPROVED");
    proposal.setApprovedAt(now);
    proposal.setApprovedEventId(eventId);
    proposals.save(proposal);
    var instance = instances.findById(proposal.getActivityInstanceId()).orElseThrow();
    instance.setStatus("COMPLETED");
    instance.setObjectiveAchieved(true);
    instance.setEvidenceQuality(preparation ? "AUTOMATIC" : "HUMAN_RECORDED");
    instance.setObjectiveEvidenceJson(finalDecision);
    instance.setExitedAt(now);
    instance.setUpdatedAt(now);
    instances.save(instance);
  }
}

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

/** Responsabilidade: exigir proposta vigente e aprovação humana antes de uma decisão comercial. */
@Component
@RequiredArgsConstructor
public class LearningCycleDecisionApproval {
  private final LearningCycleDecisionProposalRepository proposals;
  private final BusinessProcessActivityInstanceRepository instances;

  /** Recusa aprovação automática, proposta alheia, substituída ou vinculada a outra revisão. */
  public LearningCycleDecisionProposal validate(
      LearningSalesCycle cycle, LearningCycleCommand command) {
    if (!"DECISION".equals(cycle.getStage())) return null;
    var proposal =
        proposals
            .findFirstByCycleIdAndCycleRevisionOrderByAttemptDesc(
                cycle.getId(), cycle.getRevision())
            .orElse(null);
    require(
        proposal != null && "READY".equals(proposal.getStatus()),
        "Aguarde uma proposta válida de Atena antes de aprovar a decisão.");
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
    if (proposal == null) return;
    proposal.setStatus("APPROVED");
    proposal.setApprovedAt(now);
    proposal.setApprovedEventId(eventId);
    proposals.save(proposal);
    var instance = instances.findById(proposal.getActivityInstanceId()).orElseThrow();
    instance.setStatus("COMPLETED");
    instance.setObjectiveAchieved(true);
    instance.setEvidenceQuality("HUMAN_RECORDED");
    instance.setObjectiveEvidenceJson(finalDecision);
    instance.setExitedAt(now);
    instance.setUpdatedAt(now);
    instances.save(instance);
  }
}

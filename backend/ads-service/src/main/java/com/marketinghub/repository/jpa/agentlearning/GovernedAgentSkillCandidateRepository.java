package com.marketinghub.repository.jpa.agentlearning;

import com.marketinghub.agentlearning.v1.GovernedAgentSkillCandidate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: persistir skills candidatas e localizar sua versão ativa. */
public interface GovernedAgentSkillCandidateRepository
    extends JpaRepository<GovernedAgentSkillCandidate, Long> {
  /** Localiza a skill vinculada ao experimento. */
  Optional<GovernedAgentSkillCandidate> findByExperimentId(Long experimentId);

  /** Lista o histórico recente do agente. */
  List<GovernedAgentSkillCandidate> findByAgentKeyOrderByIdDesc(String agentKey);
}

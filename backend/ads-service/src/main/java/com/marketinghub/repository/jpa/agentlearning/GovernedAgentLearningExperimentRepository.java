package com.marketinghub.repository.jpa.agentlearning;

import com.marketinghub.agentlearning.v1.GovernedAgentLearningExperiment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: persistir comparações governadas de aprendizado dos agentes. */
public interface GovernedAgentLearningExperimentRepository
    extends JpaRepository<GovernedAgentLearningExperiment, Long> {
  /** Localiza uma candidata já congelada para manter criação automática idempotente. */
  java.util.Optional<GovernedAgentLearningExperiment> findByAgentKeyAndCandidateVersion(
      String agentKey, String candidateVersion);

  /** Lista os experimentos recentes de um agente para o painel operacional. */
  List<GovernedAgentLearningExperiment> findByAgentKeyOrderByIdDesc(String agentKey);

  /** Lista estratégias promovidas no escopo, da mais recente para a mais antiga. */
  List<GovernedAgentLearningExperiment> findByAgentKeyAndScopeTypeAndScopeIdAndStatusOrderByIdDesc(
      String agentKey, String scopeType, String scopeId, String status);
}

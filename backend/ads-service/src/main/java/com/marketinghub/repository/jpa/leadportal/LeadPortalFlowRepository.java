package com.marketinghub.repository.jpa.leadportal;

import com.marketinghub.leadportal.LeadPortalFlow;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: acessar os fluxos do Lead Portal com associações de leitura seguras. */
public interface LeadPortalFlowRepository extends JpaRepository<LeadPortalFlow, Long> {
  /** Lista todos os fluxos sem multiplicar perguntas pelas opções carregadas. */
  @Override
  @EntityGraph(attributePaths = {"questions", "experiment", "marketNiche", "simpleFormStyle"})
  List<LeadPortalFlow> findAll();

  /** Lista os fluxos por nome sem multiplicar perguntas pelas opções carregadas. */
  @EntityGraph(attributePaths = {"questions", "experiment", "marketNiche", "simpleFormStyle"})
  List<LeadPortalFlow> findAllByOrderByNameAsc();

  /** Lista os fluxos de um experimento sem produto cartesiano entre perguntas e opções. */
  @EntityGraph(attributePaths = {"questions", "experiment", "marketNiche", "simpleFormStyle"})
  List<LeadPortalFlow> findAllByExperimentIdOrderByCreatedAtDesc(Long experimentId);

  /** Lista os fluxos do nicho preservando uma ocorrência de cada pergunta. */
  @EntityGraph(attributePaths = {"questions", "experiment", "marketNiche", "simpleFormStyle"})
  List<LeadPortalFlow> findAllByMarketNicheIdOrderByCreatedAtDesc(Long marketNicheId);

  /** Lista fluxos aprovados sem multiplicar perguntas pelas opções. */
  @EntityGraph(attributePaths = {"questions", "experiment", "marketNiche", "simpleFormStyle"})
  List<LeadPortalFlow> findAllByApprovedTrue();

  /** Busca o fluxo pelo slug preservando a cardinalidade real das perguntas. */
  @EntityGraph(attributePaths = {"questions", "experiment", "marketNiche", "simpleFormStyle"})
  Optional<LeadPortalFlow> findBySlug(String slug);
}

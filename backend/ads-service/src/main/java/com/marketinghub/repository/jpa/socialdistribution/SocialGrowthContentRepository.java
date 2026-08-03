package com.marketinghub.repository.jpa.socialdistribution;

import com.marketinghub.socialdistribution.SocialGrowthContent;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: persistir pautas rastreáveis dos planos orgânicos. */
public interface SocialGrowthContentRepository extends JpaRepository<SocialGrowthContent, Long> {
  /** Lista as pautas de um plano na ordem editorial. */
  List<SocialGrowthContent> findByPlanIdOrderByPlannedAtAscCreatedAtAsc(Long planId);

  /** Localiza a pauta vinculada a uma publicação operacional. */
  Optional<SocialGrowthContent> findByPublicationId(Long publicationId);
}

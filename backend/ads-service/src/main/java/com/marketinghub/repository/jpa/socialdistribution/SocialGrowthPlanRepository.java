package com.marketinghub.repository.jpa.socialdistribution;

import com.marketinghub.socialdistribution.SocialGrowthPlan;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: persistir planos comerciais de crescimento orgânico. */
public interface SocialGrowthPlanRepository extends JpaRepository<SocialGrowthPlan, Long> {
  /** Lista planos recentes, opcionalmente filtrados por produto. */
  List<SocialGrowthPlan> findTop50ByProductIdOrderByCreatedAtDesc(Long productId);

  /** Lista os planos mais recentes de todos os produtos. */
  List<SocialGrowthPlan> findTop50ByOrderByCreatedAtDesc();
}

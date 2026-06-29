package com.marketinghub.repository.jpa.planning;

import com.marketinghub.planning.CommercialPlanMilestone;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: persistir e consultar marcos de planos comerciais. */
public interface CommercialPlanMilestoneRepository extends JpaRepository<CommercialPlanMilestone, Long> {
    /** Lista os marcos de um plano na ordem comercial definida. */
    List<CommercialPlanMilestone> findByPlanIdOrderBySequenceOrderAsc(Long planId);
}

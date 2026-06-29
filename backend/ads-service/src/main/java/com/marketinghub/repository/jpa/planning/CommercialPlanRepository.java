package com.marketinghub.repository.jpa.planning;

import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.CommercialPlanStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: persistir e consultar planos comerciais. */
public interface CommercialPlanRepository extends JpaRepository<CommercialPlan, Long> {
    /** Lista planos comerciais por status, priorizando os mais recentes. */
    List<CommercialPlan> findByStatusOrderByUpdatedAtDesc(CommercialPlanStatus status);
}

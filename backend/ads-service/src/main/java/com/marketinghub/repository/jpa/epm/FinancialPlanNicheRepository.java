package com.marketinghub.repository.jpa.epm;

import com.marketinghub.epm.FinancialPlanNiche;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório JPA para persistir e consultar registros FinancialPlanNiche do EPM. */
public interface FinancialPlanNicheRepository extends JpaRepository<FinancialPlanNiche, Long> {
  /** Lista os nichos financeiros vinculados ao plano informado. */
  List<FinancialPlanNiche> findByFinancialPlanIdOrderByIdAsc(Long financialPlanId);
}

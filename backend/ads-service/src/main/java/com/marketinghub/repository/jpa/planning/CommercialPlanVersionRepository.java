package com.marketinghub.repository.jpa.planning;

import com.marketinghub.planning.CommercialPlanVersion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: persistir e consultar versões imutáveis dos planos comerciais. */
public interface CommercialPlanVersionRepository
    extends JpaRepository<CommercialPlanVersion, Long> {
  /** Lista o histórico do plano da versão mais recente para a mais antiga. */
  List<CommercialPlanVersion> findByPlanIdOrderByVersionNumberDesc(Long planId);

  /** Recupera a versão atual persistida do plano. */
  Optional<CommercialPlanVersion> findTopByPlanIdOrderByVersionNumberDesc(Long planId);
}

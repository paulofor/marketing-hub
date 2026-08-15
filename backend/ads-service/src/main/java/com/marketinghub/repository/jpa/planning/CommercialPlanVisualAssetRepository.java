package com.marketinghub.repository.jpa.planning;

import com.marketinghub.planning.CommercialPlanVisualAsset;
import com.marketinghub.planning.CommercialPlanVisualAssetStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: persistir e consultar o kit visual dos planos comerciais. */
public interface CommercialPlanVisualAssetRepository
    extends JpaRepository<CommercialPlanVisualAsset, Long> {
  /** Lista o kit completo na ordem de cadastro. */
  List<CommercialPlanVisualAsset> findByCommercialPlanIdOrderByCreatedAtAsc(Long planId);

  /** Lista somente referências aprovadas para consumo dos executores. */
  List<CommercialPlanVisualAsset> findByCommercialPlanIdAndStatusOrderByCreatedAtAsc(
      Long planId, CommercialPlanVisualAssetStatus status);

  /** Calcula a próxima versão da mesma referência dentro do plano. */
  long countByCommercialPlanIdAndAssetUrl(Long planId, String assetUrl);
}

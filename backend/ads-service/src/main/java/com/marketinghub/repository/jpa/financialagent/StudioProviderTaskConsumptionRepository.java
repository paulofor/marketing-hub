package com.marketinghub.repository.jpa.financialagent;

import com.marketinghub.financialagent.StudioProviderTaskConsumption;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Responsabilidade: persistir e consolidar consumos cobrados por task/cena de provedor. */
public interface StudioProviderTaskConsumptionRepository
    extends JpaRepository<StudioProviderTaskConsumption, Long> {
  /** Localiza uma task pela identidade idempotente fornecida pelo provedor. */
  Optional<StudioProviderTaskConsumption> findByProviderAndProviderTaskId(
      String provider, String providerTaskId);

  /** Lista as tasks aceitas de um job para auditoria e conciliação. */
  List<StudioProviderTaskConsumption> findBySalesVideoJobIdOrderBySceneNumberAsc(Long jobId);

  /** Soma o custo estimado das tasks aceitas, inclusive quando o job final falhar. */
  @Query(
      "select coalesce(sum(t.estimatedCostUsd), 0) from StudioProviderTaskConsumption t where t.salesVideoJobId = :jobId")
  BigDecimal sumEstimatedCostUsdBySalesVideoJobId(Long jobId);
}

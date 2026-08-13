package com.marketinghub.repository.jpa.financialagent;

import com.marketinghub.financialagent.StudioProviderTaskConsumption;
import java.math.BigDecimal;
import java.util.Collection;
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

  /** Lista as tasks de vários jobs na ordem editorial e cronológica. */
  List<StudioProviderTaskConsumption> findBySalesVideoJobIdInOrderBySceneNumberAscAcceptedAtAsc(
      Collection<Long> jobIds);

  /** Soma o custo estimado das tasks aceitas, inclusive quando o job final falhar. */
  @Query(
      "select coalesce(sum(t.estimatedCostUsd), 0) from StudioProviderTaskConsumption t where t.salesVideoJobId = :jobId")
  BigDecimal sumEstimatedCostUsdBySalesVideoJobId(Long jobId);

  /** Soma somente custos liquidados, sem tratar tasks pendentes como custo zero. */
  @Query(
      "select coalesce(sum(t.billedCostUsd), 0) from StudioProviderTaskConsumption t where t.salesVideoJobId = :jobId")
  BigDecimal sumBilledCostUsdBySalesVideoJobId(Long jobId);

  /** Conta tasks ainda sem desfecho financeiro conhecido. */
  long countBySalesVideoJobIdAndSettlementStatusIsNull(Long jobId);
}

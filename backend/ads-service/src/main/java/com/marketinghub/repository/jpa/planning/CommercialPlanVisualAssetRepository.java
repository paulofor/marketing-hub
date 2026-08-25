package com.marketinghub.repository.jpa.planning;

import com.marketinghub.planning.CommercialPlanVisualAsset;
import com.marketinghub.planning.CommercialPlanVisualAssetStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

  /** Localiza uma importação já concluída para tornar o envio do pacote idempotente. */
  List<CommercialPlanVisualAsset> findByCommercialPlanIdAndCreativePackageIdOrderByCreatedAtAsc(
      Long planId, String creativePackageId);

  /** Lista entregáveis gerados que aguardam parecer independente. */
  List<CommercialPlanVisualAsset> findByAgentReviewStatusOrderByCreatedAtAsc(
      com.marketinghub.planning.imagestudio.v1.CommercialPlanVisualAssetReviewStatus status);

  /** Reserva pareceres pendentes e recupera revisões cuja lease expirou. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select a from CommercialPlanVisualAsset a where a.agentReviewStatus = :pending"
          + " or (a.agentReviewStatus = :processing and a.agentReviewStartedAt < :cutoff)"
          + " order by a.createdAt asc")
  List<CommercialPlanVisualAsset> findClaimableReviews(
      @Param("pending")
          com.marketinghub.planning.imagestudio.v1.CommercialPlanVisualAssetReviewStatus pending,
      @Param("processing")
          com.marketinghub.planning.imagestudio.v1.CommercialPlanVisualAssetReviewStatus processing,
      @Param("cutoff") Instant cutoff);
}

package com.marketinghub.repository.jpa.planning;

import com.marketinghub.planning.imagestudio.v1.CommercialPlanImageStudioJob;
import com.marketinghub.planning.imagestudio.v1.CommercialPlanImageStudioStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Responsabilidade: persistir e consultar a fila do Estúdio de Imagens de Têmis. */
public interface CommercialPlanImageStudioJobRepository
    extends JpaRepository<CommercialPlanImageStudioJob, Long> {
  /** Lista jobs do plano na ordem mais recente. */
  List<CommercialPlanImageStudioJob> findByCommercialPlanIdOrderByCreatedAtDesc(Long planId);

  /** Lista a fila de produção na ordem canônica. */
  List<CommercialPlanImageStudioJob> findByStatusOrderByCreatedAtAsc(
      CommercialPlanImageStudioStatus status);

  /** Reserva pendências e recupera produções cuja lease expirou. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select j from CommercialPlanImageStudioJob j where j.status = :pending"
          + " or (j.status = :processing and j.startedAt < :cutoff) order by j.createdAt asc")
  List<CommercialPlanImageStudioJob> findClaimable(
      @Param("pending") CommercialPlanImageStudioStatus pending,
      @Param("processing") CommercialPlanImageStudioStatus processing,
      @Param("cutoff") Instant cutoff);

  /** Informa se o asset foi materializado por uma execução governada do estúdio. */
  boolean existsByResultVisualAssetId(Long assetId);

  /** Localiza o job produtor pelo entregável resultante. */
  Optional<CommercialPlanImageStudioJob> findByResultVisualAssetId(Long assetId);
}

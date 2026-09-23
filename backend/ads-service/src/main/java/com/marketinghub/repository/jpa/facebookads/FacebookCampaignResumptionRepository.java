package com.marketinghub.repository.jpa.facebookads;

import com.marketinghub.facebookads.resumption.FacebookCampaignResumption;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

/** Consulta e serializa autorizações de retomada, preservando todas as execuções anteriores. */
public interface FacebookCampaignResumptionRepository
    extends JpaRepository<FacebookCampaignResumption, Long> {
  /** Recupera a autorização mais recente do experimento. */
  Optional<FacebookCampaignResumption> findFirstByExperimentIdOrderByIdDesc(Long experimentId);

  /** Lista trabalho pendente ou abandonado com limite e filtro no banco. */
  @Query(
      "select r from FacebookCampaignResumption r where "
          + "(r.status = 'PENDING' and (r.startDate is null or r.startDate <= :today)) "
          + "or (r.status = 'RUNNING' and r.leaseUntil < :now) order by r.id")
  List<FacebookCampaignResumption> findPending(
      @Param("now") Instant now, @Param("today") java.time.LocalDate today, Pageable pageable);

  /** Impede reservas e callbacks concorrentes de alterarem a mesma execução. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select r from FacebookCampaignResumption r where r.id = :id")
  Optional<FacebookCampaignResumption> findLocked(@Param("id") Long id);
}

package com.marketinghub.repository.jpa.gerasalespage.v1;

import com.marketinghub.gerasalespage.v1.GeraSalesPagePublicationStageAudit;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Responsabilidade: consultar snapshots de etapas usadas por uma pagina de venda publicada. */
public interface GeraSalesPagePublicationStageAuditRepository
    extends JpaRepository<GeraSalesPagePublicationStageAudit, Long> {
  /** Lista etapas auditadas de uma publicacao na ordem do pipeline. */
  List<GeraSalesPagePublicationStageAudit> findByPublicationAuditIdOrderByStageOrderAsc(
      Long publicationAuditId);

  /** Soma custos em USD das etapas publicadas do GeraSalesPage para um experimento. */
  @Query(
      """
            select coalesce(sum(s.costUsd), 0)
              from GeraSalesPagePublicationStageAudit s
              join s.publicationAudit p
             where p.experimentId = :experimentId
            """)
  BigDecimal sumCostUsdByExperimentId(@Param("experimentId") Long experimentId);
}

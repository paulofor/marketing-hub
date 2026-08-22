package com.marketinghub.repository.jpa.experiment.funnel;

import com.marketinghub.experiment.funnel.ExperimentFunnelEvent;
import com.marketinghub.experiment.funnel.ExperimentFunnelStage;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repositório de eventos do funil do experimento. */
public interface ExperimentFunnelEventRepository
    extends JpaRepository<ExperimentFunnelEvent, Long> {

  String RENDER_COMPLETE_SOURCE = "lead-portal-render-complete";
  String LANDING_PAGE_ANALYTICS_SOURCE = "landing-page-analytics";
  String SUBMISSION_SOURCE = "lead_portal_submission";

  boolean existsByExperimentIdAndStageAndSourceAndPayload(
      Long experimentId, ExperimentFunnelStage stage, String source, String payload);

  @Query(
      """
            select e.stage as stage,
                   count(e.id) as total,
                   count(distinct e.lead) as uniqueLeads,
                   max(e.occurredAt) as lastEvent
            from ExperimentFunnelEvent e
            where e.experiment.id = :experimentId
              and (e.source is null or lower(trim(e.source)) = 'manual')
              and (:baseline is null or e.occurredAt > :baseline)
            group by e.stage
            """)
  List<StageAggregation> aggregateManualByExperiment(
      @Param("experimentId") Long experimentId, @Param("baseline") Instant baseline);

  /**
   * Busca os eventos normalizados de analytics da landing respeitando o marco temporal do funil.
   */
  @Query(
      """
            select normalized.id as id,
                   normalized.funnelEvent.payload as payload,
                   normalized.occurredAt as occurredAt
            from ExperimentLandingAnalyticsEvent normalized
            where normalized.experiment.id = :experimentId
              and normalized.funnelEvent.source = :source
              and (:baseline is null or normalized.occurredAt > :baseline)
            order by normalized.occurredAt desc, normalized.id desc
            """)
  List<LandingAnalyticsEventProjection> findLandingAnalyticsEvents(
      @Param("experimentId") Long experimentId,
      @Param("source") String source,
      @Param("baseline") Instant baseline,
      Pageable pageable);

  /** Conta os eventos normalizados disponíveis no mesmo escopo temporal do analytics. */
  @Query(
      """
            select count(normalized.id)
            from ExperimentLandingAnalyticsEvent normalized
            where normalized.experiment.id = :experimentId
              and normalized.funnelEvent.source = :source
              and (:baseline is null or normalized.occurredAt > :baseline)
            """)
  long countLandingAnalyticsEvents(
      @Param("experimentId") Long experimentId,
      @Param("source") String source,
      @Param("baseline") Instant baseline);

  /**
   * Apaga fisicamente os eventos de analytics da landing para remover sessões de teste do banco.
   */
  @Modifying
  @Query(
      "delete from ExperimentFunnelEvent e where e.experiment.id = :experimentId and e.source = :source")
  int deleteByExperimentIdAndSource(
      @Param("experimentId") Long experimentId, @Param("source") String source);

  /** Apaga fisicamente todos os eventos restantes do funil de um experimento. */
  @Modifying
  @Query("delete from ExperimentFunnelEvent e where e.experiment.id = :experimentId")
  int deleteByExperimentId(@Param("experimentId") Long experimentId);

  interface StageAggregation {
    ExperimentFunnelStage getStage();

    long getTotal();

    Long getUniqueLeads();

    Instant getLastEvent();
  }

  /**
   * Projeção mínima para transportar eventos de analytics da landing sem carregar entidades
   * completas.
   */
  interface LandingAnalyticsEventProjection {

    /** Retorna o identificador do evento para ordenação estável. */
    Long getId();

    /** Retorna o payload textual registrado pelo endpoint público de analytics. */
    String getPayload();

    /** Retorna o instante em que o evento ocorreu. */
    Instant getOccurredAt();
  }
}

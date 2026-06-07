package com.marketinghub.repository.jpa.experiment.funnel;

import com.marketinghub.experiment.funnel.ExperimentLandingAnalyticsEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repositório de eventos normalizados de analytics da landing do experimento.
 */
public interface ExperimentLandingAnalyticsEventRepository extends JpaRepository<ExperimentLandingAnalyticsEvent, Long> {

    /**
     * Busca evento normalizado já registrado para o mesmo identificador público de evento.
     */
    Optional<ExperimentLandingAnalyticsEvent> findFirstByExperimentIdAndEventId(Long experimentId, String eventId);

    /**
     * Verifica page_view duplicado dentro da janela operacional canônica de deduplicação.
     */
    @Query("""
            select case when count(e.id) > 0 then true else false end
            from ExperimentLandingAnalyticsEvent e
            where e.experiment.id = :experimentId
              and e.visitorId = :visitorId
              and e.sessionId = :sessionId
              and lower(e.eventType) = lower(:eventType)
              and e.pageUrl = :pageUrl
              and e.occurredAt between :windowStart and :windowEnd
            """)
    boolean existsPageViewInDeduplicationWindow(@Param("experimentId") Long experimentId,
                                                @Param("visitorId") String visitorId,
                                                @Param("sessionId") String sessionId,
                                                @Param("eventType") String eventType,
                                                @Param("pageUrl") String pageUrl,
                                                @Param("windowStart") Instant windowStart,
                                                @Param("windowEnd") Instant windowEnd);

    /**
     * Agrega page_views normalizados por visitante provável para análise de recorrência.
     */
    @Query(value = """
            SELECT e.visitor_id AS visitorId,
                   COUNT(DISTINCT e.session_id) AS totalSessions,
                   SUM(CASE WHEN LOWER(e.event_type) = 'page_view' THEN 1 ELSE 0 END) AS validPageViews,
                   MIN(e.occurred_at) AS firstAccessAt,
                   MAX(e.occurred_at) AS lastAccessAt,
                   COUNT(DISTINCT CASE
                       WHEN LOWER(e.event_type) = 'page_view' AND e.page_url IS NOT NULL AND e.page_url <> ''
                       THEN e.page_url
                       ELSE NULL
                   END) AS distinctPages,
                   (
                       SELECT e2.user_agent
                       FROM experiment_landing_analytics_event e2
                       WHERE e2.experiment_id = e.experiment_id
                         AND e2.visitor_id = e.visitor_id
                         AND (:baseline IS NULL OR e2.occurred_at > :baseline)
                       ORDER BY e2.occurred_at DESC, e2.id DESC
                       LIMIT 1
                   ) AS lastUserAgent
            FROM experiment_landing_analytics_event e
            WHERE e.experiment_id = :experimentId
              AND e.visitor_id IS NOT NULL
              AND e.visitor_id <> ''
              AND LOWER(e.event_type) = 'page_view'
              AND (:baseline IS NULL OR e.occurred_at > :baseline)
            GROUP BY e.experiment_id, e.visitor_id
            ORDER BY MAX(e.occurred_at) DESC
            """, nativeQuery = true)
    List<VisitorRecurrenceProjection> aggregateVisitorsByExperiment(@Param("experimentId") Long experimentId,
                                                                    @Param("baseline") Instant baseline);

    /**
     * Projeção agregada de recorrência por visitante provável da landing.
     */
    interface VisitorRecurrenceProjection {

        /**
         * Retorna o identificador first-party bruto usado apenas para mascaramento de resposta.
         */
        String getVisitorId();

        /**
         * Retorna a quantidade de sessões distintas associadas ao visitante provável.
         */
        long getTotalSessions();

        /**
         * Retorna a quantidade de page_views válidos após deduplicação operacional.
         */
        long getValidPageViews();

        /**
         * Retorna o primeiro acesso válido do visitante provável.
         */
        Instant getFirstAccessAt();

        /**
         * Retorna o último acesso válido do visitante provável.
         */
        Instant getLastAccessAt();

        /**
         * Retorna a quantidade de páginas distintas visualizadas pelo visitante provável.
         */
        long getDistinctPages();

        /**
         * Retorna o último user-agent observado para diagnóstico de dispositivo.
         */
        String getLastUserAgent();
    }

    /**
     * Apaga eventos normalizados de analytics de um experimento antes de remover os eventos brutos vinculados.
     */
    @Modifying
    @Query("delete from ExperimentLandingAnalyticsEvent e where e.experiment.id = :experimentId")
    int deleteByExperimentId(@Param("experimentId") Long experimentId);
}

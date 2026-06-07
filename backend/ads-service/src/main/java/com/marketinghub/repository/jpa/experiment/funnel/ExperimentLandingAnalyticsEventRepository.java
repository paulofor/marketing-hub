package com.marketinghub.repository.jpa.experiment.funnel;

import com.marketinghub.experiment.funnel.ExperimentLandingAnalyticsEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
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
     * Apaga eventos normalizados de analytics de um experimento antes de remover os eventos brutos vinculados.
     */
    @Modifying
    @Query("delete from ExperimentLandingAnalyticsEvent e where e.experiment.id = :experimentId")
    int deleteByExperimentId(@Param("experimentId") Long experimentId);
}

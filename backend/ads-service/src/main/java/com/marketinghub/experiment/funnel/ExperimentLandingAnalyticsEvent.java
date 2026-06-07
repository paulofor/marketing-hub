package com.marketinghub.experiment.funnel;

import com.marketinghub.experiment.Experiment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Evento normalizado de analytics da landing usado para consultas recorrentes por visitante provável.
 */
@Entity
@Table(name = "experiment_landing_analytics_event")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentLandingAnalyticsEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "experiment_id", nullable = false)
    private Experiment experiment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "funnel_event_id", nullable = false)
    private ExperimentFunnelEvent funnelEvent;

    @Column(name = "event_id", length = 128)
    private String eventId;

    @Column(name = "visitor_id", length = 128)
    private String visitorId;

    @Column(name = "session_id", length = 128)
    private String sessionId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "section_id", length = 190)
    private String sectionId;

    @Column(name = "page_url", length = 2048)
    private String pageUrl;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}

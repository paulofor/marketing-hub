package com.marketinghub.facebookads;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Registra cada passo executado pelo worker na publicação de uma campanha de experimento.
 */
@Entity
@Table(name = "facebook_campaign_publication_job_step")
@Getter
@Setter
@NoArgsConstructor
public class FacebookCampaignPublicationJobStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", length = 64, nullable = false)
    private String jobId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "experiment_id")
    private Experiment experiment;

    @Column(name = "step_name", length = 120, nullable = false)
    private String stepName;

    @Column(name = "provider", length = 32, nullable = false)
    private String provider;

    @Column(name = "endpoint", columnDefinition = "LONGTEXT")
    private String endpoint;

    @Column(name = "http_method", length = 16)
    private String httpMethod;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "request_payload", columnDefinition = "LONGTEXT")
    private String requestPayload;

    @Column(name = "response_payload", columnDefinition = "LONGTEXT")
    private String responsePayload;

    @Column(name = "error_message", columnDefinition = "LONGTEXT")
    private String errorMessage;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}

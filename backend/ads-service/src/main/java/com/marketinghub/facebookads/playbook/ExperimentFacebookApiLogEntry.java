package com.marketinghub.facebookads.playbook;

import com.marketinghub.experiment.Experiment;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "experiment_facebook_api_log")
@Getter
@Setter
@NoArgsConstructor
public class ExperimentFacebookApiLogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "experiment_id", nullable = false)
    private Experiment experiment;

    @Enumerated(EnumType.STRING)
    @Column(length = 64, nullable = false)
    private ExperimentFacebookApiLogContext context;

    @Column(length = 32, nullable = false)
    private String provider;

    @Column(columnDefinition = "LONGTEXT")
    private String endpoint;

    @Column(name = "http_method", length = 16)
    private String httpMethod;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "requested_at")
    private Instant requestedAt;

    @Column(name = "responded_at")
    private Instant respondedAt;

    @Column(name = "request_payload", columnDefinition = "LONGTEXT")
    private String requestPayload;

    @Column(name = "response_payload", columnDefinition = "LONGTEXT")
    private String responsePayload;

    @Column(name = "error_message", columnDefinition = "LONGTEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}

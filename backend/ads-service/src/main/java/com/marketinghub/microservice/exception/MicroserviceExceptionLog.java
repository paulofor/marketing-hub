package com.marketinghub.microservice.exception;

import com.marketinghub.microservice.Microservice;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "microservice_exception_log", indexes = {
        @Index(name = "idx_microservice_exception_microservice", columnList = "microservice_id, occurred_at DESC"),
        @Index(name = "idx_microservice_exception_severity", columnList = "severity")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MicroserviceExceptionLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "microservice_id", nullable = false)
    private Microservice microservice;

    @Column(name = "exception_type", length = 255)
    private String exceptionType;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String message;

    @Lob
    @Column(name = "stack_trace", columnDefinition = "LONGTEXT")
    private String stackTrace;

    @Column(length = 20)
    private String severity;

    @Column(name = "service_version", length = 100)
    private String serviceVersion;

    @Column(length = 255)
    private String hostname;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String context;

    @Column(name = "occurred_at")
    private Instant occurredAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}

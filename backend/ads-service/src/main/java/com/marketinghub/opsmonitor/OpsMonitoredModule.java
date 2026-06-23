package com.marketinghub.opsmonitor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** Representa um módulo monitorado operacionalmente pelo Marketing Hub. */
@Entity
@Table(name = "ops_monitored_module")
@Getter
@Setter
public class OpsMonitoredModule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 100)
    private String code;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "type", nullable = false, length = 50)
    private String type;

    @Column(name = "base_url", nullable = false, length = 512)
    private String baseUrl;

    @Column(name = "health_path", nullable = false, length = 255)
    private String healthPath;

    @Column(name = "log_path", length = 255)
    private String logPath;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "criticality", nullable = false, length = 30)
    private String criticality;

    @Column(name = "offline_threshold_seconds", nullable = false)
    private Integer offlineThresholdSeconds;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

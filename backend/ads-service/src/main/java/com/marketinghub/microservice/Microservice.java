package com.marketinghub.microservice;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Entity representing an internal or external microservice managed by the Marketing Hub.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Microservice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Lob
    private String description;

    @Column(name = "base_url", length = 512)
    private String baseUrl;

    @Column(length = 100)
    private String category;

    @Column(length = 50)
    private String status;

    @Column(length = 255)
    private String owner;

    @Column(name = "documentation_url", length = 512)
    private String documentationUrl;

    @Column(name = "health_check_path", length = 255)
    private String healthCheckPath;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}

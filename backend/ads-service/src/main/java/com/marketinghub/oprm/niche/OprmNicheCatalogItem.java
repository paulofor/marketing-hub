package com.marketinghub.oprm.niche;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "oprm_niche_catalog", uniqueConstraints = {
        @UniqueConstraint(name = "uk_oprm_niche_catalog_code", columnNames = "cnae_code")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OprmNicheCatalogItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cnae_code", nullable = false, length = 16)
    private String cnaeCode;

    @Column(name = "cnae_label", nullable = false, length = 255)
    private String cnaeLabel;

    @Column(name = "source", nullable = false, length = 64)
    private String source;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}

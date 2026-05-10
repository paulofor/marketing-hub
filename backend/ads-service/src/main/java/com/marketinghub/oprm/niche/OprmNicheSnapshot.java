package com.marketinghub.oprm.niche;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.*;

@Entity
@Table(name = "oprm_niche_snapshot", uniqueConstraints = {
        @UniqueConstraint(name = "uk_oprm_niche_snapshot_key", columnNames = {
                "snapshot_date", "source", "cnae_code", "uf", "municipio"
        })
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OprmNicheSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(nullable = false, length = 64)
    private String source;

    @Column(name = "cnae_code", nullable = false, length = 16)
    private String cnaeCode;

    @Column(nullable = false, length = 2)
    private String uf;

    @Column(nullable = false, length = 128)
    private String municipio;

    @Column(name = "mei_active", nullable = false)
    private Integer meiActive;

    @Column(nullable = false)
    private Integer openings;

    @Column(nullable = false)
    private Integer closures;

    @Column(nullable = false)
    private Integer net;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}

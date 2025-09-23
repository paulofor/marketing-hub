package com.marketinghub.media;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Media asset stored in the system.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Asset {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private AssetType type;

    @Enumerated(EnumType.STRING)
    private MediaProvider provider;

    private String externalId;

    @Enumerated(EnumType.STRING)
    private AssetStatus status;

    private String url;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String payload;

    private Long campaignId;

    private String model;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String prompt;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}

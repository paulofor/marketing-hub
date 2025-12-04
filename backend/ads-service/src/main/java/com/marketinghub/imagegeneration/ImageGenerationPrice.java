package com.marketinghub.imagegeneration;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Price entry for a given size/orientation inside a quality tier.
 */
@Entity
@Table(name = "image_generation_price")
@Data
public class ImageGenerationPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quality_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ImageGenerationQuality quality;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ImageOrientation orientation;

    @Column(nullable = false)
    private int width;

    @Column(nullable = false)
    private int height;

    @Column(name = "size_label", nullable = false, length = 32)
    private String sizeLabel;

    @Column(name = "unit_price_usd", nullable = false, precision = 10, scale = 5)
    private BigDecimal unitPriceUsd;

    @Column(nullable = false)
    private boolean preferred;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}

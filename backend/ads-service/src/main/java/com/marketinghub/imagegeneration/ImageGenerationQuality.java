package com.marketinghub.imagegeneration;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Describes a quality tier available for a generation model.
 */
@Entity
@Table(name = "image_generation_quality")
@Data
public class ImageGenerationQuality {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ImageGenerationModel model;

    @Column(nullable = false, length = 32)
    private String code;

    @Column(name = "display_name", nullable = false, length = 64)
    private String displayName;

    @Column(name = "api_quality", length = 32)
    private String apiQuality;

    @Column(name = "is_default", nullable = false)
    private boolean defaultQuality;

    @Column(nullable = false)
    private int position;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @OneToMany(mappedBy = "quality", fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<ImageGenerationPrice> prices = new ArrayList<>();
}

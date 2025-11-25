package com.marketinghub.imagedeliverable;

import com.marketinghub.media.Asset;
import com.marketinghub.model.Lead;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Groups a batch of generated images linked to a lead submission.
 */
@Entity
@Table(name = "image_deliverable_package")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageDeliverablePackage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id", nullable = false)
    @ToString.Exclude
    private Lead lead;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "input_asset_id", nullable = false)
    @ToString.Exclude
    private Asset inputAsset;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ImageDeliverableStatus status = ImageDeliverableStatus.RECEIVED;

    @Column(name = "planned_outputs")
    private Integer plannedOutputs;

    @Builder.Default
    @Column(name = "free_images", nullable = false)
    private Integer freeImages = 0;

    @Column(length = 255)
    private String model;

    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(nullable = false)
    private String prompt;

    @Builder.Default
    @OneToMany(mappedBy = "packageRef", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private List<ImageDeliverableItem> items = new ArrayList<>();

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}

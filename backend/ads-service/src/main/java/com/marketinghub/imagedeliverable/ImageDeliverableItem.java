package com.marketinghub.imagedeliverable;

import com.marketinghub.media.Asset;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Individual generated image that belongs to a package.
 */
@Entity
@Table(name = "image_deliverable_item", uniqueConstraints = {
        @UniqueConstraint(name = "uq_image_deliverable_item_order", columnNames = {"package_id", "position_index"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageDeliverableItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ImageDeliverablePackage packageRef;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Asset asset;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_type", nullable = false, length = 20)
    private ImageDeliverableAccessType accessType;

    @Column(name = "position_index", nullable = false)
    private int position;

    @CreationTimestamp
    private Instant createdAt;
}

package com.marketinghub.watermark.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "flow_submission_image_watermark")
public class FlowSubmissionImageWatermarkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", unique = true)
    private FlowSubmissionImageItemEntity item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id")
    private AssetEntity asset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "optimized_asset_id")
    private AssetEntity optimizedAsset;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public FlowSubmissionImageItemEntity getItem() {
        return item;
    }

    public void setItem(FlowSubmissionImageItemEntity item) {
        this.item = item;
    }

    public AssetEntity getAsset() {
        return asset;
    }

    public void setAsset(AssetEntity asset) {
        this.asset = asset;
    }

    public AssetEntity getOptimizedAsset() {
        return optimizedAsset;
    }

    public void setOptimizedAsset(AssetEntity optimizedAsset) {
        this.optimizedAsset = optimizedAsset;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

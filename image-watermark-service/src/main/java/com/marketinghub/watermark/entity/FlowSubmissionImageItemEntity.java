package com.marketinghub.watermark.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "flow_submission_image_item")
public class FlowSubmissionImageItemEntity {

    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id")
    private FlowSubmissionImagePackageEntity imagePackage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id")
    private AssetEntity asset;

    @OneToOne(mappedBy = "item", fetch = FetchType.LAZY)
    private FlowSubmissionImageWatermarkEntity watermark;

    @Column(name = "access_type")
    private String accessType;

    @Column(name = "position_index")
    private Integer positionIndex;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public FlowSubmissionImagePackageEntity getImagePackage() {
        return imagePackage;
    }

    public void setImagePackage(FlowSubmissionImagePackageEntity imagePackage) {
        this.imagePackage = imagePackage;
    }

    public AssetEntity getAsset() {
        return asset;
    }

    public void setAsset(AssetEntity asset) {
        this.asset = asset;
    }

    public FlowSubmissionImageWatermarkEntity getWatermark() {
        return watermark;
    }

    public void setWatermark(FlowSubmissionImageWatermarkEntity watermark) {
        this.watermark = watermark;
    }

    public String getAccessType() {
        return accessType;
    }

    public void setAccessType(String accessType) {
        this.accessType = accessType;
    }

    public Integer getPositionIndex() {
        return positionIndex;
    }

    public void setPositionIndex(Integer positionIndex) {
        this.positionIndex = positionIndex;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

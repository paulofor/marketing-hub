package com.marketinghub.imagedeliverable.dto;

import com.marketinghub.imagedeliverable.ImageDeliverableStatus;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO describing an {@link com.marketinghub.imagedeliverable.ImageDeliverablePackage}.
 */
@Data
public class ImageDeliverablePackageDto {
    private Long id;
    private UUID leadId;
    private Long inputAssetId;
    private String inputAssetUrl;
    private ImageDeliverableStatus status;
    private Integer plannedOutputs;
    private Integer freeImages;
    private String model;
    private String prompt;
    private List<ImageDeliverableItemDto> items;
    private Instant createdAt;
    private Instant updatedAt;
}

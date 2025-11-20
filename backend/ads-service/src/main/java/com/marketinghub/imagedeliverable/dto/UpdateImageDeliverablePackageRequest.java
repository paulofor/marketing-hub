package com.marketinghub.imagedeliverable.dto;

import com.marketinghub.imagedeliverable.ImageDeliverableStatus;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * Payload to update an existing image deliverable package.
 */
@Data
public class UpdateImageDeliverablePackageRequest {
    private UUID leadId;
    private Long inputAssetId;
    private Integer plannedOutputs;
    private Integer freeImages;
    private ImageDeliverableStatus status;
    private String model;
    private String prompt;
    private List<ImageDeliverableItemRequest> items;
}

package com.marketinghub.imagedeliverable.dto;

import com.marketinghub.imagedeliverable.ImageDeliverableStatus;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Payload to create a new image deliverable package for a lead.
 */
@Data
public class CreateImageDeliverablePackageRequest {
    private UUID leadId;
    private Long inputAssetId;
    private Integer plannedOutputs;
    private Integer freeImages;
    private ImageDeliverableStatus status;
    private String model;
    private String prompt;
    private List<ImageDeliverableItemRequest> items = new ArrayList<>();
}

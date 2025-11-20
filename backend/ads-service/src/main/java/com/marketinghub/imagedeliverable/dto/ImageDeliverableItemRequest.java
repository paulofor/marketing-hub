package com.marketinghub.imagedeliverable.dto;

import com.marketinghub.imagedeliverable.ImageDeliverableAccessType;
import lombok.Data;

/**
 * Describes one generated image to attach to a package.
 */
@Data
public class ImageDeliverableItemRequest {
    private Long assetId;
    private ImageDeliverableAccessType accessType;
}

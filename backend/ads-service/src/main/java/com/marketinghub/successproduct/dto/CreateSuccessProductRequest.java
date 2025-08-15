package com.marketinghub.successproduct.dto;

import com.marketinghub.successproduct.SuccessProductPlatform;
import lombok.Data;

/**
 * Request body for creating a success product.
 */
@Data
public class CreateSuccessProductRequest {
    private String description;
    private SuccessProductPlatform platform;
}

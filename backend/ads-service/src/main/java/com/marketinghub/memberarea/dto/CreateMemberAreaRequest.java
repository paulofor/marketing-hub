package com.marketinghub.memberarea.dto;

import lombok.Data;

/**
 * Request payload for creating a member area.
 */
@Data
public class CreateMemberAreaRequest {
    private Long productId;
    private String name;
    private String accessUrl;
    private String description;
}

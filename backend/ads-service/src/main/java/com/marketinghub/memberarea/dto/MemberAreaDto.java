package com.marketinghub.memberarea.dto;

import java.time.Instant;
import lombok.Data;

/**
 * DTO representing a {@link com.marketinghub.memberarea.MemberArea}.
 */
@Data
public class MemberAreaDto {
    private Long id;
    private Long productId;
    private String name;
    private String accessUrl;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;
}

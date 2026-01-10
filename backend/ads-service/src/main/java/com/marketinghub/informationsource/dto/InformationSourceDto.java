package com.marketinghub.informationsource.dto;

import lombok.Data;

import java.time.Instant;

/**
 * Data transfer object for {@link com.marketinghub.informationsource.InformationSource}.
 */
@Data
public class InformationSourceDto {
    private Long id;
    private Long marketNicheId;
    private String name;
    private String url;
    private Instant createdAt;
    private Instant updatedAt;
}

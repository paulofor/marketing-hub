package com.marketinghub.informationsource.dto;

import lombok.Data;

/**
 * Request body for creating an information source.
 */
@Data
public class CreateInformationSourceRequest {
    private Long marketNicheId;
    private String name;
    private String url;
}

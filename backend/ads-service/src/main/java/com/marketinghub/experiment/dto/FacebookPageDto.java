package com.marketinghub.experiment.dto;

import lombok.Data;

/**
 * DTO representing a Facebook page linked to an experiment.
 */
@Data
public class FacebookPageDto {
    private Long id;
    private Long accountId;
    private String pageId;
    private String name;
}

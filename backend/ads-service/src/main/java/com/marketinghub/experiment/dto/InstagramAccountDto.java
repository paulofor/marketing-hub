package com.marketinghub.experiment.dto;

import lombok.Data;

/**
 * DTO for InstagramAccount associated to experiments.
 */
@Data
public class InstagramAccountDto {
    private Long id;
    private String name;
    private String handle;
    private String code;
}

package com.marketinghub.leadportal.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SimpleFormStyle(
        String slug,
        String name,
        SimpleFormStyleDefinition definition) {
}

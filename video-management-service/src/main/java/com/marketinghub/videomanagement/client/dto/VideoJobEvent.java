package com.marketinghub.videomanagement.client.dto;

/** Responsabilidade: transportar evidências persistidas do mesmo job para recuperação sem geração. */
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
public record VideoJobEvent(Long id, String detailsJson) {}

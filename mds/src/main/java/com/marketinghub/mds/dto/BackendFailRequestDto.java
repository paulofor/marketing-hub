package com.marketinghub.mds.dto;

public record BackendFailRequestDto(String reason, String stageName, String message) {
}

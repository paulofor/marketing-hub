package com.marketinghub.mds.dto;

import java.util.Map;

public record BackendHeartbeatRequestDto(String stageName, String message, Map<String, Object> payload) {
}

package com.marketinghub.settings.dto;

import java.time.Instant;

public record GeneralSettingDto(
        String name,
        String value,
        Instant updatedAt
) {
}

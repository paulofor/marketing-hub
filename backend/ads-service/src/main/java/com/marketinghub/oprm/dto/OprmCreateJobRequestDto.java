package com.marketinghub.oprm.dto;

import com.marketinghub.oprm.OprmJobType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record OprmCreateJobRequestDto(
        @NotNull OprmJobType jobType,
        @NotBlank String occupationSeedRef,
        String correlationId,
        List<String> inputRefs
) {
}

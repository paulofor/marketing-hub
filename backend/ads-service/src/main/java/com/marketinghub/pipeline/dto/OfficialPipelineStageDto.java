package com.marketinghub.pipeline.dto;

import java.util.List;
import lombok.Builder;

/**
 * DTO que descreve uma etapa oficial e seus aliases canônicos para a tela administrativa.
 */
@Builder
public record OfficialPipelineStageDto(
        String canonicalCode,
        String operationalCode,
        String name,
        int position,
        boolean required,
        boolean configurable,
        String executionModule,
        String rootPackage,
        String modulePackage,
        StageFieldPolicyDto fieldPolicy,
        List<String> aliases) {}

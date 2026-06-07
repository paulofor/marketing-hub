package com.marketinghub.pipeline.dto;

import java.util.List;
import lombok.Builder;

/**
 * DTO que descreve um pipeline oficial protegido contra edição estrutural perigosa.
 */
@Builder
public record OfficialPipelineDto(
        String module,
        String code,
        String name,
        String canonicalVersion,
        boolean official,
        List<String> aliases,
        List<String> implementationModules,
        List<String> backendPackages,
        List<String> modulePackages,
        PipelineFieldPolicyDto fieldPolicy,
        List<OfficialPipelineStageDto> stages) {}

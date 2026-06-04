package com.marketinghub.pipeline.dto;

import java.util.List;
import lombok.Builder;

/**
 * DTO que expõe à tela os módulos, pipelines e etapas permitidos pelo contrato oficial.
 */
@Builder
public record PipelineMetadataDto(List<String> validModules, List<OfficialPipelineDto> officialPipelines) {}

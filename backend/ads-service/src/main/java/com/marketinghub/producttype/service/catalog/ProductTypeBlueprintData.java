package com.marketinghub.producttype.service.catalog;

import jakarta.validation.constraints.Size;

/** Responsabilidade: transportar a base editável usada para construir um tipo de produto. */
public record ProductTypeBlueprintData(
    @Size(max = 64) String version,
    @Size(max = 64) String primaryChannel,
    @Size(max = 5000) String customerJob,
    @Size(max = 5000) String valueMechanism,
    @Size(max = 5000) String experienceFlow,
    @Size(max = 5000) String requiredInputs,
    @Size(max = 5000) String expectedOutputs,
    @Size(max = 5000) String memoryStrategy,
    @Size(max = 5000) String integrationRequirements,
    @Size(max = 5000) String safetyGuardrails,
    @Size(max = 5000) String successMetrics,
    @Size(max = 255) String backendSdkModule,
    @Size(max = 255) String frontendSdkModule) {}

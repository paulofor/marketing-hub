package com.marketinghub.microservice.dto;

import java.util.List;

/** Inventário operacional consolidado de portas, hosts e referências de deploy. */
public record OperationalInventoryDto(
    List<DiscoveredMicroserviceDto> services, List<DeploymentWorkflowInventoryDto> deployments) {}

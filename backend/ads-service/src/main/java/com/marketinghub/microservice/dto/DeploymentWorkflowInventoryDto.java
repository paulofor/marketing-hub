package com.marketinghub.microservice.dto;

import java.util.List;

/** Projeção do deploy versionado declarado em um workflow do GitHub Actions. */
public record DeploymentWorkflowInventoryDto(
    String workflowFile,
    String workflowName,
    String jobName,
    String deployHost,
    String deployUser,
    String remotePath,
    List<String> secretReferences,
    String triggerMode) {}

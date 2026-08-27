package com.marketinghub.agentdetail.service.getDetail;

import java.time.Instant;
import java.util.List;

/** Responsabilidade: consolidar todos os dados específicos do contrato atual de um agente. */
public record AgentDetailResponse(
    Long id,
    String name,
    String nickname,
    Long portraitAssetId,
    String portraitUrl,
    String agentKey,
    String status,
    Integer currentVersion,
    Long themeId,
    String themeName,
    String ownerName,
    String description,
    String businessObjective,
    String successMetrics,
    String modelName,
    String executionMode,
    Boolean automaticExecutionEnabled,
    Instant automaticExecutionChangedAt,
    String automaticExecutionChangedBy,
    String triggerPolicy,
    String responsibilityContract,
    String orchestratorPolicy,
    String analysisPolicy,
    String offeringPolicy,
    String authorityPolicy,
    String promptContractPath,
    String schemaContractPath,
    List<AgentDetailItemResponse> inputs,
    List<AgentDetailItemResponse> outputs,
    List<AgentDetailItemResponse> internalFunctions,
    List<AgentDetailResourceResponse> executionResources,
    Instant createdAt,
    Instant updatedAt,
    Instant lastContractChangeAt) {}

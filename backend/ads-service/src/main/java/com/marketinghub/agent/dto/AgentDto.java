package com.marketinghub.agent.dto;

import java.time.Instant;
import java.util.List;
import lombok.Data;

@Data
/** Responsabilidade: transportar o cadastro e a governanca atual de um agente. */
public class AgentDto {
  private Long id;
  private String name;
  private String agentKey;
  private String status;
  private Integer currentVersion;
  private String ownerName;
  private String businessObjective;
  private String successMetrics;
  private String modelName;
  private String triggerPolicy;
  private String authorityPolicy;
  private String responsibilityContract;
  private String orchestratorPolicy;
  private String analysisPolicy;
  private String offeringPolicy;
  private String promptContractPath;
  private String schemaContractPath;
  private String executionMode;
  private String description;
  private Long themeId;
  private String themeName;
  private List<AgentItemDto> inputs;
  private List<AgentItemDto> outputs;
  private List<AgentItemDto> internalFunctions;
  private Instant createdAt;
  private Instant updatedAt;
}

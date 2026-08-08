package com.marketinghub.agent.dto;

import java.util.List;
import lombok.Data;

@Data
/** Responsabilidade: receber os dados editaveis do cadastro de um agente. */
public class SaveAgentRequest {
  private String name;
  private String agentKey;
  private String status;
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
  private List<SaveAgentItemRequest> inputs;
  private List<SaveAgentItemRequest> outputs;
  private List<SaveAgentItemRequest> internalFunctions;
}

package com.marketinghub.agent.mapper;

import com.marketinghub.agent.Agent;
import com.marketinghub.agent.AgentInput;
import com.marketinghub.agent.AgentInternalFunction;
import com.marketinghub.agent.AgentOutput;
import com.marketinghub.agent.AgentTheme;
import com.marketinghub.agent.dto.AgentDto;
import com.marketinghub.agent.dto.AgentItemDto;
import com.marketinghub.agent.dto.AgentThemeDto;
import com.marketinghub.agent.integration.AgentWorkflowFreshness;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
/** Responsabilidade: converter entidades de agentes e temas em contratos da API. */
public class AgentMapper {

  /** Converte um agente completo e a data auditavel de sua versao para o contrato de leitura. */
  public AgentDto toDto(Agent agent, Instant lastContractChangeAt) {
    return toDto(agent, lastContractChangeAt, null);
  }

  /** Converte um agente com a data do contrato e a recência operacional do executor. */
  public AgentDto toDto(
      Agent agent, Instant lastContractChangeAt, AgentWorkflowFreshness workflowFreshness) {
    AgentDto dto = new AgentDto();
    dto.setId(agent.getId());
    dto.setName(agent.getName());
    dto.setNickname(agent.getNickname());
    dto.setPortraitAssetId(
        Optional.ofNullable(agent.getPortraitAsset()).map(asset -> asset.getId()).orElse(null));
    dto.setPortraitUrl(
        Optional.ofNullable(agent.getPortraitAsset()).map(asset -> asset.getUrl()).orElse(null));
    dto.setAgentKey(agent.getAgentKey());
    dto.setStatus(agent.getStatus());
    dto.setCurrentVersion(agent.getCurrentVersion());
    dto.setOwnerName(agent.getOwnerName());
    dto.setBusinessObjective(agent.getBusinessObjective());
    dto.setSuccessMetrics(agent.getSuccessMetrics());
    dto.setModelName(agent.getModelName());
    dto.setTriggerPolicy(agent.getTriggerPolicy());
    dto.setAuthorityPolicy(agent.getAuthorityPolicy());
    dto.setResponsibilityContract(agent.getResponsibilityContract());
    dto.setOrchestratorPolicy(agent.getOrchestratorPolicy());
    dto.setAnalysisPolicy(agent.getAnalysisPolicy());
    dto.setOfferingPolicy(agent.getOfferingPolicy());
    dto.setPromptContractPath(agent.getPromptContractPath());
    dto.setSchemaContractPath(agent.getSchemaContractPath());
    dto.setExecutionMode(agent.getExecutionMode());
    dto.setDescription(agent.getDescription());
    dto.setThemeId(Optional.ofNullable(agent.getTheme()).map(AgentTheme::getId).orElse(null));
    dto.setThemeName(Optional.ofNullable(agent.getTheme()).map(AgentTheme::getName).orElse(null));
    dto.setInputs(mapInputs(agent.getInputs()));
    dto.setOutputs(mapOutputs(agent.getOutputs()));
    dto.setInternalFunctions(mapInternalFunctions(agent.getInternalFunctions()));
    dto.setCreatedAt(agent.getCreatedAt());
    dto.setUpdatedAt(agent.getUpdatedAt());
    dto.setLastContractChangeAt(lastContractChangeAt);
    if (workflowFreshness != null) {
      dto.setLastWorkflowRunAt(workflowFreshness.lastWorkflowRunAt());
      dto.setWorkflowName(workflowFreshness.workflowName());
      dto.setWorkflowFile(workflowFreshness.workflowFile());
      dto.setWorkflowConclusion(workflowFreshness.workflowConclusion());
      dto.setWorkflowUrl(workflowFreshness.workflowUrl());
    }
    return dto;
  }

  /** Converte um tema para o contrato de leitura. */
  public AgentThemeDto toDto(AgentTheme theme) {
    AgentThemeDto dto = new AgentThemeDto();
    dto.setId(theme.getId());
    dto.setName(theme.getName());
    dto.setDescription(theme.getDescription());
    return dto;
  }

  /** Converte entradas persistidas para itens do contrato. */
  private List<AgentItemDto> mapInputs(List<AgentInput> inputs) {
    return inputs == null
        ? List.of()
        : inputs.stream()
            .sorted(Comparator.comparingInt(AgentInput::getOrderIndex))
            .map(
                input -> {
                  AgentItemDto dto = new AgentItemDto();
                  dto.setId(input.getId());
                  dto.setName(input.getName());
                  dto.setType(input.getType());
                  dto.setDescription(input.getDescription());
                  dto.setOrderIndex(input.getOrderIndex());
                  return dto;
                })
            .toList();
  }

  /** Converte saidas persistidas para itens do contrato. */
  private List<AgentItemDto> mapOutputs(List<AgentOutput> outputs) {
    return outputs == null
        ? List.of()
        : outputs.stream()
            .sorted(Comparator.comparingInt(AgentOutput::getOrderIndex))
            .map(
                output -> {
                  AgentItemDto dto = new AgentItemDto();
                  dto.setId(output.getId());
                  dto.setName(output.getName());
                  dto.setType(output.getType());
                  dto.setDescription(output.getDescription());
                  dto.setOrderIndex(output.getOrderIndex());
                  return dto;
                })
            .toList();
  }

  /** Converte ferramentas persistidas para itens do contrato. */
  private List<AgentItemDto> mapInternalFunctions(List<AgentInternalFunction> functions) {
    return functions == null
        ? List.of()
        : functions.stream()
            .sorted(Comparator.comparingInt(AgentInternalFunction::getOrderIndex))
            .map(
                function -> {
                  AgentItemDto dto = new AgentItemDto();
                  dto.setId(function.getId());
                  dto.setName(function.getName());
                  dto.setType(function.getType());
                  dto.setDescription(function.getDescription());
                  dto.setOrderIndex(function.getOrderIndex());
                  return dto;
                })
            .toList();
  }
}

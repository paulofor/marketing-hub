package com.marketinghub.agent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agent.Agent;
import com.marketinghub.agent.AgentInput;
import com.marketinghub.agent.AgentInternalFunction;
import com.marketinghub.agent.AgentOutput;
import com.marketinghub.agent.AgentVersion;
import com.marketinghub.agent.dto.SaveAgentItemRequest;
import com.marketinghub.agent.dto.SaveAgentRequest;
import com.marketinghub.repository.jpa.agent.AgentRepository;
import com.marketinghub.repository.jpa.agent.AgentVersionRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Responsabilidade: manter o cadastro atual e o historico versionado dos agentes. */
@Service
public class AgentService {

  private final AgentRepository repository;
  private final AgentThemeService themeService;
  private final AgentVersionRepository versionRepository;
  private final ObjectMapper objectMapper;

  /** Configura as dependencias do cadastro e versionamento. */
  public AgentService(
      AgentRepository repository,
      AgentThemeService themeService,
      AgentVersionRepository versionRepository,
      ObjectMapper objectMapper) {
    this.repository = repository;
    this.themeService = themeService;
    this.versionRepository = versionRepository;
    this.objectMapper = objectMapper;
  }

  /** Cria o agente e registra a primeira versao imutavel do contrato. */
  @Transactional
  public Agent create(SaveAgentRequest request) {
    Agent agent = new Agent();
    apply(agent, request);
    Agent saved = repository.save(agent);
    saveVersion(saved);
    return saved;
  }

  /** Atualiza o cadastro e cria uma nova versao quando o contrato for salvo. */
  @Transactional
  public Agent update(Long id, SaveAgentRequest request) {
    Agent agent = repository.findDetailedById(id).orElseThrow();
    agent.setCurrentVersion(
        (agent.getCurrentVersion() == null ? 0 : agent.getCurrentVersion()) + 1);
    apply(agent, request);
    Agent saved = repository.save(agent);
    saveVersion(saved);
    return saved;
  }

  /** Recupera um agente com todos os seus contratos operacionais. */
  /** Lista os agentes cadastrados em ordem alfabetica. */
  @Transactional(readOnly = true)
  public Agent get(Long id) {
    Agent agent = repository.findDetailedById(id).orElseThrow();
    initialize(agent);
    return agent;
  }

  @Transactional(readOnly = true)
  public List<Agent> list() {
    List<Agent> agents = repository.findAllByOrderByNameAsc();
    agents.forEach(this::initialize);
    return agents;
  }

  /** Inicializa relacionamentos necessarios para leitura fora da transacao. */
  private void initialize(Agent agent) {
    agent.getInputs().size();
    agent.getOutputs().size();
    agent.getInternalFunctions().size();
    if (agent.getTheme() != null) {
      agent.getTheme().getName();
    }
  }

  /** Aplica os campos editaveis e substitui os contratos filhos. */
  private void apply(Agent agent, SaveAgentRequest request) {
    agent.setName(request.getName());
    agent.setAgentKey(request.getAgentKey());
    agent.setStatus(request.getStatus() == null ? "DRAFT" : request.getStatus());
    agent.setOwnerName(request.getOwnerName());
    agent.setBusinessObjective(request.getBusinessObjective());
    agent.setSuccessMetrics(request.getSuccessMetrics());
    agent.setModelName(request.getModelName());
    agent.setTriggerPolicy(request.getTriggerPolicy());
    agent.setAuthorityPolicy(request.getAuthorityPolicy());
    agent.setResponsibilityContract(request.getResponsibilityContract());
    agent.setOrchestratorPolicy(request.getOrchestratorPolicy());
    agent.setAnalysisPolicy(request.getAnalysisPolicy());
    agent.setOfferingPolicy(request.getOfferingPolicy());
    agent.setPromptContractPath(request.getPromptContractPath());
    agent.setSchemaContractPath(request.getSchemaContractPath());
    agent.setExecutionMode(request.getExecutionMode());
    agent.setDescription(request.getDescription());
    agent.setTheme(themeService.get(request.getThemeId()));

    replaceInputs(agent, request.getInputs());
    replaceOutputs(agent, request.getOutputs());
    replaceFunctions(agent, request.getInternalFunctions());
  }

  /** Persiste uma fotografia imutavel do contrato que governou a versao. */
  private void saveVersion(Agent agent) {
    AgentVersion version = new AgentVersion();
    version.setAgent(agent);
    version.setVersionNumber(agent.getCurrentVersion());
    version.setContractSnapshot(serializeContract(agent));
    version.setCreatedAt(Instant.now());
    versionRepository.save(version);
  }

  /** Serializa somente os campos de governanca relevantes para auditoria. */
  private String serializeContract(Agent agent) {
    try {
      LinkedHashMap<String, Object> contract = new LinkedHashMap<>();
      contract.put("agentKey", agent.getAgentKey());
      contract.put("name", agent.getName());
      contract.put("status", agent.getStatus());
      contract.put("executionMode", agent.getExecutionMode());
      contract.put("businessObjective", agent.getBusinessObjective());
      contract.put("successMetrics", agent.getSuccessMetrics());
      contract.put("model", agent.getModelName());
      contract.put("triggerPolicy", agent.getTriggerPolicy());
      contract.put("authorityPolicy", agent.getAuthorityPolicy());
      contract.put("responsibilityContract", agent.getResponsibilityContract());
      contract.put("orchestratorPolicy", agent.getOrchestratorPolicy());
      contract.put("analysisPolicy", agent.getAnalysisPolicy());
      contract.put("offeringPolicy", agent.getOfferingPolicy());
      contract.put("promptContractPath", agent.getPromptContractPath());
      contract.put("schemaContractPath", agent.getSchemaContractPath());
      contract.put(
          "inputs",
          agent.getInputs().stream()
              .map(
                  item ->
                      List.of(
                          item.getName(),
                          valueOrEmpty(item.getType()),
                          valueOrEmpty(item.getDescription())))
              .toList());
      contract.put(
          "outputs",
          agent.getOutputs().stream()
              .map(
                  item ->
                      List.of(
                          item.getName(),
                          valueOrEmpty(item.getType()),
                          valueOrEmpty(item.getDescription())))
              .toList());
      contract.put(
          "tools",
          agent.getInternalFunctions().stream()
              .map(
                  item ->
                      List.of(
                          item.getName(),
                          valueOrEmpty(item.getType()),
                          valueOrEmpty(item.getDescription())))
              .toList());
      return objectMapper.writeValueAsString(contract);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException(
          "Nao foi possivel versionar o contrato do agente " + agent.getAgentKey(), ex);
    }
  }

  /** Evita valores nulos nas listas do contrato serializado. */
  private String valueOrEmpty(String value) {
    return value == null ? "" : value;
  }

  /** Substitui as entradas declaradas pelo agente. */
  private void replaceInputs(Agent agent, List<SaveAgentItemRequest> items) {
    agent.getInputs().clear();
    if (items == null) {
      return;
    }
    for (int i = 0; i < items.size(); i++) {
      SaveAgentItemRequest item = items.get(i);
      AgentInput input = new AgentInput();
      input.setAgent(agent);
      input.setName(item.getName());
      input.setType(item.getType());
      input.setDescription(item.getDescription());
      input.setOrderIndex(item.getOrderIndex() != null ? item.getOrderIndex() : i);
      agent.getInputs().add(input);
    }
  }

  /** Substitui as saidas declaradas pelo agente. */
  private void replaceOutputs(Agent agent, List<SaveAgentItemRequest> items) {
    agent.getOutputs().clear();
    if (items == null) {
      return;
    }
    for (int i = 0; i < items.size(); i++) {
      SaveAgentItemRequest item = items.get(i);
      AgentOutput output = new AgentOutput();
      output.setAgent(agent);
      output.setName(item.getName());
      output.setType(item.getType());
      output.setDescription(item.getDescription());
      output.setOrderIndex(item.getOrderIndex() != null ? item.getOrderIndex() : i);
      agent.getOutputs().add(output);
    }
  }

  /** Substitui as ferramentas declaradas pelo agente. */
  private void replaceFunctions(Agent agent, List<SaveAgentItemRequest> items) {
    agent.getInternalFunctions().clear();
    if (items == null) {
      return;
    }
    for (int i = 0; i < items.size(); i++) {
      SaveAgentItemRequest item = items.get(i);
      AgentInternalFunction function = new AgentInternalFunction();
      function.setAgent(agent);
      function.setName(item.getName());
      function.setType(item.getType());
      function.setDescription(item.getDescription());
      function.setOrderIndex(item.getOrderIndex() != null ? item.getOrderIndex() : i);
      agent.getInternalFunctions().add(function);
    }
  }
}
  /** Lista os agentes cadastrados em ordem alfabetica. */

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
import com.marketinghub.agent.service.uploadportrait.AgentPortraitUploadResponse;
import com.marketinghub.media.Asset;
import com.marketinghub.media.AssetStatus;
import com.marketinghub.media.AssetType;
import com.marketinghub.repository.jpa.agent.AgentRepository;
import com.marketinghub.repository.jpa.agent.AgentVersionRepository;
import com.marketinghub.repository.jpa.media.AssetRepository;
import com.marketinghub.storage.AssetStorageService;
import com.marketinghub.storage.AssetUploadCategory;
import com.marketinghub.storage.AssetUploadContext;
import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: manter o cadastro atual e o historico versionado dos agentes. */
@Service
public class AgentService {
  private static final long MAX_PORTRAIT_FILE_SIZE = 5L * 1024 * 1024;

  private final AgentRepository repository;
  private final AgentThemeService themeService;
  private final AgentVersionRepository versionRepository;
  private final ObjectMapper objectMapper;
  private final AssetRepository assetRepository;
  private final AssetStorageService storageService;

  /** Configura as dependencias do cadastro e versionamento. */
  public AgentService(
      AgentRepository repository,
      AgentThemeService themeService,
      AgentVersionRepository versionRepository,
      ObjectMapper objectMapper,
      AssetRepository assetRepository,
      AssetStorageService storageService) {
    this.repository = repository;
    this.themeService = themeService;
    this.versionRepository = versionRepository;
    this.objectMapper = objectMapper;
    this.assetRepository = assetRepository;
    this.storageService = storageService;
  }

  /** Valida e armazena uma imagem PNG, JPEG ou WebP de até 5 MB. */
  public AgentPortraitUploadResponse uploadPortrait(MultipartFile file) throws IOException {
    validatePortraitFile(file);
    validatePortraitSignature(file);
    AssetStorageService.StoredObject stored =
        storageService.store(
            file, new AssetUploadContext(AssetUploadCategory.AGENT_PORTRAIT, null, null, null));
    Asset asset =
        assetRepository.save(
            Asset.builder()
                .type(AssetType.IMAGE)
                .provider(com.marketinghub.media.MediaProvider.USER_UPLOAD)
                .status(AssetStatus.READY)
                .url(stored.publicUrl())
                .externalId(stored.storedFileName())
                .payload("{\"category\":\"AGENT_PORTRAIT\"}")
                .build());
    return new AgentPortraitUploadResponse(asset.getId(), asset.getUrl());
  }

  /** Cria o agente e registra a primeira versao imutavel do contrato. */
  @Transactional
  public Agent create(SaveAgentRequest request) {
    Agent agent = new Agent();
    validateNickname(request.getNickname(), null);
    apply(agent, request);
    Agent saved = repository.save(agent);
    saveVersion(saved);
    return saved;
  }

  /** Atualiza o cadastro e cria uma nova versao quando o contrato for salvo. */
  @Transactional
  public Agent update(Long id, SaveAgentRequest request) {
    Agent agent = repository.findDetailedById(id).orElseThrow();
    validateNickname(request.getNickname(), id);
    agent.setCurrentVersion(
        (agent.getCurrentVersion() == null ? 0 : agent.getCurrentVersion()) + 1);
    apply(agent, request);
    Agent saved = repository.save(agent);
    saveVersion(saved);
    return saved;
  }

  /** Recupera um agente com todos os seus contratos operacionais. */
  @Transactional(readOnly = true)
  public Agent get(Long id) {
    Agent agent = repository.findDetailedById(id).orElseThrow();
    initialize(agent);
    return agent;
  }

  /** Lista os agentes cadastrados em ordem alfabetica pelo apelido. */
  @Transactional(readOnly = true)
  public List<Agent> list() {
    List<Agent> agents = repository.findAllByOrderByNicknameAsc();
    agents.forEach(this::initialize);
    return agents;
  }

  /** Resolve em uma consulta a ultima alteracao contratual auditada de cada agente informado. */
  @Transactional(readOnly = true)
  public Map<Long, Instant> currentVersionChanges(List<Agent> agents) {
    if (agents.isEmpty()) {
      return Map.of();
    }
    List<Long> agentIds = agents.stream().map(Agent::getId).toList();
    return versionRepository.findCurrentVersionChanges(agentIds).stream()
        .collect(
            Collectors.toUnmodifiableMap(
                AgentVersionRepository.CurrentVersionChange::getAgentId,
                AgentVersionRepository.CurrentVersionChange::getChangedAt,
                (first, ignored) -> first));
  }

  /** Inicializa relacionamentos necessarios para leitura fora da transacao. */
  private void initialize(Agent agent) {
    agent.getInputs().size();
    agent.getOutputs().size();
    agent.getInternalFunctions().size();
    if (agent.getTheme() != null) {
      agent.getTheme().getName();
    }
    if (agent.getPortraitAsset() != null) {
      agent.getPortraitAsset().getUrl();
    }
  }

  /** Aplica os campos editaveis e substitui os contratos filhos. */
  private void apply(Agent agent, SaveAgentRequest request) {
    agent.setName(request.getName());
    agent.setNickname(request.getNickname().trim());
    agent.setPortraitAsset(resolvePortrait(request.getPortraitAssetId()));
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
      contract.put("nickname", agent.getNickname());
      contract.put(
          "portraitAssetId",
          agent.getPortraitAsset() == null ? null : agent.getPortraitAsset().getId());
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

  /** Resolve somente assets de imagem prontos e criados para identificar agentes. */
  private Asset resolvePortrait(Long assetId) {
    if (assetId == null) {
      return null;
    }
    Asset asset =
        assetRepository
            .findById(assetId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Imagem do agente não encontrada."));
    if (asset.getType() != AssetType.IMAGE
        || asset.getStatus() != AssetStatus.READY
        || asset.getPayload() == null
        || !asset.getPayload().contains("\"category\":\"AGENT_PORTRAIT\"")) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "O asset informado não é uma imagem de agente válida.");
    }
    return asset;
  }

  /** Rejeita arquivos vazios, grandes ou fora dos formatos seguros para a interface. */
  private void validatePortraitFile(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione uma imagem.");
    }
    if (file.getSize() > MAX_PORTRAIT_FILE_SIZE) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "A imagem deve ter no máximo 5 MB.");
    }
    String type =
        file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
    if (!type.equals("image/png") && !type.equals("image/jpeg") && !type.equals("image/webp")) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Use uma imagem PNG, JPEG ou WebP.");
    }
  }

  /** Confirma pela assinatura binária que o conteúdo corresponde ao formato declarado. */
  private void validatePortraitSignature(MultipartFile file) throws IOException {
    byte[] header;
    try (var input = file.getInputStream()) {
      header = input.readNBytes(12);
    }
    boolean png =
        header.length >= 8
            && (header[0] & 0xff) == 0x89
            && header[1] == 'P'
            && header[2] == 'N'
            && header[3] == 'G';
    boolean jpeg =
        header.length >= 3
            && (header[0] & 0xff) == 0xff
            && (header[1] & 0xff) == 0xd8
            && (header[2] & 0xff) == 0xff;
    boolean webp =
        header.length >= 12
            && header[0] == 'R'
            && header[1] == 'I'
            && header[2] == 'F'
            && header[3] == 'F'
            && header[8] == 'W'
            && header[9] == 'E'
            && header[10] == 'B'
            && header[11] == 'P';
    if (!png && !jpeg && !webp) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "O conteúdo do arquivo não corresponde a uma imagem válida.");
    }
  }

  /** Garante um apelido curto e exclusivo antes da persistencia. */
  private void validateNickname(String nickname, Long currentAgentId) {
    if (nickname == null || nickname.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Apelido do agente é obrigatório.");
    }
    String normalized = nickname.trim();
    if (normalized.length() > 60) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Apelido do agente deve ter no máximo 60 caracteres.");
    }
    boolean duplicate =
        currentAgentId == null
            ? repository.existsByNicknameIgnoreCase(normalized)
            : repository.existsByNicknameIgnoreCaseAndIdNot(normalized, currentAgentId);
    if (duplicate) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Já existe um agente com este apelido.");
    }
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

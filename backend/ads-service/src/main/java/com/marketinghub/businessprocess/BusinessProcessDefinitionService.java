package com.marketinghub.businessprocess;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.businessprocessresource.BusinessProcessExecutionResourceRepository;
import java.text.Normalizer;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: governar cadastro, validação, versionamento e publicação de processos. */
@Service
public class BusinessProcessDefinitionService {
  private final BusinessProcessDefinitionRepository repository;
  private final AgentTaskRepository agentTaskRepository;
  private final BusinessProcessExecutionResourceRepository executionResourceRepository;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  /** Configura persistência, serialização e relógio operacional. */
  @Autowired
  public BusinessProcessDefinitionService(
      BusinessProcessDefinitionRepository repository,
      AgentTaskRepository agentTaskRepository,
      BusinessProcessExecutionResourceRepository executionResourceRepository,
      ObjectMapper objectMapper) {
    this(
        repository,
        agentTaskRepository,
        executionResourceRepository,
        objectMapper,
        Clock.systemUTC());
  }

  /** Preserva a montagem isolada de cenários que não usam recurso especializado. */
  BusinessProcessDefinitionService(
      BusinessProcessDefinitionRepository repository,
      AgentTaskRepository agentTaskRepository,
      ObjectMapper objectMapper) {
    this(repository, agentTaskRepository, null, objectMapper, Clock.systemUTC());
  }

  /** Permite testes determinísticos do ciclo de publicação. */
  BusinessProcessDefinitionService(
      BusinessProcessDefinitionRepository repository,
      AgentTaskRepository agentTaskRepository,
      ObjectMapper objectMapper,
      Clock clock) {
    this(repository, agentTaskRepository, null, objectMapper, clock);
  }

  /** Permite validar recursos especializados com relógio determinístico nos testes. */
  BusinessProcessDefinitionService(
      BusinessProcessDefinitionRepository repository,
      AgentTaskRepository agentTaskRepository,
      BusinessProcessExecutionResourceRepository executionResourceRepository,
      ObjectMapper objectMapper,
      Clock clock) {
    this.repository = repository;
    this.agentTaskRepository = agentTaskRepository;
    this.executionResourceRepository = executionResourceRepository;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  /** Lista todas as versões cadastradas sem inferir estado no frontend. */
  @Transactional(readOnly = true)
  public List<BusinessProcessDefinitionResponse> list() {
    return repository.findAllByOrderByNameAscVersionNumberDesc().stream()
        .map(this::response)
        .toList();
  }

  /** Busca uma versão pelo identificador auditável. */
  @Transactional(readOnly = true)
  public BusinessProcessDefinitionResponse get(Long id) {
    return response(required(id));
  }

  /** Cadastra uma versão inicialmente em rascunho após validar o grafo. */
  @Transactional
  public BusinessProcessDefinitionResponse create(BusinessProcessDefinitionRequest request) {
    String code = request.processCode().trim();
    if (repository.findByProcessCodeAndVersionNumber(code, request.versionNumber()).isPresent()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta versão do processo já existe.");
    }
    validateEquivalentProcess(request.name(), code, null);
    validateResponsibilityBoundary(request.processType(), request.parentProcessCode(), code);
    validateDiagram(request.diagram());
    BusinessProcessDefinition value = new BusinessProcessDefinition();
    value.setProcessCode(code);
    value.setName(request.name().trim());
    value.setPurpose(request.purpose().trim());
    value.setOwnerName(request.ownerName().trim());
    value.setTriggerDescription(request.triggerDescription().trim());
    value.setOutcomeDescription(request.outcomeDescription().trim());
    value.setVersionNumber(request.versionNumber());
    value.setStatus("DRAFT");
    value.setTechnicalReference(trimToNull(request.technicalReference()));
    value.setProcessType(request.processType().trim());
    value.setParentProcessCode(trimToNull(request.parentProcessCode()));
    value.setDiagramJson(write(request.diagram()));
    value.setCreatedAt(Instant.now(clock));
    return response(repository.save(value));
  }

  /** Atualiza uma versão em rascunho sem alterar versões publicadas ou aposentadas. */
  @Transactional
  public BusinessProcessDefinitionResponse updateDraft(
      Long id, BusinessProcessDefinitionRequest request) {
    BusinessProcessDefinition value = required(id);
    if (!"DRAFT".equals(value.getStatus())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Somente versões em rascunho podem ser editadas.");
    }
    if (!value.getProcessCode().equals(request.processCode().trim())
        || !Objects.equals(value.getVersionNumber(), request.versionNumber())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Código e número da versão não podem ser alterados.");
    }
    validateEquivalentProcess(request.name(), value.getProcessCode(), value.getId());
    validateResponsibilityBoundary(
        request.processType(), request.parentProcessCode(), value.getProcessCode());
    validateDiagram(request.diagram());
    applyEditableFields(value, request);
    return response(repository.save(value));
  }

  /** Publica uma versão válida e aposenta a versão anteriormente vigente. */
  @Transactional
  public BusinessProcessDefinitionResponse publish(Long id) {
    BusinessProcessDefinition selected = required(id);
    if ("PUBLISHED".equals(selected.getStatus())) {
      return response(selected);
    }
    validateResponsibilityBoundary(
        processType(selected), selected.getParentProcessCode(), selected.getProcessCode());
    validateDiagram(read(selected.getDiagramJson()));
    Instant now = Instant.now(clock);
    repository.findAllByProcessCodeOrderByVersionNumberDesc(selected.getProcessCode()).stream()
        .filter(item -> "PUBLISHED".equals(item.getStatus()))
        .forEach(item -> item.setStatus("RETIRED"));
    selected.setStatus("PUBLISHED");
    selected.setPublishedAt(now);
    return response(repository.save(selected));
  }

  /** Exclui somente rascunho sem tarefas vinculadas, preservando histórico operacional. */
  @Transactional
  public void deleteDraft(Long id) {
    BusinessProcessDefinition value = required(id);
    if (!"DRAFT".equals(value.getStatus())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Somente versões em rascunho podem ser excluídas.");
    }
    if (agentTaskRepository.existsByProcessDefinitionId(id)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "O rascunho possui tarefas vinculadas e não pode ser excluído.");
    }
    repository.delete(value);
  }

  /** Impede que códigos diferentes cadastrem processos com nomes semanticamente equivalentes. */
  private void validateEquivalentProcess(String name, String processCode, Long ignoredId) {
    String normalizedName = normalizeName(name);
    boolean duplicate =
        repository.findAllByOrderByNameAscVersionNumberDesc().stream()
            .filter(item -> !Objects.equals(item.getId(), ignoredId))
            .filter(item -> !item.getProcessCode().equals(processCode))
            .anyMatch(item -> normalizeName(item.getName()).equals(normalizedName));
    if (duplicate) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Já existe um processo equivalente. Crie uma nova versão do processo existente.");
    }
  }

  /** Normaliza acentos, caixa e pontuação para comparar a identidade comercial do processo. */
  private String normalizeName(String value) {
    String withoutAccents =
        Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "");
    return withoutAccents.replaceAll("[^a-z0-9]+", " ").trim();
  }

  /** Garante que subprocessos tenham um único processo de valor pai vigente. */
  private void validateResponsibilityBoundary(
      String processType, String parentProcessCode, String processCode) {
    String normalizedType = processType == null ? "" : processType.trim();
    String normalizedParent = trimToNull(parentProcessCode);
    if ("VALUE_PROCESS".equals(normalizedType)) {
      if (normalizedParent != null) {
        throw invalid("Processo de valor não pode declarar processo pai.");
      }
      return;
    }
    if (!"SUBPROCESS".equals(normalizedType)) {
      throw invalid("O tipo deve ser VALUE_PROCESS ou SUBPROCESS.");
    }
    if (normalizedParent == null || normalizedParent.equals(processCode)) {
      throw invalid("Subprocesso exige um processo de valor pai diferente dele próprio.");
    }
    BusinessProcessDefinition parent =
        repository
            .findFirstByProcessCodeAndStatusOrderByVersionNumberDesc(normalizedParent, "PUBLISHED")
            .orElseThrow(() -> invalid("O processo de valor pai precisa estar publicado."));
    if (!"VALUE_PROCESS".equals(processType(parent))) {
      throw invalid("O pai de um subprocesso precisa ser um processo de valor.");
    }
  }

  /** Valida integridade BPM mínima: eventos únicos e fluxos apontando para nós existentes. */
  private void validateDiagram(JsonNode diagram) {
    JsonNode nodes = diagram.path("nodes");
    JsonNode flows = diagram.path("flows");
    if (!nodes.isArray() || nodes.size() < 3 || !flows.isArray() || flows.isEmpty()) {
      throw invalid("O diagrama deve ter ao menos início, atividade, fim e seus fluxos.");
    }
    Set<String> ids = new HashSet<>();
    int starts = 0;
    int ends = 0;
    for (JsonNode node : nodes) {
      String id = node.path("id").asText("").trim();
      String type = node.path("type").asText("").trim();
      if (id.isEmpty() || node.path("label").asText("").isBlank() || !ids.add(id)) {
        throw invalid("Cada elemento precisa de id e nome únicos.");
      }
      starts += "START".equals(type) ? 1 : 0;
      ends += "END".equals(type) ? 1 : 0;
      if (!Set.of("START", "TASK", "GATEWAY", "END").contains(type)) {
        throw invalid("Todo elemento deve possuir um tipo BPM reconhecido.");
      }
      validateExecutionResource(node, type);
      validateDocumentOutput(node, type);
      validateSubprocessReference(node, type);
    }
    if (starts != 1 || ends != 1) {
      throw invalid("O processo deve ter exatamente um início e um fim.");
    }
    for (JsonNode flow : flows) {
      if (!ids.contains(flow.path("from").asText()) || !ids.contains(flow.path("to").asText())) {
        throw invalid("Todo fluxo deve conectar elementos existentes.");
      }
    }
  }

  /** Valida o recurso opcional somente em atividades e contra o catálogo ativo do backend. */
  private void validateExecutionResource(JsonNode node, String nodeType) {
    String resourceCode = node.path("executionResourceCode").asText("").trim();
    if (resourceCode.isEmpty()) return;
    if (!"TASK".equals(nodeType)) {
      throw invalid("Somente atividades podem exigir um recurso especializado.");
    }
    if (resourceCode.length() > 100 || !resourceCode.matches("[a-z0-9]+(?:-[a-z0-9]+)*")) {
      throw invalid("O código do recurso especializado é inválido.");
    }
    if (executionResourceRepository == null
        || executionResourceRepository.findByResourceCodeAndActiveTrue(resourceCode).isEmpty()) {
      throw invalid("O recurso especializado informado não está disponível.");
    }
  }

  /** Valida a saída documental opcional somente em atividades executáveis. */
  private void validateDocumentOutput(JsonNode node, String nodeType) {
    JsonNode documentOutput = node.path("documentOutput");
    if (documentOutput.isMissingNode() || documentOutput.isNull()) return;
    if (!"TASK".equals(nodeType)) {
      throw invalid("Somente atividades podem declarar um documento como saída do objetivo.");
    }
    String label = documentOutput.path("label").asText("").trim();
    if (!documentOutput.isObject() || label.isEmpty() || label.length() > 160) {
      throw invalid("A saída documental exige um nome entre 1 e 160 caracteres.");
    }
  }

  /** Valida a chamada explícita de subprocesso e impede uma segunda execução implícita. */
  private void validateSubprocessReference(JsonNode node, String nodeType) {
    String subprocessCode = node.path("subprocessCode").asText("").trim();
    if (subprocessCode.isEmpty()) return;
    if (!"TASK".equals(nodeType)) {
      throw invalid("Somente atividades podem chamar subprocessos.");
    }
    if (!node.path("executionResourceCode").asText("").isBlank()) {
      throw invalid("Uma atividade não pode executar recurso e subprocesso ao mesmo tempo.");
    }
    if (!subprocessCode.matches("[a-z0-9]+(?:-[a-z0-9]+)*")) {
      throw invalid("O código do subprocesso é inválido.");
    }
    BusinessProcessDefinition subprocess =
        repository
            .findFirstByProcessCodeAndStatusOrderByVersionNumberDesc(subprocessCode, "PUBLISHED")
            .orElseThrow(() -> invalid("O subprocesso informado precisa estar publicado."));
    if (!"SUBPROCESS".equals(processType(subprocess))) {
      throw invalid("A atividade deve apontar para uma definição classificada como subprocesso.");
    }
  }

  /** Copia os campos que podem mudar enquanto a definição ainda é rascunho. */
  private void applyEditableFields(
      BusinessProcessDefinition value, BusinessProcessDefinitionRequest request) {
    value.setName(request.name().trim());
    value.setPurpose(request.purpose().trim());
    value.setOwnerName(request.ownerName().trim());
    value.setTriggerDescription(request.triggerDescription().trim());
    value.setOutcomeDescription(request.outcomeDescription().trim());
    value.setTechnicalReference(trimToNull(request.technicalReference()));
    value.setProcessType(request.processType().trim());
    value.setParentProcessCode(trimToNull(request.parentProcessCode()));
    value.setDiagramJson(write(request.diagram()));
  }

  /** Converte uma entidade no contrato oficial da tela. */
  private BusinessProcessDefinitionResponse response(BusinessProcessDefinition value) {
    BusinessProcessDefinition parent =
        value.getParentProcessCode() == null
            ? null
            : repository
                .findFirstByProcessCodeAndStatusOrderByVersionNumberDesc(
                    value.getParentProcessCode(), "PUBLISHED")
                .orElse(null);
    return new BusinessProcessDefinitionResponse(
        value.getId(),
        value.getProcessCode(),
        value.getName(),
        value.getPurpose(),
        value.getOwnerName(),
        value.getTriggerDescription(),
        value.getOutcomeDescription(),
        value.getVersionNumber(),
        value.getStatus(),
        value.getTechnicalReference(),
        read(value.getDiagramJson()),
        value.getCreatedAt(),
        value.getPublishedAt(),
        processType(value),
        value.getParentProcessCode(),
        parent == null ? null : parent.getId(),
        parent == null ? null : parent.getName());
  }

  /** Interpreta registros anteriores à classificação como processos de valor. */
  private String processType(BusinessProcessDefinition value) {
    return value.getProcessType() == null ? "VALUE_PROCESS" : value.getProcessType();
  }

  /** Obtém uma versão existente ou responde 404. */
  private BusinessProcessDefinition required(Long id) {
    return repository
        .findById(id)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Processo não encontrado."));
  }

  /** Serializa o diagrama estruturado para persistência auditável. */
  private String write(JsonNode value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException ex) {
      throw invalid("Diagrama inválido.");
    }
  }

  /** Lê o diagrama persistido sem ocultar corrupção do contrato. */
  private JsonNode read(String value) {
    try {
      return objectMapper.readTree(value);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Diagrama persistido inválido.", ex);
    }
  }

  /** Produz erro de validação consistente para a API administrativa. */
  private ResponseStatusException invalid(String message) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }

  /** Normaliza referência técnica opcional. */
  private String trimToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}

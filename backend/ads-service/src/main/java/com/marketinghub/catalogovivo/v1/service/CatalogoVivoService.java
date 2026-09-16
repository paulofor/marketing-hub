package com.marketinghub.catalogovivo.v1.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.catalogovivo.v1.service.catalog.*;
import com.marketinghub.catalogovivo.v1.service.commands.*;
import com.marketinghub.catalogovivo.v1.service.pending.CatalogPromptResponse;
import com.marketinghub.repository.jdbc.catalogovivo.CatalogoVivoRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessActivityDefinitionRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: governar versões textuais e fixar contratos de instrução nas tarefas Opala. */
@Service
@RequiredArgsConstructor
@Slf4j
public class CatalogoVivoService {
  public static final String PROCESS = "opala-commercial-preparation-v1";
  private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{([^{}]+)}}");
  private final CatalogoVivoRepository repository;
  private final BusinessProcessDefinitionRepository processes;
  private final BusinessProcessActivityDefinitionRepository activities;
  private final ExperimentRepository experiments;
  private final ObjectMapper json;

  /** Consulta conjunto completo e todos os impedimentos de uma vez, sem mutar o catálogo. */
  @Transactional(readOnly = true)
  public CatalogResponse catalog() {
    return describe(processId());
  }

  /** Publica no harness somente as versões ativas do agente consultado, sem auditorias extensas. */
  @Transactional(readOnly = true)
  public List<CatalogResponse.Item> harness(String agentKey) {
    return repository.bindingsForAgent(agentKey).stream()
        .map(
            b ->
                new CatalogResponse.Item(
                    b,
                    b.activeVersionId() == null
                        ? List.of()
                        : repository.version(b.activeVersionId()).stream().toList(),
                    List.of()))
        .toList();
  }

  /** Resolve somente a versão v1 formalmente migrada; versões futuras exigem seu próprio lote. */
  public long processId() {
    return processes
        .findFirstByProcessCodeAndStatusOrderByVersionNumberDesc(PROCESS, "PUBLISHED")
        .filter(p -> p.getVersionNumber() == 1)
        .orElseThrow(() -> conflict("Subprocesso Opala v1 indisponível."))
        .getId();
  }

  /**
   * Monta o relatório com cobertura derivada das atividades, sem outra lista manual de caminhos.
   */
  private CatalogResponse describe(long processId) {
    var bindings = repository.bindings(processId);
    var issues = coverage(processId, bindings, null);
    return new CatalogResponse(
        "Catálogo Vivo — Piloto Opala",
        processId,
        "DATABASE",
        issues.isEmpty(),
        issues,
        bindings.stream()
            .map(
                b ->
                    new CatalogResponse.Item(
                        b,
                        repository.versions(b.id()),
                        repository.events(b.id()),
                        repository.usages(b.id())))
            .toList(),
        repository.pinnedCount(processId),
        repository.failureCount(processId));
  }

  /** Registra novo rascunho, mantendo todas as versões anteriores imutáveis. */
  @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
  public CatalogVersion draft(long bindingId, CatalogDraftRequest request) {
    var binding = requiredBinding(bindingId);
    repository.lockProcess(binding.processId());
    validateText(request.text());
    requireText(request.operatorName(), 160, "Responsável");
    requireText(request.reason(), 1000, "Motivo");
    long id =
        repository.insertDraft(
            bindingId, request.text(), sha256(request.text()), request.operatorName().trim());
    repository.audit(
        bindingId, id, "DRAFT_CREATED", request.operatorName().trim(), request.reason().trim());
    return requiredVersion(id);
  }

  /** Registra revisão explícita do hash exibido ao operador, sem aprovar gasto ou publicação. */
  @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
  public CatalogVersion review(long versionId, CatalogReviewRequest request) {
    var version = requiredVersion(versionId);
    var binding = requiredBinding(version.bindingId());
    repository.lockProcess(binding.processId());
    version = repository.currentVersion(versionId).orElseThrow();
    if (!Objects.equals(version.sha256(), request.expectedSha256()))
      throw conflict("O texto mudou; consulte e revise a versão atual.");
    requireText(request.operatorName(), 160, "Responsável");
    requireText(request.reason(), 1000, "Parecer");
    validateVersion(binding, version, false);
    if (!"REVIEWED".equals(version.status())) {
      repository.review(versionId, request.operatorName().trim(), request.reason().trim());
      repository.audit(
          binding.id(),
          versionId,
          "REVIEWED",
          request.operatorName().trim(),
          request.reason().trim());
    }
    return requiredVersion(versionId);
  }

  /**
   * Ativa ou recupera versões revisadas em transação única, recusando concorrência desatualizada.
   */
  @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
  public CatalogResponse activate(CatalogActivationRequest request) {
    long processId = processId();
    repository.lockProcess(processId);
    var bindings = repository.currentBindings(processId);
    requireText(request.operatorName(), 160, "Responsável");
    requireText(request.reason(), 1000, "Motivo");
    var ids = new HashSet<>(bindings.stream().map(CatalogBinding::id).toList());
    if (request.versions() == null
        || request.expectedActiveVersions() == null
        || !ids.equals(request.versions().keySet())
        || !ids.equals(request.expectedActiveVersions().keySet()))
      throw conflict("Selecione o conjunto completo das atividades Opala.");
    for (var binding : bindings)
      if (!Objects.equals(
          binding.activeVersionId(), request.expectedActiveVersions().get(binding.id())))
        throw conflict("Outra ativação ocorreu. Atualize o catálogo antes de confirmar.");
    var issues = coverage(processId, bindings, request.versions());
    if (!issues.isEmpty()) throw conflict(String.join("; ", issues));
    for (var binding : bindings) {
      long selected = request.versions().get(binding.id());
      if (!Objects.equals(binding.activeVersionId(), selected)) {
        repository.activate(binding.id(), selected);
        repository.audit(
            binding.id(),
            selected,
            "ACTIVATED",
            request.operatorName().trim(),
            request.reason().trim());
      }
    }
    return describe(processId);
  }

  /** Confere cobertura e compatibilidade antes de disponibilizar um subprocesso ao executor. */
  public void requireReady(long processId) {
    var issues = coverage(processId, repository.bindings(processId), null);
    if (!issues.isEmpty()) throw conflict(String.join("; ", issues));
  }

  /** Lista todas as atividades de agente ausentes ou inconsistentes no pacote de instruções. */
  private List<String> coverage(
      long processId, List<CatalogBinding> bindings, Map<Long, Long> selected) {
    return coverage(processId, bindings, selected, false);
  }

  /**
   * Usa leituras confirmadas quando a fixação participa de transação BPM iniciada anteriormente.
   */
  private List<String> coverage(
      long processId, List<CatalogBinding> bindings, Map<Long, Long> selected, boolean current) {
    var issues = new ArrayList<String>();
    var definitions = activities.findAllByProcessDefinitionIdOrderByIdAsc(processId);
    for (var definition : definitions) {
      try {
        var owners = json.readTree(definition.getDefinitionJson()).path("responsibleAgentKeys");
        if (!owners.isArray() || owners.isEmpty()) continue;
        var matches =
            bindings.stream().filter(b -> b.activityDefinitionId() == definition.getId()).toList();
        if (matches.size() != 1) {
          issues.add(definition.getActivityId() + ": vínculo obrigatório ausente ou ambíguo.");
          continue;
        }
        var binding = matches.getFirst();
        if (owners.size() != 1
            || !binding.agentKey().equals(owners.get(0).asText())
            || !"PDE".equals(binding.productTypeCode()))
          issues.add(definition.getActivityId() + ": agente ou tipo incompatível.");
        Long id = selected == null ? binding.activeVersionId() : selected.get(binding.id());
        if (id == null) {
          issues.add(definition.getActivityId() + ": sem versão ativa.");
          continue;
        }
        validateVersion(
            binding,
            current ? repository.currentVersion(id).orElseThrow() : requiredVersion(id),
            true);
      } catch (Exception ex) {
        log.error(
            "catalogo-vivo coverage processId={} activityId={}",
            processId,
            definition.getActivityId(),
            ex);
        issues.add(definition.getActivityId() + ": " + ex.getMessage());
      }
    }
    if (definitions.isEmpty()) issues.add("Processo sem atividades persistidas.");
    for (var b : bindings)
      if (definitions.stream().noneMatch(a -> a.getId() == b.activityDefinitionId()))
        issues.add("Vínculo órfão: " + b.id());
    return issues;
  }

  /** Fixa a versão ao criar uma tarefa; ausência de catálogo vira bloqueio persistido e visível. */
  @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
  public void pin(AgentTask task) {
    if (!migrated(task) || repository.pinned(task.getId()).isPresent()) return;
    try {
      long processId = task.getProcessDefinition().getId();
      repository.lockProcess(processId);
      var bindings = repository.currentBindings(processId);
      var issues = coverage(processId, bindings, null, true);
      if (!issues.isEmpty()) throw conflict(String.join("; ", issues));
      var matches =
          bindings.stream()
              .filter(
                  b ->
                      b.activityId().equals(task.getProcessActivityId())
                          && b.agentId() == task.getAssignedAgent().getId())
              .toList();
      if (matches.size() != 1) throw conflict("Atividade sem vínculo único para este agente.");
      var binding = matches.getFirst();
      if (task.getSourceReference() == null
          || !task.getSourceReference().matches("experiment:[1-9][0-9]{0,17}"))
        throw conflict("O prompt Opala exige experimento explícito.");
      var experiment =
          experiments
              .findById(Long.valueOf(task.getSourceReference().substring(11)))
              .orElseThrow(() -> conflict("O experimento da tarefa não está disponível."));
      if (experiment.getProduct() == null
          || experiment.getProduct().getProductTypeDefinition() == null
          || !Objects.equals(
              experiment.getProduct().getProductTypeDefinition().getId(), binding.productTypeId()))
        throw conflict("O experimento não pertence ao tipo deste prompt.");
      Long versionId =
          task.getActivityInstance() == null
              ? binding.activeVersionId()
              : repository
                  .previousVersion(task.getActivityInstance().getId(), binding.id())
                  .orElse(binding.activeVersionId());
      validateVersion(binding, repository.currentVersion(versionId).orElseThrow(), true);
      repository.pin(task.getId(), binding.id(), versionId, task.getSourceReference());
    } catch (ResponseStatusException | IllegalStateException ex) {
      log.error(
          "catalogo-vivo pin taskId={} source={}", task.getId(), task.getSourceReference(), ex);
      task.setStatus("BLOCKED");
      task.setExecutionError(ex.getMessage());
      task.setBlockerCategory("TECHNICAL_FAILURE");
      task.setBlockerAction(
          "CATALOGO_VIVO: Revise o conjunto em /catalogo-vivo/opala e solicite nova tentativa da atividade.");
    }
  }

  /** Entrega apenas a versão já fixada; indisponibilidade nunca autoriza fallback em arquivo. */
  @Transactional(readOnly = true)
  public CatalogPromptResponse prompt(AgentTask task) {
    if (!migrated(task)) return null;
    var prompt =
        repository
            .pinned(task.getId())
            .orElseThrow(
                () ->
                    conflict(
                        "Tarefa Opala sem prompt fixado. Revise o Catálogo Vivo e solicite nova tentativa."));
    if (!Objects.equals(repository.pinnedSource(task.getId()), task.getSourceReference())
        || !prompt.agentKey().equals(task.getAssignedAgent().getAgentKey())
        || !prompt.activityId().equals(task.getProcessActivityId())
        || prompt.processVersion() != task.getProcessDefinition().getVersionNumber())
      throw conflict("Prompt fixado incompatível com a tarefa.");
    return prompt;
  }

  /** Reconhece somente o subprocesso explicitamente migrado, preservando demais consumidores. */
  private boolean migrated(AgentTask task) {
    return task != null
        && task.getProcessDefinition() != null
        && PROCESS.equals(task.getProcessDefinition().getProcessCode());
  }

  /**
   * Exige instrução fixada e contexto da própria tarefa no texto efetivamente enviado ao modelo.
   */
  public void validateExecution(AgentTask task, String activityPrompt) {
    if (!migrated(task)) return;
    var p = prompt(task);
    String[] pieces = p.text().split(Pattern.quote("{{TASK_CONTEXT}}"), -1);
    if (activityPrompt == null
        || activityPrompt.length() < pieces[0].length() + pieces[1].length()
        || !activityPrompt.startsWith(pieces[0])
        || !activityPrompt.endsWith(pieces[1]))
      throw conflict("A instrução auditada diverge da versão fixada no Catálogo Vivo.");
    try {
      var context =
          json.readTree(
              activityPrompt.substring(
                  pieces[0].length(), activityPrompt.length() - pieces[1].length()));
      if (context == null
          || !context.isObject()
          || context.path("taskId").asLong(-1) != task.getId()
          || !context.path("agentKey").asText().equals(task.getAssignedAgent().getAgentKey())
          || !context.path("activityId").asText().equals(task.getProcessActivityId())
          || !context.path("sourceReference").asText().equals(task.getSourceReference())
          || !context.path("processCode").asText().equals(PROCESS)
          || context.path("processVersion").asInt(-1)
              != task.getProcessDefinition().getVersionNumber()
          || context.path("catalogPromptReference").path("versionId").asLong(-1) != p.versionId()
          || !context.path("catalogPromptReference").path("sha256").asText().equals(p.sha256()))
        throw conflict("O contexto auditado não corresponde à tarefa e à versão fixadas.");
    } catch (java.io.IOException ex) {
      log.error("catalogo-vivo contexto inválido taskId={}", task.getId(), ex);
      throw conflict("O contexto auditado não contém JSON válido.");
    }
  }

  /** Impede conclusão de atividade de agente migrada sem comprovar o uso de seu prompt textual. */
  public void requireModelAudit(AgentTask task, String mode, boolean terminalCompletion) {
    if (migrated(task) && terminalCompletion && !"MODEL".equals(mode))
      throw conflict(
          "A atividade Opala exige auditoria do modelo e da instrução fixada no catálogo.");
  }

  /** Valida propriedade, estado, texto e schema empacotado do executor antes de ativar ou usar. */
  private void validateVersion(CatalogBinding binding, CatalogVersion version, boolean reviewed) {
    if (version.bindingId() != binding.id()) throw conflict("A versão pertence a outra atividade.");
    if (reviewed && !"REVIEWED".equals(version.status()))
      throw conflict("Versão ainda não revisada.");
    validateText(version.text());
    if (!sha256(version.text()).equals(version.sha256()))
      throw conflict("Hash do texto divergente.");
    try {
      var resource =
          new ClassPathResource(
              "agent-behavior-files/"
                  + binding.executorModule()
                  + "/src/main/resources/"
                  + binding.schemaId());
      var content = resource.getContentAsString(StandardCharsets.UTF_8);
      if (!sha256(content).equals(binding.schemaSha256()) || !json.readTree(content).isObject())
        throw conflict("Schema incompatível com o executor empacotado.");
    } catch (java.io.IOException ex) {
      log.error(
          "catalogo-vivo schema bindingId={} schemaId={}", binding.id(), binding.schemaId(), ex);
      throw conflict("Schema do executor indisponível para validação.");
    }
  }

  /** Permite exatamente o placeholder de contexto do contrato Opala. */
  public static void validateText(String text) {
    requireText(text, 100000, "Texto");
    var matcher = PLACEHOLDER.matcher(text);
    int count = 0;
    while (matcher.find()) {
      if (!"TASK_CONTEXT".equals(matcher.group(1)))
        throw conflict("Placeholder desconhecido: " + matcher.group(1));
      count++;
    }
    if (count != 1
        || text.replace("{{TASK_CONTEXT}}", "").contains("{{")
        || text.replace("{{TASK_CONTEXT}}", "").contains("}}"))
      throw conflict("O texto deve conter uma única ocorrência de {{TASK_CONTEXT}}.");
  }

  /** Exige campos administrativos limitados também quando a chamada não passa pelo controller. */
  private static void requireText(String text, int max, String field) {
    if (text == null || text.isBlank() || text.length() > max)
      throw conflict(field + " obrigatório, com até " + max + " caracteres.");
  }

  /** Carrega vínculo somente do piloto formalmente migrado. */
  private CatalogBinding requiredBinding(long id) {
    var binding = repository.binding(id).orElseThrow(() -> conflict("Vínculo não encontrado."));
    if (binding.processId() != processId()) throw conflict("Vínculo fora do piloto Opala v1.");
    return binding;
  }

  /** Exige versão existente, protegida pelas chaves estrangeiras do banco. */
  private CatalogVersion requiredVersion(long id) {
    return repository.version(id).orElseThrow(() -> conflict("Versão não encontrada: " + id));
  }

  /** Calcula identidade exata do texto UTF-8 preservado na auditoria. */
  public static String sha256(String text) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
    } catch (java.security.NoSuchAlgorithmException ex) {
      log.error("catalogo-vivo sha256 indisponível", ex);
      throw new IllegalStateException("SHA-256 indisponível", ex);
    }
  }

  /** Expõe um impedimento acionável sem converter inconsistência em execução bem-sucedida. */
  private static ResponseStatusException conflict(String message) {
    return new ResponseStatusException(HttpStatus.CONFLICT, message);
  }
}

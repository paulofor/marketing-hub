package com.marketinghub.businessprocess.automation.v1.service.continuity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.BusinessProcessActivityInstance;
import com.marketinghub.businessprocess.automation.v1.*;
import com.marketinghub.businessprocess.execution.service.BusinessProcessActivityExecutionService;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import com.marketinghub.repository.jpa.businessprocess.*;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.processautomation.*;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Responsabilidade: preparar continuidade pausada com compatibilidade explícita e provas
 * preservadas.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessVersionContinuityService {
  private final ProductRepository products;
  private final LearningSalesCycleRepository cycles;
  private final BusinessProcessDefinitionRepository processes;
  private final BusinessProcessChainDefinitionRepository chains;
  private final BusinessProcessActivityDefinitionRepository definitions;
  private final BusinessProcessActivityInstanceRepository instances;
  private final ProcessRunRepository runs;
  private final ProcessRunEventRepository events;
  private final BusinessProcessActivityExecutionService activities;
  private final ProcessVersionContinuityLookup lookup;
  private final ObjectMapper json;

  @org.springframework.beans.factory.annotation.Autowired(required = false)
  private com.marketinghub.product.executionprofile.v1.service.ExecutionProfileContext
      executionProfile;

  /** Responsabilidade: apresentar a prova original e os campos que precisam de nova conferência. */
  public record Output(
      String activityId,
      String name,
      Long instanceId,
      boolean accepted,
      List<String> changedFields,
      com.fasterxml.jackson.databind.JsonNode sourceContract,
      com.fasterxml.jackson.databind.JsonNode targetContract,
      com.fasterxml.jackson.databind.JsonNode evidence,
      List<Long> taskIds) {}

  /** Responsabilidade: apresentar uma proposta sem iniciar execução nem modificar autorizações. */
  public record Preview(
      Long productId,
      Long cycleId,
      Long chainId,
      String reference,
      Long sourceProcessId,
      Long targetProcessId,
      Long sourceRunId,
      Long successorRunId,
      String fingerprint,
      List<Output> outputs,
      String reason,
      boolean canPrepare,
      String url) {}

  /**
   * Responsabilidade: registrar a conferência do operador vinculada ao conteúdo que ele consultou.
   */
  public record Request(
      String fingerprint,
      String operatorName,
      String reason,
      Map<String, String> compatibilityEvidence) {}

  /** Compara a versão histórica com a publicada, mantendo o mesmo produto/ciclo/experimento. */
  @Transactional(readOnly = true)
  public Preview preview(Long productId, Long cycleId, Long requestedProcessId) {
    var product =
        products.findById(productId).orElseThrow(() -> conflict("Produto não encontrado."));
    var cycle =
        cycles
            .findById(cycleId)
            .filter(c -> productId.equals(c.getProductId()))
            .orElseThrow(() -> conflict("Ciclo não pertence ao produto."));
    var requested =
        processes
            .findById(requestedProcessId)
            .orElseThrow(() -> conflict("Processo não encontrado."));
    var originalChain = chains.findById(cycle.getChainDefinitionId()).orElseThrow();
    var originalSource =
        originalChain.getItems().stream()
            .map(i -> i.getProcessDefinition())
            .filter(p -> p.getProcessCode().equals(requested.getProcessCode()))
            .findFirst()
            .orElseThrow(() -> conflict("Processo não pertence à cadeia original do ciclo."));
    String reference =
        com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleExecutionContext
            .constructionSource(product, cycle);
    if (!"pde-construction-approval".equals(originalSource.getProcessCode()))
      reference = "experiment:" + cycle.getExperimentId();
    // Uma rota histórica conserva seu recibo; o destino mais recente usa seu predecessor.
    var recorded = lookup.bindings(productId, cycleId, reference);
    Long sourceId = originalSource.getId();
    if (!requested.getId().equals(sourceId)) {
      var incoming =
          recorded.stream().filter(b -> requested.getId().equals(b.targetProcessId())).findFirst();
      sourceId =
          incoming.isPresent()
              ? ("RETIRED".equals(requested.getStatus())
                  ? requested.getId()
                  : incoming.get().sourceProcessId())
              : lookup.effective(productId, cycleId, reference, originalSource.getId());
    }
    var source = processes.findById(sourceId).orElseThrow();
    var inheritedTasks = lookup.reusedTasks(productId, cycleId, reference, source.getId());
    var run =
        runs.findFirstByProductIdAndProcessDefinitionIdAndChainDefinitionIdAndLearningCycleIdAndSourceReferenceOrderByIdDesc(
                productId, source.getId(), cycle.getChainDefinitionId(), cycleId, reference)
            .orElseThrow(() -> conflict("Não há execução histórica neste contexto."));
    var binding =
        lookup.bindings(productId, cycleId, reference).stream()
            .filter(b -> source.getId().equals(b.sourceProcessId()))
            .findFirst();
    var target =
        binding.isPresent()
            ? processes.findById(binding.get().targetProcessId()).orElseThrow()
            : chains
                .findFirstByChainCodeAndStatusOrderByVersionNumberDesc(
                    originalChain.getChainCode(), "PUBLISHED")
                .orElseThrow(() -> conflict("Não há cadeia publicada nesta família."))
                .getItems()
                .stream()
                .map(i -> i.getProcessDefinition())
                .filter(
                    p ->
                        p.getProcessCode().equals(source.getProcessCode())
                            && "PUBLISHED".equals(p.getStatus()))
                .findFirst()
                .orElseThrow(() -> conflict("Não há destino publicado para este processo."));
    var history =
        activities.productProcessExecutions(
            source.getId(), productId, cycleId, cycle.getChainDefinitionId(), false, reference);
    var old = definitions.findAllByProcessDefinitionIdOrderByIdAsc(source.getId());
    var current = definitions.findAllByProcessDefinitionIdOrderByIdAsc(target.getId());
    var outputs = new ArrayList<Output>();
    for (var a : history.activities()) {
      if (!a.selectedVersionActivity()) continue;
      var before =
          old.stream()
              .filter(d -> d.getActivityId().equals(a.activityId()))
              .findFirst()
              .orElseThrow();
      var after =
          current.stream()
              .filter(d -> d.getActivityId().equals(a.activityId()))
              .findFirst()
              .orElse(null);
      outputs.add(
          new Output(
              a.activityId(),
              a.activityName(),
              a.activityInstanceId(),
              a.objectiveAchieved(),
              ProcessContractCompatibility.differences(
                  tree(before.getDefinitionJson()),
                  after == null ? null : tree(after.getDefinitionJson())),
              tree(before.getDefinitionJson()),
              after == null ? null : tree(after.getDefinitionJson()),
              a.activityInstanceId() == null
                  ? json.valueToTree(a.stateEvidence())
                  : tree(
                      instances
                          .findById(a.activityInstanceId())
                          .map(BusinessProcessActivityInstance::getObjectiveEvidenceJson)
                          .orElse(null)),
              a.tasks().stream()
                  .filter(
                      t ->
                          (source.getId().equals(t.processDefinitionId())
                                  || inheritedTasks.contains(t.taskId()))
                              && "COMPLETED".equals(t.status()))
                  .map(t -> t.taskId())
                  .toList()));
    }
    boolean active =
        history.activities().stream()
            .flatMap(a -> a.tasks().stream())
            .anyMatch(t -> Set.of("PENDING", "IN_PROGRESS").contains(t.status()));
    boolean structuralChange =
        outputs.isEmpty()
            || !old.stream()
                .map(d -> d.getActivityId())
                .collect(java.util.stream.Collectors.toSet())
                .equals(
                    current.stream()
                        .map(d -> d.getActivityId())
                        .collect(java.util.stream.Collectors.toSet()))
            || outputs.stream()
                .anyMatch(
                    o ->
                        o.accepted()
                            && o.changedFields().stream()
                                .anyMatch(f -> !Set.of("/description", "/name").contains(f)));
    boolean profileConflict =
        executionProfile != null
            && executionProfile.bound(productId, reference).isPresent()
            && !executionProfile.pins(reference, target.getId());
    String reason =
        binding.isPresent()
            ? "Continuidade registrada. A retomada é uma ação separada e mantém os controles financeiros."
            : !"OPEN".equals(cycle.getStatus())
                ? "Ciclo encerrado; histórico preservado."
                : !"PAUSED".equals(run.getStatus()) || active
                    ? "Pause e aguarde as tarefas em andamento antes de preparar a continuidade."
                    : !"RETIRED".equals(source.getStatus()) || source.getId().equals(target.getId())
                        ? "Não há transição de versão retirada para publicada."
                        : outputs.stream()
                                .anyMatch(
                                    o ->
                                        o.accepted()
                                            && (o.instanceId() == null
                                                || o.targetContract() == null))
                            ? "Há entrega sem ocorrência ou contrato de destino; a transição exige mapeamento antes do reuso."
                            : null;
    if (binding.isEmpty() && structuralChange)
      reason =
          "O contrato estrutural da entrega mudou. Preserve as provas e revalide a integração antes de adotar esta versão.";
    if (binding.isEmpty() && profileConflict)
      reason =
          "A ficha desta referência fixa outra versão. Revise a composição pelo fluxo de fichas antes de preparar a continuidade.";
    String fingerprint =
        hash(
            encode(
                List.of(
                    run.getRevision(),
                    cycle.getRevision(),
                    source.getId(),
                    target.getId(),
                    target.getDiagramJson(),
                    outputs)));
    return new Preview(
        productId,
        cycleId,
        cycle.getChainDefinitionId(),
        reference,
        source.getId(),
        target.getId(),
        run.getId(),
        binding.map(ProcessVersionContinuityLookup.Binding::successorRunId).orElse(null),
        fingerprint,
        outputs,
        reason == null
            ? "Confira os requisitos e as provas das entregas alteradas. Preparar não inicia agentes nem aumenta limites."
            : reason,
        reason == null,
        url(productId, cycleId, cycle.getChainDefinitionId(), target.getId(), reference));
  }

  /**
   * Registra adoção idempotente e cópias referenciais das provas em transação, sem trabalho pago.
   */
  @Transactional
  public Preview prepare(Long productId, Long cycleId, Long processId, Request request) {
    products.findLockedById(productId).orElseThrow(() -> conflict("Produto não encontrado."));
    cycles.findLocked(productId, cycleId).orElseThrow(() -> conflict("Ciclo não encontrado."));
    var p = preview(productId, cycleId, processId);
    if (p.successorRunId() != null) return p;
    if (request == null || !Objects.equals(p.fingerprint(), request.fingerprint()))
      throw conflict("As provas mudaram. Atualize a conferência.");
    if (!p.canPrepare()) throw conflict(p.reason());
    requireText(request.operatorName(), 160, "Informe o responsável pela conferência.");
    requireText(request.reason(), 1000, "Informe o motivo da continuidade.");
    for (var o : p.outputs())
      if (o.accepted() && !o.changedFields().isEmpty())
        requireText(
            request.compatibilityEvidence() == null
                ? null
                : request.compatibilityEvidence().get(o.activityId()),
            4000,
            "Registre a evidência que comprova os novos requisitos de " + o.name() + ".");
    var sourceRun = runs.findById(p.sourceRunId()).orElseThrow();
    var next = new ProcessRun();
    next.setProductId(productId);
    next.setChainDefinitionId(p.chainId());
    next.setLearningCycleId(cycleId);
    next.setSourceReference(p.reference());
    next.setProcessDefinitionId(p.targetProcessId());
    next.setScopeKey(
        hash(
            productId
                + "|"
                + p.targetProcessId()
                + "|"
                + p.chainId()
                + "|"
                + cycleId
                + "|"
                + p.reference()));
    if (runs.findByScopeKey(next.getScopeKey()).isPresent())
      throw conflict("Já existe execução de destino; concilie-a antes de adotar outra versão.");
    next.setStatus("PAUSED");
    next.setReason(
        "Continuidade preparada; retomada separada sujeita aos gates e limites existentes.");
    next.setCreatedAt(Instant.now());
    next.setUpdatedAt(next.getCreatedAt());
    next.setLastReconciledAt(next.getCreatedAt());
    runs.saveAndFlush(next);
    events
        .findFirstByRunIdAndEventTypeOrderByIdDesc(sourceRun.getId(), "PAUSE_CONTEXT")
        .ifPresent(
            original -> {
              var inherited = new ProcessRunEvent();
              inherited.setRunId(next.getId());
              inherited.setEventType("PAUSE_CONTEXT");
              inherited.setStatus("PAUSED");
              inherited.setMessage(original.getMessage());
              inherited.setDetailsJson(original.getDetailsJson());
              inherited.setCreatedAt(next.getCreatedAt());
              events.saveAndFlush(inherited);
            });
    var reused = new ArrayList<Map<String, Object>>();
    for (var o : p.outputs())
      if (o.accepted()) {
        var original = instances.findById(o.instanceId()).orElseThrow();
        if (!original.isObjectiveAchieved()
            || !p.reference().equals(original.getSourceReference())
            || !p.sourceProcessId()
                .equals(original.getActivityDefinition().getProcessDefinition().getId()))
          throw conflict("A prova original mudou; refaça a conferência.");
        var definition =
            definitions
                .findByProcessDefinitionIdAndActivityId(p.targetProcessId(), o.activityId())
                .orElseThrow();
        if (instances
            .findFirstByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
                definition.getId(), p.reference())
            .isPresent())
          throw conflict("O destino já possui ocorrência; não é permitido sobrescrever provas.");
        var copy = new BusinessProcessActivityInstance();
        copy.setActivityDefinition(definition);
        copy.setSourceReference(p.reference());
        copy.setOccurrenceNumber(1);
        copy.setStatus(original.getStatus());
        copy.setObjectiveAchieved(true);
        copy.setEnteredAt(next.getCreatedAt());
        copy.setExitedAt(next.getCreatedAt());
        copy.setCreatedAt(next.getCreatedAt());
        copy.setUpdatedAt(next.getCreatedAt());
        copy.setCostCoverage("NOT_REPORTED");
        copy.setEvidenceQuality(original.getEvidenceQuality());
        // A referência de reuso não duplica o custo; o valor original continua no histórico
        // financeiro.
        copy.setObjectiveEvidenceJson(original.getObjectiveEvidenceJson());
        instances.saveAndFlush(copy);
        reused.add(
            Map.of(
                "activityId",
                o.activityId(),
                "sourceInstanceId",
                original.getId(),
                "targetInstanceId",
                copy.getId(),
                "taskIds",
                o.taskIds()));
      }
    next.setTotalActivities(p.outputs().size());
    next.setCompletedActivities(reused.size());
    next.setRemainingActivities(p.outputs().size() - reused.size());
    var event = new ProcessRunEvent();
    event.setRunId(sourceRun.getId());
    event.setEventType("VERSION_CONTINUATION");
    event.setStatus(sourceRun.getStatus());
    event.setMessage(
        "Continuidade preparada por " + request.operatorName() + ": " + request.reason());
    event.setActionKey("version-target:" + p.targetProcessId());
    event.setCreatedAt(next.getCreatedAt());
    event.setDetailsJson(
        encode(
            Map.of(
                "binding",
                new ProcessVersionContinuityLookup.Binding(
                    p.sourceProcessId(), p.targetProcessId(), next.getId(), p.chainId()),
                "fingerprint",
                p.fingerprint(),
                "operatorName",
                request.operatorName(),
                "reason",
                request.reason(),
                "outputs",
                p.outputs(),
                "reused",
                reused,
                "compatibilityEvidence",
                request.compatibilityEvidence() == null
                    ? Map.of()
                    : request.compatibilityEvidence())));
    events.saveAndFlush(event);
    return preview(productId, cycleId, processId);
  }

  /** Valida campos auditáveis antes de qualquer gravação. */
  private void requireText(String value, int max, String message) {
    if (value == null || value.isBlank() || value.length() > max) throw conflict(message);
  }

  /** Lê contratos preservando falha fechada e contexto de diagnóstico. */
  private com.fasterxml.jackson.databind.JsonNode tree(String value) {
    try {
      return value == null ? json.nullNode() : json.readTree(value);
    } catch (Exception ex) {
      log.error("Contrato inválido na continuidade de processo", ex);
      throw conflict("Contrato de continuidade inválido.");
    }
  }

  /** Serializa recibo estruturado sem descartar a causa de falhas. */
  private String encode(Object value) {
    try {
      return json.writeValueAsString(value);
    } catch (Exception ex) {
      log.error("Falha ao serializar recibo de continuidade", ex);
      throw new IllegalStateException(ex);
    }
  }

  /** Calcula identidade da conferência e da execução sem incluir credenciais. */
  private String hash(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              java.security.MessageDigest.getInstance("SHA-256")
                  .digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception ex) {
      log.error("Falha ao identificar continuidade", ex);
      throw new IllegalStateException(ex);
    }
  }

  /** Produz conflito funcional recuperável pela tela. */
  private ResponseStatusException conflict(String message) {
    return new ResponseStatusException(HttpStatus.CONFLICT, message);
  }

  /** Conserva a cadeia e referência originais na navegação para a versão adotada. */
  private String url(Long product, Long cycle, Long chain, Long process, String reference) {
    return "/products/"
        + product
        + "/value-chain-history/processes/"
        + process
        + "/activities?chainId="
        + chain
        + "&learningCycleId="
        + cycle
        + "&sourceReference="
        + java.net.URLEncoder.encode(reference, StandardCharsets.UTF_8);
  }
}

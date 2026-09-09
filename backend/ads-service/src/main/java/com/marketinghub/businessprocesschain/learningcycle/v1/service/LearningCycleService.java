package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleRules.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocesschain.BusinessProcessChainDefinition;
import com.marketinghub.businessprocesschain.learningcycle.v1.*;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.command.LearningCycleCommand;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.command.LearningCycleCommand.Action;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.createCycle.CreateLearningCycleRequest;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.getCycles.*;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.reconcileMeasurement.ReconcileLearningCycleMeasurementRequest;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.ExperimentType;
import com.marketinghub.repository.jpa.businessprocess.*;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.learningcycle.*;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: governar ciclos, decisões e memória comercial por experimento. */
@Service
@Slf4j
public class LearningCycleService {
  private final LearningSalesCycleRepository cycles;
  private final LearningSalesCycleEventRepository events;
  private final ProductRepository products;
  private final ExperimentRepository experiments;
  private final BusinessProcessChainDefinitionRepository chains;
  private final BusinessProcessDefinitionRepository processes;
  private final BusinessProcessActivityDefinitionRepository activities;
  private final LearningCycleJson json;
  private final LearningCycleBpmLedger ledger;
  private final LearningCycleEvidence evidence;
  private final Clock clock;
  private final LearningCycleVideoEvidence videoEvidence;
  private final LearningCycleOrganization organization;
  private final LearningCycleMeasurementCollector measurementCollector;

  @Autowired
  private com.marketinghub.businessprocesschain.learningcycle.v1.decision.service
          .LearningCycleDecisionApproval
      decisionApproval;

  /** Configura fontes oficiais, contratos e relógio de decisão. */
  @Autowired
  public LearningCycleService(
      LearningSalesCycleRepository cycles,
      LearningSalesCycleEventRepository events,
      ProductRepository products,
      ExperimentRepository experiments,
      BusinessProcessChainDefinitionRepository chains,
      BusinessProcessDefinitionRepository processes,
      BusinessProcessActivityDefinitionRepository activities,
      LearningCycleJson json,
      LearningCycleBpmLedger ledger,
      LearningCycleEvidence evidence,
      LearningCycleVideoEvidence videoEvidence,
      LearningCycleOrganization organization,
      LearningCycleMeasurementCollector measurementCollector) {
    this(
        cycles,
        events,
        products,
        experiments,
        chains,
        processes,
        activities,
        json,
        ledger,
        evidence,
        videoEvidence,
        organization,
        measurementCollector,
        Clock.systemUTC());
  }

  /** Permite homologar janelas e decisões com tempo determinístico. */
  LearningCycleService(
      LearningSalesCycleRepository cycles,
      LearningSalesCycleEventRepository events,
      ProductRepository products,
      ExperimentRepository experiments,
      BusinessProcessChainDefinitionRepository chains,
      BusinessProcessDefinitionRepository processes,
      BusinessProcessActivityDefinitionRepository activities,
      LearningCycleJson json,
      LearningCycleBpmLedger ledger,
      LearningCycleEvidence evidence,
      LearningCycleVideoEvidence videoEvidence,
      LearningCycleOrganization organization,
      LearningCycleMeasurementCollector measurementCollector,
      Clock clock) {
    this.cycles = cycles;
    this.events = events;
    this.products = products;
    this.experiments = experiments;
    this.chains = chains;
    this.processes = processes;
    this.activities = activities;
    this.json = json;
    this.ledger = ledger;
    this.evidence = evidence;
    this.clock = clock;
    this.videoEvidence = videoEvidence;
    this.organization = organization;
    this.measurementCollector = measurementCollector;
  }

  /** Lista ciclos do próprio produto, preservando escolhas históricas e estados terminais. */
  @Transactional(readOnly = true)
  public List<LearningCycleResponse> list(Long productId) {
    return list(productId, null);
  }

  /** Seleciona o histórico da cadeia no banco sem misturar jornadas independentes do produto. */
  @Transactional(readOnly = true)
  public List<LearningCycleResponse> list(Long productId, Long chainId) {
    requireProduct(productId, false);
    var selected =
        chainId == null
            ? cycles.findByProductIdOrderByIdDesc(productId)
            : cycles.findByProductIdAndChainCodeOrderByIdDesc(
                productId, requiredChain(chainId).getChainCode());
    return selected.stream().map(this::response).toList();
  }

  /** Expõe o grafo instalado e os destinos reais da cadeia, com experimentos do próprio produto. */
  @Transactional(readOnly = true)
  public LearningCycleCatalog catalog(Long chainId, Long productId) {
    var chain = requiredChain(chainId);
    var process = requiredCycleProcess();
    List<LearningCycleCatalog.ExperimentOption> options = List.of();
    if (productId != null) {
      requireProduct(productId, false);
      Set<Long> used = new HashSet<>();
      cycles
          .findByProductIdOrderByIdDesc(productId)
          .forEach(cycle -> used.add(cycle.getExperimentId()));
      options =
          experiments.findByProductIdOrderByUpdatedAtDescIdDesc(productId).stream()
              .map(
                  experiment -> {
                    var historicalPublication = evidence.historicalPublication(experiment);
                    boolean baseline = historicalPublication.isPresent();
                    boolean allowed =
                        !used.contains(experiment.getId())
                            && experiment.getExperimentType() != ExperimentType.FAKE_EXPERIMENT
                            && (baseline
                                ? experiment.getStatus() != ExperimentStatus.RUNNING
                                : experiment.getStatus() == ExperimentStatus.PLANNED);
                    return new LearningCycleCatalog.ExperimentOption(
                        experiment.getId(),
                        experiment.getName(),
                        String.valueOf(experiment.getStatus()),
                        allowed,
                        baseline,
                        allowed
                            ? (baseline
                                ? historicalPublication.orElseThrow().summary()
                                : "Novo ciclo: iniciar pelo aprendizado e hipótese.")
                            : "Já pertence a um ciclo, está em operação ou não possui publicação produtiva comprovada.");
                  })
              .toList();
    }
    var successorChain =
        chains
            .findFirstByChainCodeAndStatusOrderByVersionNumberDesc(
                chain.getChainCode(), "PUBLISHED")
            .orElse(null);
    return new LearningCycleCatalog(
        process.getId(),
        process.getVersionNumber(),
        json.read(process.getDiagramJson()),
        targets(chain),
        options,
        organization.describe(chain, process, productId, openCycle(productId, chain)),
        successorChain == null ? null : successorChain.getId(),
        successorChain == null
            ? null
            : successorChain.getName() + " · v" + successorChain.getVersionNumber(),
        createExperimentUrl(productId));
  }

  /** Abre o cadastro existente com o produto e seu nicho, sem recriar a descoberta. */
  private String createExperimentUrl(Long productId) {
    if (productId == null) return null;
    var product = products.findById(productId).orElseThrow();
    return "/experiments/new?productId="
        + productId
        + (product.getMarketNiche() == null ? "" : "&nicheId=" + product.getMarketNiche().getId());
  }

  /** Liga o BPM ao ambiente do ciclo, preservando cadeia, produto e ocorrência já aberta. */
  @Transactional(readOnly = true)
  public LearningCycleEntry entry(Long processId, Long productId, Long chainId) {
    if (productId != null) requireProduct(productId, false);
    var selected =
        processes.findById(processId).orElseThrow(() -> notFound("Processo não encontrado."));
    var cycleProcess = requiredCycleProcess();
    boolean isCycle = PROCESS_CODE.equals(selected.getProcessCode());
    if (!isCycle && !Objects.equals(cycleProcess.getParentProcessCode(), selected.getProcessCode()))
      return null;
    List<BusinessProcessChainDefinition> candidates;
    if (chainId != null) candidates = List.of(requiredChain(chainId));
    else {
      Long parentId =
          isCycle
              ? processes
                  .findFirstByProcessCodeAndStatusOrderByVersionNumberDesc(
                      cycleProcess.getParentProcessCode(), "PUBLISHED")
                  .map(BusinessProcessDefinition::getId)
                  .orElse(null)
              : processId;
      candidates = parentId == null ? List.of() : chains.findByProcessDefinitionId(parentId);
    }
    return candidates.stream()
        .filter(
            chain ->
                chain.getItems().stream()
                    .anyMatch(
                        item ->
                            isCycle
                                ? Objects.equals(
                                    item.getProcessDefinition().getProcessCode(),
                                    selected.getParentProcessCode())
                                : item.getProcessDefinition().getId().equals(processId)))
        .sorted(
            Comparator.comparing(
                    (BusinessProcessChainDefinition chain) ->
                        !"PUBLISHED".equals(chain.getStatus()))
                .thenComparing(
                    BusinessProcessChainDefinition::getVersionNumber, Comparator.reverseOrder()))
        .map(
            chain ->
                organization.describe(chain, cycleProcess, productId, openCycle(productId, chain)))
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }

  /** Consulta uma ocorrência aberta sem iniciar trabalho nem inferir pelo último experimento. */
  private LearningSalesCycle openCycle(Long productId, BusinessProcessChainDefinition chain) {
    return productId == null
        ? null
        : cycles
            .findFirstByProductIdAndChainCodeAndOpenSlot(productId, chain.getChainCode(), 1)
            .orElse(null);
  }

  /** Abre uma iteração atômica, sem criar campanha nem alterar a seleção comercial do produto. */
  @Transactional
  public LearningCycleResponse create(Long productId, CreateLearningCycleRequest request) {
    requireProduct(productId, true);
    String input = json.write(request);
    var replay = cycles.findByProductIdAndRequestKey(productId, request.requestKey().toString());
    if (replay.isPresent()) {
      require(
          input.equals(replay.get().getCreationJson()),
          "Esta chave já foi usada com outro conteúdo.");
      return response(replay.get());
    }
    var chain = requiredChain(request.chainDefinitionId());
    require(
        "PUBLISHED".equals(chain.getStatus()), "Inicie o ciclo com a versão publicada da cadeia.");
    var placement = organization.describe(chain, requiredCycleProcess(), productId, null);
    require(
        placement != null && placement.canStartCycle(),
        "A cadeia precisa chamar o ciclo no BPM de venda e aprendizado antes de iniciar uma iteração.");
    require(
        !cycles.existsByProductIdAndChainCodeAndOpenSlot(productId, chain.getChainCode(), 1),
        "Conclua o ciclo aberto desta cadeia antes de iniciar o sucessor.");
    var experiment = requiredExperiment(productId, request.experimentId());
    require(
        experiment.getExperimentType() != ExperimentType.FAKE_EXPERIMENT,
        "Experimentos de teste não podem compor ciclos comerciais.");
    require(
        !cycles.existsByExperimentId(experiment.getId()),
        "Este experimento já possui um ciclo. Use outro experimento para a nova hipótese.");
    require(
        request.windowEnd().isAfter(request.windowStart()),
        "A janela precisa terminar depois do início.");
    Instant now = Instant.now(clock).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    var historicalPublication = evidence.historicalPublication(experiment);
    if (request.baseline()) {
      require(
          request.previousCycleId() == null
              && experiment.getStatus() != ExperimentStatus.RUNNING
              && historicalPublication.isPresent(),
          "A referência histórica deve ter sido publicada e estar fora de operação.");
    } else {
      require(
          experiment.getStatus() == ExperimentStatus.PLANNED && historicalPublication.isEmpty(),
          "Um novo ciclo exige experimento planejado sem exposição anterior.");
      require(
          request.windowEnd().isAfter(now), "Defina uma janela futura para o novo experimento.");
    }
    LearningSalesCycle previous = null;
    if (request.previousCycleId() != null) {
      previous = requiredCycle(productId, request.previousCycleId());
      require(
          "ADJUSTED".equals(previous.getStatus()),
          "O predecessor precisa terminar com decisão de ajuste e aprendizado.");
      require(
          previous.getChainCode().equals(chain.getChainCode()),
          "O sucessor deve continuar a mesma cadeia de valor.");
      require(
          cycles.findByPreviousCycleId(previous.getId()).isEmpty(),
          "O ciclo anterior já possui sucessor.");
      require(
          experiment.getStatus() != ExperimentStatus.RUNNING,
          "Não adote um experimento já em operação como sucessor.");
    }
    var cycle = new LearningSalesCycle();
    cycle.setProductId(productId);
    cycle.setChainDefinitionId(chain.getId());
    cycle.setChainCode(chain.getChainCode());
    cycle.setProcessDefinitionId(requiredCycleProcess().getId());
    cycle.setExperimentId(experiment.getId());
    cycle.setPreviousCycleId(request.previousCycleId());
    cycle.setRequestKey(request.requestKey().toString());
    cycle.setCreationJson(input);
    cycle.setBriefJson(input);
    cycle.setBaseline(request.baseline());
    cycle.setProductVersion(request.productVersion().trim());
    cycle.setBudgetLimitBrl(request.budgetLimitBrl());
    cycle.setWindowStart(request.windowStart());
    cycle.setWindowEnd(request.windowEnd());
    cycle.setStage(request.baseline() ? "MEASUREMENT" : "LEARNING");
    cycle.setStatus("OPEN");
    cycle.setOpenSlot(1);
    cycle.setRevision(0);
    cycle.setCreatedAt(now);
    cycle.setUpdatedAt(now);
    cycle.setVersionChangedAt(Instant.EPOCH);
    cycle.setInheritedLearningJson(
        previous == null
            ? "{}"
            : json.write(
                Map.of(
                    "cycleId",
                    previous.getId(),
                    "experimentId",
                    previous.getExperimentId(),
                    "productVersion",
                    previous.getProductVersion(),
                    "brief",
                    json.read(previous.getBriefJson()),
                    "events",
                    eventResponses(previous),
                    "priorCycleReference",
                    previous.getPreviousCycleId() == null ? "" : previous.getPreviousCycleId())));
    if (previous != null) {
      String returnCode =
          processes.findById(previous.getReturnProcessId()).orElseThrow().getProcessCode();
      String activityId = previous.getReturnActivityId();
      var target =
          targets(chain).stream()
              .filter(
                  value ->
                      returnCode.equals(value.processCode())
                          && activityId.equals(value.activityId()))
              .findFirst()
              .orElseThrow(
                  () ->
                      new ResponseStatusException(
                          HttpStatus.CONFLICT,
                          "O destino de correção não existe nesta versão da cadeia. Revise o contrato do processo antes de continuar."));
      cycle.setReturnProcessId(target.processDefinitionId());
      cycle.setReturnActivityId(target.activityId());
    }
    cycles.saveAndFlush(cycle);
    if (request.baseline()) {
      recordHistoricalAdoption(cycle, request, historicalPublication.orElseThrow(), now);
    }
    ledger.open(cycle, now);
    reconcileAutomatically(cycle, experiment, now, "STAGE_ENTERED");
    log.info(
        "Ciclos: ciclo aberto productId={} cycleId={} experimentId={} predecessor={} baseline={}",
        productId,
        cycle.getId(),
        cycle.getExperimentId(),
        cycle.getPreviousCycleId(),
        cycle.isBaseline());
    return response(cycle);
  }

  /** Registra a origem e as lacunas da referência sem fabricar execução ou aprovação retroativa. */
  private void recordHistoricalAdoption(
      LearningSalesCycle cycle,
      CreateLearningCycleRequest request,
      LearningCycleHistoricalPublication publication,
      Instant now) {
    var event = new LearningSalesCycleEvent();
    event.setCycleId(cycle.getId());
    event.setRequestKey(request.requestKey().toString());
    event.setRequestJson(cycle.getCreationJson());
    event.setRevision(0);
    event.setFromStage("MEASUREMENT");
    event.setToStage("MEASUREMENT");
    event.setAction("ADOPT_BASELINE");
    event.setOperatorName(request.operatorName().trim());
    event.setSummary(publication.summary());
    event.setEvidenceReference(publication.reference());
    event.setEvidenceJson(json.write(publication));
    event.setCreatedAt(now);
    events.saveAndFlush(event);
  }

  /** Aplica sob lock e lê commits recentes para reconhecer aprovações concorrentes idênticas. */
  @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
  public LearningCycleResponse command(Long productId, Long cycleId, LearningCycleCommand request) {
    requireProduct(productId, true);
    var cycle =
        cycles
            .findLocked(productId, cycleId)
            .orElseThrow(() -> notFound("Ciclo não encontrado neste produto."));
    var replay = events.findByCycleIdAndRequestKey(cycleId, request.requestKey().toString());
    String input = json.write(request);
    if (replay.isPresent()) {
      require(
          json.read(input).equals(json.read(replay.get().getRequestJson())),
          "A chave deste comando já foi usada com conteúdo diferente.");
      return response(cycle);
    }
    require(
        cycle.getRevision() == request.expectedRevision(),
        "O ciclo mudou. Atualize a tela antes de decidir novamente.");
    require(
        "OPEN".equals(cycle.getStatus()),
        "Este ciclo já foi encerrado e seu histórico é imutável.");
    require(
        actions(cycle.getStage()).contains(request.action()),
        "Comando incompatível com a etapa atual.");
    require(
        request.evidence().isObject() && input.length() <= 64000,
        "Envie evidência estruturada de até 64 KB.");
    String from = cycle.getStage();
    Instant now = Instant.now(clock).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    var experiment = requiredExperiment(productId, cycle.getExperimentId());
    var approvedProposal = decisionApproval.validate(cycle, request);
    apply(cycle, experiment, request, now);
    cycle.setRevision(cycle.getRevision() + 1);
    cycle.setUpdatedAt(now);
    var event = new LearningSalesCycleEvent();
    event.setCycleId(cycleId);
    event.setRequestKey(request.requestKey().toString());
    event.setRequestJson(input);
    event.setRevision(cycle.getRevision());
    event.setFromStage(from);
    event.setToStage(cycle.getStage());
    event.setAction(request.action().name());
    event.setOperatorName(request.operatorName().trim());
    event.setSummary(request.summary().trim());
    event.setEvidenceReference(request.evidenceReference().trim());
    event.setEvidenceJson(json.write(request.evidence()));
    event.setCreatedAt(now);
    events.saveAndFlush(event);
    decisionApproval.record(approvedProposal, event.getId(), input, now);
    String completion =
        request.action() == Action.REWORK || request.action() == Action.FIX_MEASUREMENT
            ? "BLOCKED"
            : request.action() == Action.STOP || request.action() == Action.INCONCLUSIVE
                ? "CANCELLED"
                : "COMPLETED";
    ledger.finish(cycle, input, completion, request.summary(), now);
    if ("OPEN".equals(cycle.getStatus())) ledger.open(cycle, now);
    cycles.saveAndFlush(cycle);
    reconcileAutomatically(cycle, experiment, now, "STAGE_ENTERED");
    log.info(
        "Ciclos: decisão persistida productId={} cycleId={} experimentId={} action={} from={} to={} revision={}",
        productId,
        cycleId,
        cycle.getExperimentId(),
        request.action(),
        from,
        cycle.getStage(),
        cycle.getRevision());
    return response(cycle);
  }

  /**
   * Reconcilia novamente as fontes oficiais de uma medição bloqueada sem receber números do
   * navegador.
   */
  @Transactional
  public LearningCycleResponse reconcileMeasurement(
      Long productId, Long cycleId, ReconcileLearningCycleMeasurementRequest request) {
    requireProduct(productId, true);
    var cycle =
        cycles
            .findLocked(productId, cycleId)
            .orElseThrow(() -> notFound("Ciclo não encontrado neste produto."));
    String input = json.write(request);
    var replay = events.findByCycleIdAndRequestKey(cycleId, request.requestKey().toString());
    if (replay.isPresent()) {
      require(
          json.read(input).equals(json.read(replay.get().getRequestJson())),
          "A chave desta conciliação já foi usada com outro conteúdo.");
      return response(cycle);
    }
    require(
        cycle.getRevision() == request.expectedRevision(),
        "O ciclo mudou. Atualize a tela antes de reconciliar novamente.");
    require(
        "OPEN".equals(cycle.getStatus()) && "MEASUREMENT".equals(cycle.getStage()),
        "A conciliação automática só pode ser refeita durante a etapa de medição.");
    Instant now = Instant.now(clock).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    var experiment = requiredExperiment(productId, cycle.getExperimentId());
    persistAutomaticMeasurement(
        cycle,
        experiment,
        measurementCollector.collect(cycle, experiment, now),
        request.requestKey().toString(),
        input,
        now);
    return response(cycle);
  }

  /** Tenta concluir toda entrada em medição e preserva o bloqueio se a fonte não estiver pronta. */
  private void reconcileAutomatically(
      LearningSalesCycle cycle, Experiment experiment, Instant now, String trigger) {
    if (!"OPEN".equals(cycle.getStatus()) || !"MEASUREMENT".equals(cycle.getStage())) return;
    String requestKey = UUID.randomUUID().toString();
    String input =
        json.write(
            Map.of(
                "contractVersion",
                LearningCycleMeasurementCollector.CONTRACT,
                "requestKey",
                requestKey,
                "trigger",
                trigger));
    persistAutomaticMeasurement(
        cycle,
        experiment,
        measurementCollector.collect(cycle, experiment, now),
        requestKey,
        input,
        now);
  }

  /** Persiste uma fotografia válida ou um bloqueio, mantendo revisão e ledger BPM coerentes. */
  private void persistAutomaticMeasurement(
      LearningSalesCycle cycle,
      Experiment experiment,
      LearningCycleMeasurementCollector.Result collected,
      String requestKey,
      String input,
      Instant now) {
    var result = preventUnchangedSnapshot(cycle, experiment, collected, now);
    String from = cycle.getStage();
    if (result.ready()) {
      validateMetrics(cycle, result.evidence(), now);
      cycle.setStage("DECISION");
    }
    cycle.setRevision(cycle.getRevision() + 1);
    cycle.setUpdatedAt(now);
    var event = new LearningSalesCycleEvent();
    event.setCycleId(cycle.getId());
    event.setRequestKey(requestKey);
    event.setRequestJson(input);
    event.setRevision(cycle.getRevision());
    event.setFromStage(from);
    event.setToStage(cycle.getStage());
    event.setAction(result.ready() ? "MEASURE" : "MEASUREMENT_BLOCKED");
    event.setOperatorName("Marketing Hub · backend");
    event.setSummary(result.summary());
    event.setEvidenceReference(result.evidenceReference());
    event.setEvidenceJson(json.write(result.evidence()));
    event.setCreatedAt(now);
    events.saveAndFlush(event);
    ledger.finishAutomaticMeasurement(
        cycle,
        json.write(result.evidence()),
        result.ready() ? "COMPLETED" : "BLOCKED",
        result.summary(),
        now);
    ledger.open(cycle, now);
    cycles.saveAndFlush(cycle);
    log.info(
        "Ciclos: conciliação automática persistida productId={} cycleId={} experimentId={} status={} revision={}",
        cycle.getProductId(),
        cycle.getId(),
        experiment.getId(),
        result.ready() ? "READY" : "BLOCKED",
        cycle.getRevision());
  }

  /** Impede que continuar coleta produza outra decisão sem qualquer mudança nas fontes. */
  private LearningCycleMeasurementCollector.Result preventUnchangedSnapshot(
      LearningSalesCycle cycle,
      Experiment experiment,
      LearningCycleMeasurementCollector.Result result,
      Instant now) {
    JsonNode previous = latestMetrics(cycle);
    String fingerprint = result.evidence().path("sourceFingerprint").asText();
    if (!result.ready()
        || fingerprint.isBlank()
        || !fingerprint.equals(previous.path("sourceFingerprint").asText())) return result;
    JsonNode blocked =
        json.read(
            json.write(
                Map.of(
                    "contractVersion",
                    LearningCycleMeasurementCollector.CONTRACT,
                    "automatic",
                    true,
                    "dataValid",
                    false,
                    "experimentId",
                    experiment.getId(),
                    "observedAt",
                    now.toString(),
                    "sourceFingerprint",
                    fingerprint,
                    "blocker",
                    "As fontes ainda não mudaram desde a última leitura. Aguarde novos dados antes de decidir novamente.")));
    return new LearningCycleMeasurementCollector.Result(
        false,
        blocked,
        "Conciliação automática bloqueada: as fontes ainda não mudaram desde a última leitura.",
        "internal://learning-cycles/"
            + cycle.getId()
            + "/experiments/"
            + experiment.getId()
            + "/measurement-unchanged");
  }

  /** Valida o movimento e resolve a próxima etapa sem permitir bypass por campos da tela. */
  private void apply(
      LearningSalesCycle cycle, Experiment experiment, LearningCycleCommand request, Instant now) {
    JsonNode data = request.evidence();
    if (requiresCurrentApproval(request.action(), cycle.getStage())) {
      String blocker = approvalBlocker(cycle);
      require(blocker == null, blocker);
    }
    switch (request.action()) {
      case COMPLETE -> {
        switch (cycle.getStage()) {
          case "LEARNING" -> {
            text(data, "learning");
            text(data, "competingExplanation");
          }
          case "PLANNING" -> {
            text(data, "planReference");
            text(data, "stopRule");
          }
          case "ADJUSTMENT" -> {
            require(
                cycle.getProductVersion().equals(text(data, "productVersion")),
                "O ajuste precisa corresponder à versão declarada. Use Devolver para correção para trocar a versão.");
            text(data, "changeEvidence");
          }
          case "VIDEO_BRIEF" -> {
            require(videoWorkflow(cycle), "Este BPM não possui etapas de vídeo.");
            videoEvidence.brief(data);
          }
          case "CAMPAIGN_VIDEO" ->
              videoEvidence.production(
                  cycle, data, com.marketinghub.experiment.video.ExperimentVideoSlot.AD);
          case "PDE_ENTRY_VIDEO" ->
              videoEvidence.production(
                  cycle, data, com.marketinghub.experiment.video.ExperimentVideoSlot.LANDING_HERO);
          case "VIDEO_APPROVAL" -> videoEvidence.integration(cycle, data);
          case "VALIDATION" -> {
            if (videoWorkflow(cycle)) videoEvidence.current(cycle, false);
            evidence.validation(cycle, data);
          }
          case "AUTHORIZATION" -> evidence.authorization(cycle, experiment, data, now);
          case "PUBLICATION" -> evidence.publication(cycle, experiment, authorizationTime(cycle));
          default ->
              throw new ResponseStatusException(
                  HttpStatus.CONFLICT, "Etapa sem conclusão simples.");
        }
        cycle.setStage(next(cycle.getStage(), videoWorkflow(cycle)));
      }
      case REWORK -> {
        require(
            !evidence.operated(experiment)
                || (data.path("technicalOnly").asBoolean(false)
                    && experiment.getStatus() != ExperimentStatus.RUNNING),
            "Depois da publicação, pause a operação e confirme correção exclusivamente técnica; mudança comercial exige sucessor.");
        String version = text(data, "productVersion");
        require(
            version.length() <= 160 && !cycle.getProductVersion().equals(version),
            "Declare uma versão nova para invalidar as aprovações anteriores.");
        selectTarget(cycle, data);
        text(data, "rootCause");
        cycle.setProductVersion(version);
        cycle.setVersionChangedAt(now);
        cycle.setStage("ADJUSTMENT");
      }
      case MEASURE -> {
        validateMetrics(cycle, data, now);
        cycle.setStage("DECISION");
      }
      case ADJUST -> {
        JsonNode metrics = latestMetrics(cycle);
        requireDeliveryResolved(metrics);
        require(
            metrics.path("dataValid").asBoolean(false)
                && metrics.path("testDataExcluded").asBoolean(false),
            "Corrija os dados antes de concluir um ajuste comercial.");
        require(
            experiment.getStatus() != ExperimentStatus.RUNNING,
            "Pause ou encerre o experimento pelo fluxo oficial antes de preparar o sucessor.");
        selectTarget(cycle, data);
        text(data, "rootCause");
        text(data, "learning");
        text(data, "nextHypothesis");
        close(cycle, "ADJUSTED", now);
      }
      case CONTINUE -> {
        requireDeliveryResolved(latestMetrics(cycle));
        String blocker = collectionBlocker(cycle, latestMetrics(cycle), now);
        require(blocker == null, blocker);
        require(
            experiment.getStatus() == ExperimentStatus.RUNNING,
            "A continuidade não reativa uma campanha pausada. Use a operação oficial autorizada.");
        cycle.setStage("MEASUREMENT");
      }
      case FIX_MEASUREMENT -> {
        text(data, "rootCause");
        text(data, "correctionPlan");
        cycle.setStage("MEASUREMENT");
      }
      case SCALE -> {
        requireDeliveryResolved(latestMetrics(cycle));
        String blocker =
            scaleBlocker(cycle, latestMetrics(cycle), json.read(cycle.getBriefJson()), now);
        require(blocker == null, blocker);
        text(data, "scaleHypothesis");
        cycle.setStage("SCALE_AUTHORIZATION");
      }
      case AUTHORIZE_SCALE -> {
        String blocker =
            scaleBlocker(cycle, latestMetrics(cycle), json.read(cycle.getBriefJson()), now);
        require(blocker == null, blocker);
        require(
            data.path("confirmed").asBoolean(false),
            "Confirme explicitamente a autorização de expansão.");
        require(
            data.path("budgetLimitBrl").isNumber()
                && data.path("budgetLimitBrl").decimalValue().precision() <= 12
                && data.path("budgetLimitBrl").decimalValue().scale() <= 2
                && data.path("budgetLimitBrl").decimalValue().compareTo(cycle.getBudgetLimitBrl())
                    > 0,
            "A expansão deve declarar um novo teto total superior ao anterior.");
        Instant end = parseInstant(data, "windowEnd");
        require(
            end.isAfter(now) && !end.isBefore(cycle.getWindowEnd()),
            "A janela ampliada precisa cobrir a janela anterior e ter saldo de tempo.");
        cycle.setBudgetLimitBrl(data.path("budgetLimitBrl").decimalValue());
        cycle.setWindowEnd(end);
        evidence.authorization(cycle, experiment, data, now);
        cycle.setStage("MEASUREMENT");
      }
      case STOP, INCONCLUSIVE -> {
        requireDeliveryResolved(latestMetrics(cycle));
        require(
            experiment.getStatus() != ExperimentStatus.RUNNING,
            "Encerre ou pause o experimento pelo fluxo oficial antes de fechar o ciclo.");
        close(cycle, request.action() == Action.STOP ? "CLOSED" : "INCONCLUSIVE", now);
      }
    }
  }

  /** Impede avanço comercial com venda sem entrega comprovada na fotografia conciliada. */
  private void requireDeliveryResolved(JsonNode metrics) {
    String blocker = deliveryBlocker(metrics);
    require(blocker == null, blocker);
  }

  /** Explica o mesmo bloqueio de entrega na disponibilidade e na execução da decisão. */
  private String deliveryBlocker(JsonNode metrics) {
    return metrics.path("netSales").asLong() == 0
            || metrics.path("deliveryVerified").asBoolean(false)
        ? null
        : "Há vendas sem entrega comprovada. Conclua a atividade 6.2 e reconcilie a medição antes de avançar.";
  }

  /** Fecha a iteração, liberando o slot sem reescrever o experimento anterior. */
  private void close(LearningSalesCycle cycle, String status, Instant now) {
    cycle.setStatus(status);
    cycle.setClosedAt(now);
    cycle.setOpenSlot(null);
  }

  /** Vincula o retorno a uma atividade real da mesma cadeia e registra a causa em separado. */
  private void selectTarget(LearningSalesCycle cycle, JsonNode data) {
    require(
        data.path("returnProcessId").canConvertToLong(),
        "Selecione o processo que corrigirá a causa.");
    Long processId = data.path("returnProcessId").asLong();
    String activityId = text(data, "returnActivityId");
    requireTarget(cycle, processId, activityId);
    cycle.setReturnProcessId(processId);
    cycle.setReturnActivityId(activityId);
  }

  /** Rejeita retornos para atividade inexistente, outro produto ou processo fora da composição. */
  private void requireTarget(LearningSalesCycle cycle, Long processId, String activityId) {
    require(
        targets(requiredChain(cycle.getChainDefinitionId())).stream()
            .anyMatch(
                target ->
                    Objects.equals(target.processDefinitionId(), processId)
                        && Objects.equals(target.activityId(), activityId)),
        "O retorno deve apontar uma atividade real da cadeia selecionada.");
  }

  /**
   * Lista os processos e seus subprocessos publicados, preservando a versão exata do macroprocesso.
   */
  private List<LearningCycleCatalog.Target> targets(BusinessProcessChainDefinition chain) {
    Map<Long, BusinessProcessDefinition> selected = new LinkedHashMap<>();
    chain.getItems().stream()
        .sorted(
            Comparator.comparing(
                com.marketinghub.businessprocesschain.BusinessProcessChainItem::getSequenceNumber))
        .forEach(
            item -> {
              var process = item.getProcessDefinition();
              selected.put(process.getId(), process);
              processes
                  .findAllByParentProcessCodeAndStatusOrderByNameAscVersionNumberDesc(
                      process.getProcessCode(), "PUBLISHED")
                  .stream()
                  .filter(sub -> !PROCESS_CODE.equals(sub.getProcessCode()))
                  .forEach(sub -> selected.put(sub.getId(), sub));
            });
    return selected.values().stream()
        .flatMap(
            process ->
                activities.findAllByProcessDefinitionIdOrderByIdAsc(process.getId()).stream()
                    .map(
                        activity ->
                            new LearningCycleCatalog.Target(
                                process.getId(),
                                process.getName(),
                                activity.getActivityId(),
                                activity.getName(),
                                activity.getOwnerName(),
                                process.getProcessCode())))
        .toList();
  }

  /** Recupera a fotografia mais recente, sem somar leituras cumulativas. */
  private JsonNode latestMetrics(LearningSalesCycle cycle) {
    return events.findByCycleIdOrderByRevisionAsc(cycle.getId()).stream()
        .filter(event -> "MEASURE".equals(event.getAction()))
        .reduce((first, second) -> second)
        .map(event -> json.read(event.getEvidenceJson()))
        .orElseGet(() -> json.read("{}"));
  }

  /** Localiza a última autorização da versão para impedir publicação anterior à aprovação. */
  private Instant authorizationTime(LearningSalesCycle cycle) {
    return events.findByCycleIdOrderByRevisionAsc(cycle.getId()).stream()
        .filter(
            event ->
                "AUTHORIZATION".equals(event.getFromStage())
                    && "COMPLETE".equals(event.getAction())
                    && !event.getCreatedAt().isBefore(cycle.getVersionChangedAt()))
        .map(LearningSalesCycleEvent::getCreatedAt)
        .max(Instant::compareTo)
        .orElse(null);
  }

  /** Exige que publicação e expansão mantenham a aprovação utilizada pelo próprio ciclo. */
  private boolean requiresCurrentApproval(Action action, String stage) {
    return action == Action.SCALE
        || action == Action.AUTHORIZE_SCALE
        || (action == Action.COMPLETE && Set.of("AUTHORIZATION", "PUBLICATION").contains(stage));
  }

  /** Uma reprovação posterior não pode ser ignorada por uma autorização que estava em aberto. */
  private String approvalBlocker(LearningSalesCycle cycle) {
    if (videoWorkflow(cycle)) {
      String blocker =
          videoEvidence.blocker(
              cycle,
              Set.of("PUBLICATION", "MEASUREMENT", "DECISION", "SCALE_AUTHORIZATION")
                  .contains(cycle.getStage()));
      if (blocker != null) return blocker;
    }
    var validation =
        events.findByCycleIdOrderByRevisionAsc(cycle.getId()).stream()
            .filter(
                event ->
                    "VALIDATION".equals(event.getFromStage())
                        && "COMPLETE".equals(event.getAction()))
            .reduce((first, second) -> second);
    if (validation.isEmpty())
      return "Este ciclo não possui homologação registrada. Uma referência histórica deve orientar um novo ciclo homologado.";
    long approvedId =
        json.read(validation.get().getEvidenceJson()).path("approvalInstanceId").asLong(-1);
    return evidence.approvals(cycle).stream().anyMatch(option -> option.id() == approvedId)
        ? null
        : "A homologação utilizada deixou de ser vigente. Retorne para correção e homologue novamente antes de publicar ou expandir.";
  }

  /** Monta uma leitura exclusivamente a partir do estado e dos eventos persistidos. */
  private LearningCycleResponse response(LearningSalesCycle cycle) {
    var successor = cycles.findByPreviousCycleId(cycle.getId());
    JsonNode metrics = latestMetrics(cycle), brief = json.read(cycle.getBriefJson());
    var process = processes.findById(cycle.getProcessDefinitionId()).orElseThrow();
    JsonNode node = json.read(process.getDiagramJson()).path("nodes");
    JsonNode stageNode = null;
    for (JsonNode item : node)
      if (cycle.getStage().equals(item.path("id").asText())) stageNode = item;
    var approvalOptions =
        "VALIDATION".equals(cycle.getStage())
            ? evidence.approvals(cycle)
            : List.<LearningCycleResponse.ApprovalOption>of();
    var commands =
        "OPEN".equals(cycle.getStatus())
            ? actions(cycle.getStage()).stream()
                .map(
                    action -> {
                      String blocker =
                          action == Action.COMPLETE
                                  && "VALIDATION".equals(cycle.getStage())
                                  && approvalOptions.isEmpty()
                              ? "Conclua o gate multiagente da mesma versão na atividade orientada antes de avançar."
                              : action == Action.CONTINUE
                                  ? collectionBlocker(cycle, metrics, Instant.now(clock))
                                  : action == Action.SCALE
                                      ? scaleBlocker(cycle, metrics, brief, Instant.now(clock))
                                      : null;
                      if (blocker == null && requiresCurrentApproval(action, cycle.getStage()))
                        blocker = approvalBlocker(cycle);
                      if (blocker == null
                          && Set.of(
                                  Action.ADJUST,
                                  Action.CONTINUE,
                                  Action.SCALE,
                                  Action.STOP,
                                  Action.INCONCLUSIVE)
                              .contains(action)) blocker = deliveryBlocker(metrics);
                      return new LearningCycleResponse.CommandOption(
                          action.name(),
                          actionLabel(action, cycle.getStage()),
                          blocker == null,
                          blocker == null
                              ? "O backend conferirá os requisitos e preservará a decisão."
                              : blocker);
                    })
                .toList()
            : List.<LearningCycleResponse.CommandOption>of();
    String workUrl = workUrl(cycle);
    String nextAction =
        stageNode == null ? label(cycle.getStage()) : stageNode.path("description").asText();
    String responsible = stageNode == null ? "Operador do ciclo" : stageNode.path("owner").asText();
    if ("DECISION".equals(cycle.getStage())) {
      nextAction =
          "Atena prepara a proposta com as evidências conciliadas. Revise, edite e aprove para registrar a decisão e o retorno no BPM.";
      responsible = "Atena · proposta; usuário · edição e aprovação";
    }
    if ("MEASUREMENT".equals(cycle.getStage())) {
      nextAction =
          "O backend concilia automaticamente funil, vendas, receita, custos e valor entregue pelas fontes oficiais do experimento. Corrija somente a fonte indicada se houver bloqueio.";
      responsible = "Marketing Hub · conciliação automática; Hermes e Plutus · interpretação";
    }
    if ("ADJUSTMENT".equals(cycle.getStage()) && cycle.getReturnProcessId() != null) {
      var target =
          activities
              .findByProcessDefinitionIdAndActivityId(
                  cycle.getReturnProcessId(), cycle.getReturnActivityId())
              .orElseThrow();
      nextAction = "Abra a atividade orientada e execute «" + target.getName() + "». " + nextAction;
      responsible = target.getOwnerName();
    }
    if ("ADJUSTED".equals(cycle.getStatus())) {
      responsible = "Operador do ciclo · preparação do sucessor";
      if (successor.isPresent()) {
        var nextCycle = successor.orElseThrow();
        nextAction =
            "Ajuste aprovado. Continue no ciclo #"
                + nextCycle.getId()
                + " · experimento #"
                + nextCycle.getExperimentId()
                + ", que recebeu o aprendizado e o retorno registrado no BPM.";
        workUrl =
            "/business-process-chains/learning-cycles?chainId="
                + nextCycle.getChainDefinitionId()
                + "&productId="
                + nextCycle.getProductId()
                + "&cycleId="
                + nextCycle.getId();
      } else {
        nextAction =
            "Ajuste aprovado. Crie um experimento planejado deste produto e vincule-o em «Criar ciclo sucessor com aprendizado». O sucessor receberá a hipótese e o destino de retorno; não repita a decisão nem execute o ajuste no experimento histórico.";
        workUrl = null;
      }
    }
    return new LearningCycleResponse(
        cycle.getId(),
        cycle.getProductId(),
        cycle.getExperimentId(),
        cycle.getPreviousCycleId(),
        successor.map(LearningSalesCycle::getId).orElse(null),
        cycle.getChainDefinitionId(),
        cycle.getProcessDefinitionId(),
        cycle.getRevision(),
        cycle.getStage(),
        label(cycle.getStage()),
        cycle.getStatus(),
        cycle.isBaseline(),
        cycle.getProductVersion(),
        cycle.getBudgetLimitBrl(),
        cycle.getWindowStart(),
        cycle.getWindowEnd(),
        nextAction,
        responsible,
        cycle.getReturnProcessId(),
        cycle.getReturnActivityId(),
        workUrl,
        json.read(process.getDiagramJson()),
        brief,
        json.read(cycle.getInheritedLearningJson()),
        eventResponses(cycle),
        approvalOptions,
        videoWorkflow(cycle) ? videoOptions(cycle) : Map.of(),
        workLinks(cycle),
        commands,
        "ADJUSTED".equals(cycle.getStatus()) && successor.isEmpty(),
        cycle.getCreatedAt(),
        cycle.getClosedAt());
  }

  /** Direciona cada etapa ao BPM responsável sem substituir a seleção do experimento no plano. */
  private String workUrl(LearningSalesCycle cycle) {
    if (VIDEO_STAGES.contains(cycle.getStage()))
      return "VIDEO_APPROVAL".equals(cycle.getStage())
          ? "/products/" + cycle.getProductId() + "/pde-versions"
          : "/audio-video-studio";
    String code =
        switch (cycle.getStage()) {
          case "LEARNING" -> "pde-sales-delivery-learning";
          case "PLANNING" -> "pde-commercial-plan-offer";
          case "ADJUSTMENT", "VALIDATION" -> "pde-construction-approval";
          case "AUTHORIZATION" -> "pde-commercial-homologation-activation";
          default -> null;
        };
    Long processId = "ADJUSTMENT".equals(cycle.getStage()) ? cycle.getReturnProcessId() : null;
    if (processId == null && code != null)
      processId =
          requiredChain(cycle.getChainDefinitionId()).getItems().stream()
              .filter(item -> code.equals(item.getProcessDefinition().getProcessCode()))
              .map(item -> item.getProcessDefinition().getId())
              .findFirst()
              .orElse(null);
    return processId == null
        ? "/experiments/" + cycle.getExperimentId()
        : "/products/"
            + cycle.getProductId()
            + "/value-chain-history/processes/"
            + processId
            + "/activities?learningCycleId="
            + cycle.getId()
            + "&chainId="
            + cycle.getChainDefinitionId()
            + ("ADJUSTMENT".equals(cycle.getStage()) && cycle.getReturnActivityId() != null
                ? "#activity-" + cycle.getReturnActivityId()
                : "");
  }

  /** Projeta a auditoria sem expor JSON dentro de texto JSON. */
  private List<LearningCycleResponse.Event> eventResponses(LearningSalesCycle cycle) {
    return events.findByCycleIdOrderByRevisionAsc(cycle.getId()).stream()
        .map(
            event ->
                new LearningCycleResponse.Event(
                    event.getId(),
                    event.getRevision(),
                    event.getFromStage(),
                    event.getToStage(),
                    event.getAction(),
                    event.getOperatorName(),
                    event.getSummary(),
                    event.getEvidenceReference(),
                    json.read(event.getEvidenceJson()),
                    event.getCreatedAt()))
        .toList();
  }

  /** Confere a identidade do produto e serializa mutações da mesma cadeia quando necessário. */
  private void requireProduct(Long id, boolean lock) {
    var product = lock ? products.findLockedById(id) : products.findById(id);
    if (product.isEmpty()) throw notFound("Produto não encontrado.");
  }

  /** Resolve somente um experimento do produto solicitado. */
  private Experiment requiredExperiment(Long productId, Long id) {
    var value = experiments.findById(id).orElseThrow(() -> notFound("Experimento não encontrado."));
    require(
        value.getProduct() != null && productId.equals(value.getProduct().getId()),
        "O experimento pertence a outro produto.");
    return value;
  }

  /** Resolve um ciclo segregado, inclusive para links históricos. */
  private LearningSalesCycle requiredCycle(Long productId, Long id) {
    return cycles
        .findById(id)
        .filter(cycle -> productId.equals(cycle.getProductId()))
        .orElseThrow(() -> notFound("Ciclo não encontrado neste produto."));
  }

  /** Localiza a versão imutável da cadeia. */
  private BusinessProcessChainDefinition requiredChain(Long id) {
    return chains.findById(id).orElseThrow(() -> notFound("Cadeia de valor não encontrada."));
  }

  /** Usa o BPM publicado mais recente para novas ocorrências, preservando a definição histórica. */
  private BusinessProcessDefinition requiredCycleProcess() {
    return processes
        .findFirstByProcessCodeAndStatusOrderByVersionNumberDesc(PROCESS_CODE, "PUBLISHED")
        .orElseThrow(() -> notFound("O BPM de ciclos ainda não possui versão publicada."));
  }

  /** Lê a versão exata do BPM persistido no ciclo, sem migrar ocorrências em andamento. */
  private boolean videoWorkflow(LearningSalesCycle cycle) {
    return processes.findById(cycle.getProcessDefinitionId()).orElseThrow().getVersionNumber() >= 2;
  }

  /** Mantém a tela legível se uma mídia desaparecer, permitindo devolver para correção. */
  private Map<String, List<LearningCycleResponse.ApprovalOption>> videoOptions(
      LearningSalesCycle cycle) {
    try {
      return videoEvidence.options(cycle);
    } catch (ResponseStatusException ex) {
      log.debug(
          "Ciclos: opções de vídeo indisponíveis cycleId={} experimentId={}",
          cycle.getId(),
          cycle.getExperimentId(),
          ex);
      return Map.of();
    }
  }

  /**
   * Expõe caminhos oficiais para produção, revisão e integração sem comandos externos implícitos.
   */
  private List<LearningCycleResponse.WorkLink> workLinks(LearningSalesCycle cycle) {
    if (!videoWorkflow(cycle) || !VIDEO_STAGES.contains(cycle.getStage())) return List.of();
    return List.of(
        new LearningCycleResponse.WorkLink("Produzir no Estúdio", "/audio-video-studio"),
        new LearningCycleResponse.WorkLink(
            "Vídeos e criativos do experimento #" + cycle.getExperimentId(),
            "/experiments/" + cycle.getExperimentId()),
        new LearningCycleResponse.WorkLink(
            "Integrar na versão PDE", "/products/" + cycle.getProductId() + "/pde-versions"),
        new LearningCycleResponse.WorkLink(
            "Conferir vídeos do produto", "/products/" + cycle.getProductId() + "/pde-videos"));
  }

  /** Padroniza uma ausência sem informar dados de outra entidade. */
  private ResponseStatusException notFound(String message) {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
  }
}

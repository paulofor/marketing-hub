package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.agenttask.*;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.singleVideoBudget.SingleVideoBudgetResponse;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.financialagent.StudioCostLedgerEntryRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.openai.OpenAiModelRepository;
import com.marketinghub.repository.jpa.salesvideo.VideoProductionCycleRepository;
import com.marketinghub.repository.jpa.salesvideo.VideoProjectRepository;
import com.marketinghub.salesvideo.VideoProject;
import com.marketinghub.videocontract.v1.BoundedSpendContracts.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Responsabilidade: governar a admissão de consumo limitado no recibo integral de um vídeo. */
@Component
@lombok.extern.slf4j.Slf4j
@RequiredArgsConstructor
public class SingleVideoSpending
    implements AgentTaskSpendingPolicy,
        com.marketinghub.salesvideo.service.clipplan.VideoProductionSpendingPolicy {
  public static final String PROTOCOL = "SINGLE_VIDEO_BOUNDED_V1";
  public static final String HEADER = "X-Single-Video-Spending-Contract";
  private Clock clock = Clock.systemUTC();

  /** Permite validar vencimentos com relógio fixo sem alterar a política em runtime. */
  void setClock(Clock clock) {
    this.clock = Objects.requireNonNull(clock);
  }

  private final AgentTaskRepository tasks;
  private final AgentTaskTargetContextProvider targets;
  private final VideoProjectRepository projects;
  private final VideoProductionCycleRepository videoCycles;
  private final LearningSalesCycleRepository cycles;
  private final LearningCycleSingleVideoBudget budgets;
  private final OpenAiModelRepository models;
  private final StudioCostLedgerEntryRepository costs;
  private final SingleVideoSpendLedger ledger;
  private final LearningCycleJson json;
  private final com.marketinghub.repository.jpa.salesvideo.SalesVideoJobRepository videoJobs;
  private final com.marketinghub.repository.jpa.salesvideo.SalesVideoProviderModelRepository
      videoModels;
  private final com.marketinghub.repository.jpa.agentaudioreview.AgentAudioReviewRepository
      audioReviews;
  private final com.marketinghub.repository.jpa.product.ProductRepository products;
  private final com.marketinghub.repository.jpa.agent.AgentRepository agents;
  private final com.marketinghub.repository.jpa.salesvideo.VideoCreditReservationRepository
      creditReservations;
  private final com.marketinghub.repository.jpa.salesvideo.SalesVideoScriptRepository scripts;
  private final com.marketinghub.repository.jpa.experiment.video.ExperimentVideoAssetRepository
      finalVideos;

  /** Reconhece o recibo persistido mesmo quando sua vigência já terminou. */
  @Override
  public boolean protectedExecution(AgentTask task) {
    return protectedContext(task) != null;
  }

  /** Recusa o transporte antigo antes da lease, sem tratar o header como liberação de gasto. */
  @Override
  public String claimBlocker(AgentTask task) {
    var protectedContext = protectedContext(task);
    if (protectedContext == null) return null;
    if (!protectedContext.receipt().current())
      return "O recibo deste vídeo não está vigente; não iniciar inferência.";
    String scopeBlocker = taskScopeBlocker(task, protectedContext);
    if (scopeBlocker != null) return scopeBlocker;
    if (!boundedTransport())
      return "Este vídeo exige transporte com reserva compartilhada e saída limitada antes da"
          + " chamada; o executor antigo não pode consumir este teto.";
    if ("IN_PROGRESS".equals(task.getStatus()) && !PROTOCOL.equals(task.getSpendingContract()))
      return "A lease anterior não comprovou transporte limitado; conciliar a execução antes de"
          + " liberar outra chamada.";
    return null;
  }

  /** Marca somente uma entrega protegida cuja implementação declarou o protocolo exigido. */
  @Override
  public void prepareClaim(AgentTask task) {
    if (protectedExecution(task) && boundedTransport()) task.setSpendingContract(PROTOCOL);
  }

  /**
   * Publica a autoridade persistida antes de o worker escolher entre transporte limitado e legado.
   */
  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Context context(String agent, Long taskId) {
    var task = owned(agent, taskId);
    var context = protectedContext(task);
    return context == null
        ? new Context(false, null, null, null, null, null)
        : new Context(
            true,
            context.receipt().eventId(),
            context.project().getId(),
            context.receipt().current()
                ? taskScopeBlocker(task, context)
                : "Recibo ou contexto não vigente.",
            ledger.find(context.receipt().eventId(), "task:" + taskId + ":main"),
            null);
  }

  /** Reserva uma única chamada Responses, sem tools, continuação remota ou troca de modelo. */
  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Lease reserve(String agent, Long taskId, Reserve input) {
    var task = owned(agent, taskId);
    var context = requireContext(task);
    lock(context);
    require(boundedTransport(), "Transporte de consumo não homologado para este recibo.");
    require(
        "IN_PROGRESS".equals(task.getStatus())
            || ("GATE_DECISION".equals(task.getTaskKind())
                && "PENDING".equals(task.getGateStatus())
                && "PENDING".equals(task.getStatus())
                && "financial-agent".equals(agent)
                && task.getSourceReference().startsWith("video-production-cycle:")),
        "Tarefa não elegível para inferência.");
    require(
        Boolean.TRUE.equals(task.getAssignedAgent().getAutomaticExecutionEnabled()),
        "Agente está em STOP.");
    if ("GATE_DECISION".equals(task.getTaskKind()) && "PENDING".equals(task.getStatus())) {
      require(
          task.getExecutionPrompt() == null || PROTOCOL.equals(task.getSpendingContract()),
          "Gate já possui tentativa sem limite comprovado; conciliar ou reaplicar o resultado"
              + " original.");
      task.setSpendingContract(PROTOCOL);
      tasks.saveAndFlush(task);
    }
    require(
        PROTOCOL.equals(task.getSpendingContract()), "Lease sem transporte limitado persistido.");
    return reserveResponse(context, "task:" + taskId + ":main", input, taskId);
  }

  /** Calcula um máximo Responses com o corpo imutável e sem depender do desconto Flex. */
  private Lease reserveResponse(
      Protected context, String operation, Reserve input, Long currentTaskId) {
    var request = Objects.requireNonNull(input.request(), "Request ausente.");
    var allowedFields =
        Set.of("model", "service_tier", "store", "max_output_tokens", "reasoning", "input", "text");
    request
        .fieldNames()
        .forEachRemaining(
            field ->
                require(
                    allowedFields.contains(field), "Campo fora do contrato limitado: " + field));
    immutableInput(request.path("input"));
    require(
        request.isObject() && "flex".equals(request.path("service_tier").asText()),
        "O transporte limitado exige Flex explícito.");
    require(
        !request.has("tools")
            && !request.has("previous_response_id")
            && !request.has("conversation")
            && !request.path("background").asBoolean()
            && !request.path("stream").asBoolean(),
        "Tools, continuação, background e streaming não pertencem ao contrato limitado.");
    int output = request.path("max_output_tokens").asInt();
    require(output >= 1 && output <= 32768, "Limite efetivo de saída ausente ou excessivo.");
    var count = Objects.requireNonNull(input.inputTokenCount(), "Contagem de entrada ausente.");
    long tokens = count.path("input_tokens").asLong(-1);
    require(
        "response.input_tokens".equals(count.path("object").asText())
            && tokens >= 0
            && tokens <= 272000,
        "Contagem de entrada não comprovada ou contexto longo não homologado.");
    String modelCode = request.path("model").asText();
    var model =
        models
            .findByCode(modelCode)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Modelo não cadastrado; substituição automática recusada."));
    require(
        model.getLastPricingSyncAt() != null
            && !model.getLastPricingSyncAt().isBefore(Instant.now(clock).minus(30, ChronoUnit.DAYS))
            && !model.getLastPricingSyncAt().isAfter(Instant.now(clock).plusSeconds(60))
            && model.getPricingSource() != null
            && model.getPricingSource().startsWith("https://developers.openai.com/"),
        "Revalidar tarifa oficial do modelo antes de consumir.");
    require(
        model.getPriceInputStandard() != null
            && model.getPriceInputStandard().signum() > 0
            && model.getPriceOutputStandard() != null
            && model.getPriceOutputStandard().signum() > 0,
        "Tarifa máxima do modelo ausente.");
    // Reserva Standard com folga de cache/long-context; nunca depende do desconto Flex para caber.
    BigDecimal maximum =
        model
            .getPriceInputStandard()
            .multiply(new BigDecimal("2.5"))
            .multiply(BigDecimal.valueOf(tokens + 1024L))
            .add(
                model
                    .getPriceOutputStandard()
                    .multiply(new BigDecimal("1.5"))
                    .multiply(BigDecimal.valueOf(output)))
            .movePointLeft(6)
            .setScale(8, RoundingMode.CEILING);
    String evidence =
        json.write(
            Map.of(
                "source",
                model.getPricingSource(),
                "observedAt",
                model.getLastPricingSyncAt().toString(),
                "inputTokens",
                tokens,
                "maxOutputTokens",
                output,
                "inputStandard",
                model.getPriceInputStandard(),
                "outputStandard",
                model.getPriceOutputStandard(),
                "inputUpperMultiplier",
                2.5,
                "outputUpperMultiplier",
                1.5,
                "providerCount",
                count));
    return ledger.reserve(
        context.receipt().eventId(),
        context.cycle().getId(),
        context.project().getId(),
        operation,
        "OPENAI_RESPONSES",
        modelCode,
        request.toString(),
        maximum,
        context.receipt().totalLimitUsd(),
        outsideLiability(context, currentTaskId),
        evidence);
  }

  /** Adquire o direito de um único envio; conexão ou resposta incerta não autoriza repetir. */
  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Lease begin(String agent, Long taskId, Long reservationId) {
    var task = owned(agent, taskId);
    var context = requireContext(task);
    lock(context);
    require(
        Boolean.TRUE.equals(task.getAssignedAgent().getAutomaticExecutionEnabled()),
        "Agente está em STOP.");
    require(boundedTransport(), "Transporte limitado não informado.");
    require(
        "IN_PROGRESS".equals(task.getStatus())
            || ("GATE_DECISION".equals(task.getTaskKind())
                && "PENDING".equals(task.getGateStatus())
                && "PENDING".equals(task.getStatus())
                && "financial-agent".equals(agent)),
        "Tarefa não está elegível para novo envio.");
    require(
        ledger
                .liability(context.receipt().eventId())
                .add(outsideLiability(context, taskId))
                .compareTo(context.receipt().totalLimitUsd())
            <= 0,
        "Cobertura integral mudou antes do envio; conciliar sem consumir.");
    return ledger.begin(
        reservationId,
        context.receipt().eventId(),
        context.project().getId(),
        "task:" + taskId + ":main");
  }

  /** Preserva a resposta bruta no backend para replay sem liberar estimativa como saldo real. */
  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Lease record(String agent, Long taskId, Long reservationId, Result input) {
    var context = Objects.requireNonNull(protectedContext(owned(agent, taskId)));
    cycles.findLockedById(context.cycle().getId()).orElseThrow();
    return ledger.record(
        reservationId,
        context.receipt().eventId(),
        context.project().getId(),
        "task:" + taskId + ":main",
        input);
  }

  /** Mantém custos novos fora do protocolo contabilizados; ausência de valor é bloqueio. */
  private BigDecimal outsideLiability(Protected context, Long currentTaskId) {
    BigDecimal sum = BigDecimal.ZERO;
    for (var row :
        costs.findByExperimentIdOrderByCreatedAtAsc(context.project().getExperimentId())) {
      require(row.getCreatedAt() != null, "Ledger sem data verificável; não presumir custo zero.");
      if (row.getCreatedAt().isBefore(context.receipt().authorizedAt())) continue;
      if ("SALES_VIDEO_JOB".equals(row.getSourceType())
          && ledger.covers(context.receipt().eventId(), "job:" + row.getSourceId() + ":")) continue;
      if ("SALES_VIDEO_JOB".equals(row.getSourceType())) {
        var job = videoJobs.findById(Long.parseLong(row.getSourceId())).orElseThrow();
        if (PROTOCOL.equals(job.getSpendingContract())) continue;
      }
      if ("AGENT_AUDIO_REVIEW".equals(row.getSourceType())) {
        var review = audioReviews.findById(Long.parseLong(row.getSourceId())).orElseThrow();
        if (ledger.find(
                context.receipt().eventId(),
                "task:" + review.getTaskId() + ":audio:" + review.getAssetId())
            != null) continue;
      }
      var value =
          row.getProviderCostUsd() == null ? row.getEstimatedCostUsd() : row.getProviderCostUsd();
      require(value != null, "Há custo novo pendente no ledger; preservar reserva e conciliar.");
      sum = sum.add(value);
    }
    Map<Long, AgentTask> related = new LinkedHashMap<>();
    for (var task :
        tasks.findModelAttemptsForSpending(
            "experiment:" + context.project().getExperimentId(), context.receipt().authorizedAt()))
      related.put(task.getId(), task);
    for (var production :
        videoCycles.findByVideoProjectIdOrderByCreatedAtDesc(context.project().getId()))
      for (var task :
          tasks.findModelAttemptsForSpending(
              "video-production-cycle:" + production.getId(), context.receipt().authorizedAt()))
        related.put(task.getId(), task);
    for (var task : related.values()) {
      if (Objects.equals(task.getId(), currentTaskId)
          || ledger.find(context.receipt().eventId(), "task:" + task.getId() + ":main") != null)
        continue;
      if (PROTOCOL.equals(task.getSpendingContract())) continue;
      require(
          task.getEstimatedCostUsd() != null,
          "Há custo de agente ainda desconhecido no contexto; não iniciar outra inferência.");
      sum = sum.add(task.getEstimatedCostUsd());
    }
    return sum;
  }

  /** Requer somente provedores de clipes com contrato finito; nenhum candidato é ativado. */
  @Override
  public String paidExecutionBlocker(VideoProject project) {
    var receipt = budgets.protectedReceipt(project);
    if (receipt == null) return null;
    if (!receipt.current()) return "Recibo integral não vigente; consumo bloqueado.";
    if (com.marketinghub.videocontract.v1.VideoCapabilities.selected(project.getProviderPlan())
            .isEmpty()
        && (project.getProviderPlan() == null
            || !project
                .getProviderPlan()
                .toUpperCase(java.util.Locale.ROOT)
                .contains("(RUNWAY_ROUTER)")))
      return "O recibo integral exige uma rota de clipes com custo máximo comprovado; outros"
          + " provedores não podem consumir por fallback.";
    return null;
  }

  /** Mantém filas antigas sem entrega de inferência protegida durante publicação gradual. */
  @Override
  public boolean workerCanConsume(VideoProject project) {
    return budgets.protectedReceipt(project) == null || boundedTransport();
  }

  /** Recusa desvio do Estúdio para um provedor sem limite integrado no recibo. */
  @Override
  public String requestBlocker(VideoProject project, String providerName) {
    if (budgets.protectedReceipt(project) == null) return null;
    if (providerName == null || !providerName.startsWith("RUNWAY"))
      return "O recibo integral não permite render por outro provedor; use o ciclo governado com"
          + " custo limitado.";
    return paidExecutionBlocker(project);
  }

  /** Libera apenas entrada privada aceita e financeiramente autorizada no mesmo recibo/contexto. */
  @Override
  public boolean privateScriptAllowed(Long profileId, Long scriptId) {
    var project =
        projects.findFirstBySalesVideoProfileIdOrderByUpdatedAtDesc(profileId).orElse(null);
    var receipt = budgets.protectedReceipt(project);
    if (receipt == null || !receipt.current()) return false;
    var script = scripts.findById(scriptId).orElseThrow();
    if (script.getStatus() != com.marketinghub.salesvideo.SalesVideoScriptStatus.PRIVATE_READY
        || script.getSource()
            != com.marketinghub.salesvideo.SalesVideoScriptSource.IRIS_PRIVATE_BRIEF
        || script.getApprovedBy() != null
        || script.getApprovedAt() != null
        || !Objects.equals(script.getProfile().getId(), profileId)) return false;
    var proof = json.read(script.getSourceEvidenceJson());
    var cycle = videoCycles.findById(proof.path("videoProductionCycleId").asLong()).orElseThrow();
    return "IRIS_PRIVATE_VIDEO_INPUT_V1".equals(proof.path("contractVersion").asText())
        && Objects.equals(cycle.getVideoProjectId(), project.getId())
        && Objects.equals(cycle.getIrisAgentTaskId(), proof.path("irisTaskId").asLong())
        && "APPROVED".equals(cycle.getFinancialDecision())
        && Objects.equals(proof.path("acceptedScriptText").asText(), script.getScriptText())
        && Objects.equals(script.getScriptText(), project.getScriptText())
        && Objects.equals(
            proof.path("acceptedHookText").asText(), Objects.toString(project.getHookText(), ""))
        && Objects.equals(
            proof.path("acceptedCtaText").asText(), Objects.toString(project.getCtaText(), ""))
        && Objects.equals(
            proof.path("acceptedCaptionPlan").asText(),
            Objects.toString(project.getCaptionPlan(), ""))
        && Objects.equals(proof.path("productVersion").asText(), project.getCampaignKey())
        && tasks
            .findById(cycle.getIrisAgentTaskId())
            .filter(
                t ->
                    "COMPLETED".equals(t.getStatus())
                        && t.getAssignedAgent() != null
                        && "communication-director".equals(t.getAssignedAgent().getAgentKey())
                        && Objects.equals(
                            t.getSourceReference(), "experiment:" + project.getExperimentId()))
            .isPresent();
  }

  /** Mantém reservas e pendências visíveis no Estúdio sem expor o conteúdo das inferências. */
  @Override
  @Transactional(readOnly = true)
  public Map<String, Object> report(VideoProject project) {
    var receipt = budgets.protectedReceipt(project);
    return receipt == null ? Map.of() : ledger.report(receipt.eventId(), receipt.totalLimitUsd());
  }

  /**
   * Permite gravar a experiência herdada explicitamente, preservando a versão alvo e os pareceres
   * históricos.
   */
  @Override
  @Transactional(isolation = Isolation.READ_COMMITTED)
  public void validateProductRecording(VideoProject project, JsonNode manifest) {
    var receipt =
        Objects.requireNonNull(budgets.protectedReceipt(project), "Gravação sem recibo integral.");
    require(receipt.current(), "Recibo integral não vigente.");
    var commercial = cycles.findByExperimentId(project.getExperimentId()).orElseThrow();
    lock(new Protected(project, commercial, receipt));
    require(
        videoCycles.findByVideoProjectIdOrderByCreatedAtDesc(project.getId()).stream()
            .noneMatch(
                v ->
                    v.getIrisAgentTaskId() != null
                        || v.getAgentTaskId() != null
                        || v.getSalesVideoJobId() != null),
        "Entrada do pedido já congelada; preservar a gravação e as tarefas existentes.");
    String source = manifest.path("sourcePrototypeVersion").asText();
    require(!source.isBlank(), "Versão do software gravado ausente.");
    boolean inherited = false;
    for (var event : json.read(commercial.getInheritedLearningJson()).path("events")) {
      var evidence = event.path("evidence");
      if (evidence.isTextual()) evidence = json.read(evidence.asText());
      var prototype = evidence.path("privatePrototype");
      if (source.equals(prototype.path("prototypeVersion").asText())
          && prototype.path("desktopValidated").asBoolean()
          && prototype.path("mobileValidated").asBoolean()
          && prototype.path("testDataExcluded").asBoolean()
          && prototype.path("noExternalSideEffects").asBoolean()) inherited = true;
    }
    require(
        inherited,
        "Software de origem não consta da experiência privada herdada; não trocar ou ativar"
            + " produto.");
  }

  /** Retém o máximo do preflight sob o mesmo lock de agentes, clipes, voz e revisões. */
  @Override
  @Transactional(isolation = Isolation.READ_COMMITTED)
  public String holdProviderCredits(
      VideoProject project,
      com.marketinghub.salesvideo.VideoProductionCycle videoCycle,
      BigDecimal maximumUsd) {
    var receipt = budgets.protectedReceipt(project);
    if (receipt == null) return null;
    try {
      require(receipt.current(), "Recibo integral não vigente.");
      require(
          Objects.equals(videoCycle.getVideoProjectId(), project.getId())
              && Objects.equals(videoCycle.getProductId(), project.getProductId())
              && Objects.equals(videoCycle.getExperimentId(), project.getExperimentId()),
          "Reserva de outro ciclo ou projeto.");
      var context =
          new Protected(
              project, cycles.findByExperimentId(project.getExperimentId()).orElseThrow(), receipt);
      lock(context);
      String operation = "cycle:" + videoCycle.getId() + ":PROVIDER_CREDITS";
      if (ledger.find(receipt.eventId(), operation) != null) return null;
      ledger.reserve(
          receipt.eventId(),
          context.cycle().getId(),
          project.getId(),
          operation,
          "RUNWAY_CREDIT_HOLD",
          "PREFLIGHT_MAXIMUM",
          json.write(
              Map.of("videoProductionCycleId", videoCycle.getId(), "maximumUsd", maximumUsd)),
          maximumUsd,
          receipt.totalLimitUsd(),
          outsideLiability(context, null),
          json.write(Map.of("source", "BACKEND_VALIDATED_PREFLIGHT", "providerCalls", 0)));
      return null;
    } catch (IllegalArgumentException ex) {
      log.warn(
          "Reserva integral de clipes recusada antes de consumo; projectId={} cycleId={}"
              + " receiptId={}",
          project.getId(),
          videoCycle.getId(),
          receipt.eventId(),
          ex);
      return ex.getMessage();
    }
  }

  /** Publica reserva e modelo de voz explícito sem permitir que metadata libere consumo. */
  @Override
  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Context jobContext(Long jobId, String purpose) {
    var context = protectedJob(jobId);
    if (context == null) return new Context(false, null, null, null, null, null);
    return new Context(
        true,
        context.receipt().eventId(),
        context.project().getId(),
        !context.receipt().current()
            ? "Recibo integral não vigente."
            : videoJobs
                    .findById(jobId)
                    .filter(
                        j ->
                            j.getStatus()
                                    == com.marketinghub.salesvideo.SalesVideoStatus.VIDEO_PROCESSING
                                && !PROTOCOL.equals(j.getSpendingContract()))
                    .isPresent()
                ? "Job anterior sem lease limitada; conciliar resultado antes de retomar."
                : !boundedTransport() ? "Worker sem contrato de gasto limitado." : null,
        ledger.find(context.receipt().eventId(), jobOperation(jobId, purpose)),
        "tts-1-hd");
  }

  /** Reserva planner, tomada ou voz pelo request real e tarifa finita validada no servidor. */
  @Override
  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Lease reserveJob(Long jobId, String purpose, Reserve input) {
    var context = Objects.requireNonNull(protectedJob(jobId), "Job sem recibo integral.");
    lock(context);
    require(boundedTransport(), "Worker sem contrato de gasto limitado.");
    var job = videoJobs.findById(jobId).orElseThrow();
    require(
        PROTOCOL.equals(job.getSpendingContract())
            && job.getStatus() == com.marketinghub.salesvideo.SalesVideoStatus.VIDEO_PROCESSING,
        "Job não possui lease limitada em execução.");
    if (job.getJobType() == com.marketinghub.salesvideo.SalesVideoJobType.RENDER)
      ledger.bindRenderJob(
          context.receipt().eventId(), context.cycle().getId(), context.project().getId(), jobId);
    String operation = jobOperation(jobId, purpose);
    if ("PLAN".equals(purpose)) return reserveResponse(context, operation, input, null);
    var request = Objects.requireNonNull(input.request());
    String model = request.path("model").asText();
    BigDecimal maximum;
    String provider;
    String evidence;
    if (purpose.matches("VOICE_[1-9][0-9]?")) {
      require(
          "nova".equals(request.path("voice").asText()), "Voz divergente da rota HD explícita.");
      require(
          "tts-1-hd".equals(model),
          "Voz precisa da rota explícita cobrada por caracteres; troca silenciosa recusada.");
      require(
          Instant.now(clock).isBefore(Instant.parse("2026-12-05T00:00:00Z")),
          "Revalidar a tarifa da voz antes de consumir.");
      var allowed = Set.of("model", "voice", "input", "response_format", "speed");
      request
          .fieldNames()
          .forEachRemaining(
              field -> require(allowed.contains(field), "Campo fora do contrato de voz limitada."));
      String text = request.path("input").asText();
      require(
          !text.isBlank() && text.codePointCount(0, text.length()) <= 4096,
          "Entrada de voz inválida.");
      maximum =
          BigDecimal.valueOf(text.getBytes(java.nio.charset.StandardCharsets.UTF_8).length)
              .multiply(new BigDecimal("0.00003000"));
      provider = "OPENAI_SPEECH";
      evidence =
          json.write(
              Map.of(
                  "source",
                  "https://developers.openai.com/api/docs/models/tts-1-hd",
                  "observedAt",
                  "2026-11-05",
                  "usdPerMillionCharacters",
                  30,
                  "upperBoundBasis",
                  "UTF8_BYTES_GE_CHARACTERS",
                  "shutdownAt",
                  "2027-01-06",
                  "modelSubstitution",
                  "EXPLICIT_BOUNDED_VOICE_ROUTE"));
    } else if (purpose.matches("CLIP_[1-9][0-9]?")) {
      require(
          job.getJobType() == com.marketinghub.salesvideo.SalesVideoJobType.RENDER,
          "Job não autoriza clipe.");
      var metadata = json.read(job.getMetadataJson());
      int scene = Integer.parseInt(purpose.substring("CLIP_".length()));
      var frozenText = metadata.path("runwayRouterRequestsJson").asText();
      JsonNode routed = null;
      if (!frozenText.isBlank()) {
        var frozen = json.read(frozenText);
        var routes = json.read(metadata.path("runwaySelectedRoutesJson").asText());
        require(
            scene >= 1
                && scene <= frozen.size()
                && routes.size() == frozen.size()
                && frozen.get(scene - 1).equals(request),
            "Tomada diverge do preflight congelado.");
        routed = routes.get(scene - 1);
        model = routed.path("model").asText();
      } else {
        var allowed =
            Set.of(
                "model",
                "promptText",
                "promptImage",
                "duration",
                "ratio",
                "seed",
                "audio",
                "contentModeration");
        request
            .fieldNames()
            .forEachRemaining(
                field -> require(allowed.contains(field), "Campo Runway sem custo limitado."));
        require(scene <= metadata.path("sceneCount").asInt(1), "Tomada fora do plano.");
        var selected = videoModels.findByProviderName(job.getProviderName()).orElseThrow();
        require(
            model.equals(selected.getExternalModelId()),
            "Substituição de modelo de clipe recusada.");
      }
      var productionCycle =
          videoCycles.findById(metadata.path("videoProductionCycleId").asLong()).orElseThrow();
      var credits =
          creditReservations.findByVideoProductionCycleId(productionCycle.getId()).orElseThrow();
      require(
          "APPROVED".equals(productionCycle.getFinancialDecision())
              && Set.of("RESERVED", "CONSUMING").contains(credits.getStatus())
              && credits.getExpiresAt().isAfter(Instant.now(clock))
              && credits.getId().equals(metadata.path("providerCreditReservationId").asLong()),
          "Parecer ou reserva de clipes não vigente.");
      var catalog = videoModels.findByAdapterKeyAndExternalModelId("RUNWAY", model).orElseThrow();
      require(
          "ACTIVE".equals(catalog.getLifecycleStatus())
              && catalog.isAdapterVerified()
              && catalog.isPricingVerified()
              && catalog.isQualityGateVerified()
              && catalog.isCommercialLicenseVerified()
              && catalog.getPricingObservedAt() != null
              && !catalog
                  .getPricingObservedAt()
                  .isBefore(Instant.now(clock).minus(30, ChronoUnit.DAYS)),
          "Modelo de clipe não ativo/homologado ou tarifa vencida.");
      var capability =
          com.marketinghub.videocontract.v1.VideoCapabilities.model(model).orElseThrow();
      var inputBody = routed == null ? request : request.path("input");
      int duration = inputBody.path("duration").asInt();
      require(
          duration > 0 && duration <= context.project().getTargetDurationSeconds(),
          "Duração de tomada inválida.");
      BigDecimal creditsAtTariff =
          capability.credits(
              duration,
              inputBody.path("resolution").asText("720p"),
              inputBody.path("audio").asBoolean(false),
              routed == null
                  ? (request.has("promptImage") ? 1 : 0)
                  : inputBody.path("referenceImages").size(),
              0,
              Instant.now(clock));
      BigDecimal ceiling =
          routed == null ? creditsAtTariff : routed.path("priceCeilingCredits").decimalValue();
      require(
          ceiling.signum() > 0 && ceiling.compareTo(creditsAtTariff) >= 0,
          "Teto por tomada inferior à tarifa vigente.");
      require(
          credits.getReservedCredits() != null
              && credits.getReservedCredits().signum() > 0
              && credits.getReservedCostUsd() != null
              && credits.getReservedCostUsd().signum() > 0,
          "Cotação monetária da reserva de créditos ausente.");
      maximum =
          ceiling
              .multiply(credits.getReservedCostUsd())
              .divide(credits.getReservedCredits(), 8, RoundingMode.CEILING);
      provider = "RUNWAY";
      evidence =
          json.write(
              Map.of(
                  "modelId",
                  catalog.getId(),
                  "source",
                  catalog.getDocumentationUrl(),
                  "observedAt",
                  catalog.getPricingObservedAt().toString(),
                  "durationSeconds",
                  duration,
                  "capabilityRevision",
                  com.marketinghub.videocontract.v1.VideoCapabilities.catalog().revision()));
    } else throw new IllegalArgumentException("Operação de mídia fora do contrato limitado.");
    if ("RUNWAY".equals(provider)) {
      var metadata = json.read(job.getMetadataJson());
      return ledger.reserveClip(
          context.receipt().eventId(),
          context.cycle().getId(),
          context.project().getId(),
          "cycle:" + metadata.path("videoProductionCycleId").asLong() + ":PROVIDER_CREDITS",
          operation,
          model,
          request.toString(),
          maximum,
          context.receipt().totalLimitUsd(),
          outsideLiability(context, null),
          evidence);
    }
    return ledger.reserve(
        context.receipt().eventId(),
        context.cycle().getId(),
        context.project().getId(),
        operation,
        provider,
        model,
        request.toString(),
        maximum,
        context.receipt().totalLimitUsd(),
        outsideLiability(context, null),
        evidence);
  }

  /** Permite somente o primeiro envio da operação do job, com o lock comum do recibo. */
  @Override
  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Lease beginJob(Long jobId, String purpose, Long reservationId) {
    var context = Objects.requireNonNull(protectedJob(jobId));
    lock(context);
    require(boundedTransport(), "Worker sem contrato de gasto limitado.");
    var apollo = agents.findByAgentKey("videomaker").orElseThrow();
    require(Boolean.TRUE.equals(apollo.getAutomaticExecutionEnabled()), "Apolo está em STOP.");
    var job = videoJobs.findById(jobId).orElseThrow();
    require(
        PROTOCOL.equals(job.getSpendingContract())
            && job.getStatus() == com.marketinghub.salesvideo.SalesVideoStatus.VIDEO_PROCESSING,
        "Job não está elegível para novo envio.");
    require(
        ledger
                .liability(context.receipt().eventId())
                .add(outsideLiability(context, null))
                .compareTo(context.receipt().totalLimitUsd())
            <= 0,
        "Cobertura integral mudou antes do envio; conciliar sem consumir.");
    return ledger.begin(
        reservationId,
        context.receipt().eventId(),
        context.project().getId(),
        jobOperation(jobId, purpose));
  }

  /** Registra resposta preservada inclusive depois de STOP, sem permitir novo envio. */
  @Override
  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Lease recordJob(Long jobId, String purpose, Long reservationId, Result input) {
    var context = Objects.requireNonNull(protectedJob(jobId));
    cycles.findLockedById(context.cycle().getId()).orElseThrow();
    return ledger.record(
        reservationId,
        context.receipt().eventId(),
        context.project().getId(),
        jobOperation(jobId, purpose),
        input);
  }

  /**
   * Reserva toda a janela de entrada auditiva e a saída textual limitada, sem hipótese por segundo.
   */
  @Transactional(isolation = Isolation.READ_COMMITTED)
  public BigDecimal reserveAudio(String agent, Long taskId, Long assetId, JsonNode request) {
    var task = owned(agent, taskId);
    var context = protectedContext(task);
    if (context == null) return null;
    require(taskScopeBlocker(task, context) == null, "Escuta fora da revisão deste vídeo.");
    require(reviewVideos(context).contains(assetId), "Áudio pertence a outra mídia ou montagem.");
    lock(context);
    require(
        PROTOCOL.equals(owned(agent, taskId).getSpendingContract()),
        "Escuta sem lease limitada persistida.");
    var allowed =
        Set.of("model", "service_tier", "store", "max_completion_tokens", "modalities", "messages");
    request
        .fieldNames()
        .forEachRemaining(
            field -> require(allowed.contains(field), "Campo fora do contrato auditivo limitado."));
    require(
        Boolean.TRUE.equals(owned(agent, taskId).getAssignedAgent().getAutomaticExecutionEnabled()),
        "Psique está em STOP.");
    require(
        "gpt-audio-1.5".equals(request.path("model").asText())
            && request.path("max_completion_tokens").asInt() == 2000
            && request.path("modalities").size() == 1
            && "text".equals(request.at("/modalities/0").asText())
            && !request.has("tools")
            && !request.path("stream").asBoolean(),
        "Escuta sem limite efetivo de saída textual.");
    require(
        Instant.now(clock).isBefore(Instant.parse("2026-12-05T00:00:00Z")),
        "Revalidar tarifa e janela da escuta.");
    BigDecimal maximum = new BigDecimal("4.11600000");
    String operation = "task:" + taskId + ":audio:" + assetId;
    var lease =
        ledger.reserve(
            context.receipt().eventId(),
            context.cycle().getId(),
            context.project().getId(),
            operation,
            "OPENAI_AUDIO_REVIEW",
            "gpt-audio-1.5",
            request.toString(),
            maximum,
            context.receipt().totalLimitUsd(),
            outsideLiability(context, taskId),
            json.write(
                Map.of(
                    "source",
                    "https://developers.openai.com/api/docs/models/gpt-audio-1.5",
                    "observedAt",
                    "2026-11-05",
                    "maxContextTokens",
                    128000,
                    "audioInputUsdPerMillion",
                    32,
                    "textOutputUsdPerMillion",
                    10,
                    "maxOutputTokens",
                    2000)));
    var admitted =
        ledger.begin(lease.id(), context.receipt().eventId(), context.project().getId(), operation);
    require(
        admitted.mayInvoke(), "Escuta anterior enviada ou incerta; preservar recibo, sem repetir.");
    return maximum;
  }

  /** Marca a resposta auditiva já preservada sem liberar seu máximo como custo faturado. */
  @Transactional(isolation = Isolation.READ_COMMITTED)
  public void recordAudio(String agent, Long taskId, Long assetId, String raw) {
    var context = protectedContext(owned(agent, taskId));
    if (context == null) return;
    cycles.findLockedById(context.cycle().getId()).orElseThrow();
    String operation = "task:" + taskId + ":audio:" + assetId;
    var lease = ledger.find(context.receipt().eventId(), operation);
    if (lease != null)
      ledger.record(
          lease.id(),
          context.receipt().eventId(),
          context.project().getId(),
          operation,
          new Result(null, raw, null));
  }

  /** Localiza o perfil e tenant reais do job, sem usar metadata como identidade financeira. */
  private Protected protectedJob(Long jobId) {
    var job = videoJobs.findById(jobId).orElseThrow();
    var metadata =
        json.read(
            job.getMetadataJson() == null || job.getMetadataJson().isBlank()
                ? "{}"
                : job.getMetadataJson());
    var project =
        metadata.path("videoProjectId").canConvertToLong()
            ? projects.findById(metadata.path("videoProjectId").asLong()).orElse(null)
            : projects
                .findFirstBySalesVideoProfileIdOrderByUpdatedAtDesc(job.getProfile().getId())
                .orElse(null);
    if (project == null) {
      require(
          !PROTOCOL.equals(job.getSpendingContract()),
          "Vínculo do job protegido foi removido; não usar transporte legado.");
      return null;
    }
    require(
        Objects.equals(project.getSalesVideoProfileId(), job.getProfile().getId()),
        "Perfil divergente do projeto do job.");
    var receipt = budgets.protectedReceipt(project);
    if (receipt == null) {
      require(
          !PROTOCOL.equals(job.getSpendingContract()),
          "Recibo do job protegido ausente; fallback recusado.");
      return null;
    }
    require(Objects.equals(project.getTenantId(), job.getTenantId()), "Job de outro tenant.");
    require(
        job.getProviderName() != null
            && (job.getProviderName().startsWith("RUNWAY")
                || job.getJobType()
                    == com.marketinghub.salesvideo.SalesVideoJobType.POST_PRODUCTION),
        "Provedor sem contrato de gasto limitado; fallback recusado.");
    return new Protected(
        project, cycles.findByExperimentId(project.getExperimentId()).orElseThrow(), receipt);
  }

  /** Define operação estável por job e finalidade; retries não recebem uma chave nova. */
  private String jobOperation(Long jobId, String purpose) {
    require(
        "PLAN".equals(purpose) || purpose.matches("(?:VOICE|CLIP)_[1-9][0-9]?"),
        "Finalidade de consumo inválida.");
    return "job:" + jobId + ":" + purpose;
  }

  /** Recusa fontes mutáveis e ferramentas cujo custo possa mudar depois da contagem da entrada. */
  private void immutableInput(JsonNode input) {
    require(input.isArray() && !input.isEmpty(), "Entrada limitada precisa ser materializada.");
    for (var message : input) {
      require(
          "user".equals(message.path("role").asText()) && message.path("content").isArray(),
          "Mensagem fora do contrato limitado.");
      for (var part : message.path("content")) {
        String type = part.path("type").asText();
        require(
            ("input_text".equals(type) && part.path("text").isTextual())
                || ("input_image".equals(type)
                    && part.path("image_url")
                        .asText()
                        .matches("data:image/(png|jpeg|webp);base64,[A-Za-z0-9+/=]+")),
            "Entrada precisa conter somente texto ou pixels imutáveis; URL, arquivo e tool externos"
                + " recusados.");
      }
    }
  }

  /** Obtém lock comercial comum a agentes, produção e revisões antes da soma ou transição. */
  private void lock(Protected context) {
    var current = cycles.findLockedById(context.cycle().getId()).orElseThrow();
    require("OPEN".equals(current.getStatus()), "Ciclo encerrado: somente replay sem consumo.");
    var product = products.findById(context.project().getProductId()).orElseThrow();
    require(Boolean.TRUE.equals(product.getAutomaticExecutionEnabled()), "Produto está em STOP.");
    var receipt = budgets.forProject(context.project());
    require(
        receipt != null && Objects.equals(receipt.eventId(), context.receipt().eventId()),
        "Autorização mudou antes da chamada.");
  }

  /** Resolve o vínculo de Plutus pelo ciclo e demais agentes pela identidade canônica da tarefa. */
  private Protected protectedContext(AgentTask task) {
    if (task == null || task.getSourceReference() == null) return null;
    var source = task.getSourceReference();
    List<VideoProject> candidates;
    if (source.matches("video-production-cycle:[1-9][0-9]*")) {
      long id = Long.parseLong(source.substring(source.indexOf(':') + 1));
      candidates =
          videoCycles.findById(id).flatMap(c -> projects.findById(c.getVideoProjectId())).stream()
              .toList();
    } else {
      var target =
          targets
              .resolve(
                  source,
                  task.getProcessDefinition() == null
                      ? null
                      : task.getProcessDefinition().getProcessCode())
              .orElse(null);
      if (target == null || target.experimentId() == null) {
        require(
            !PROTOCOL.equals(task.getSpendingContract()),
            "Tarefa protegida perdeu o contexto; fallback recusado.");
        return null;
      }
      candidates = projects.findByExperimentIdOrderByUpdatedAtDesc(target.experimentId());
    }
    Protected selected = null;
    for (var project : candidates) {
      var receipt = budgets.protectedReceipt(project);
      if (receipt == null) continue;
      require(selected == null, "Tarefa vinculada a mais de um vídeo protegido; contexto ambíguo.");
      selected =
          new Protected(
              project, cycles.findByExperimentId(project.getExperimentId()).orElseThrow(), receipt);
    }
    require(
        selected != null || !PROTOCOL.equals(task.getSpendingContract()),
        "Recibo da tarefa protegida ausente; fallback recusado.");
    return selected;
  }

  /** Exige recibo, projeto e versão vigentes, sem aceitar sua identidade do request do worker. */
  private Protected requireContext(AgentTask task) {
    var context = protectedContext(task);
    require(
        context != null && context.receipt().current(), "Recibo integral vigente não encontrado.");
    String blocker = taskScopeBlocker(task, context);
    require(blocker == null, blocker);
    return context;
  }

  /**
   * Restringe o recibo às tarefas vinculadas ao vídeo, sem financiar outras atividades do
   * experimento.
   */
  private String taskScopeBlocker(AgentTask task, Protected context) {
    String agent = task.getAssignedAgent() == null ? "" : task.getAssignedAgent().getAgentKey();
    if ("communication-director".equals(agent)
        && videoCycles.findByIrisAgentTaskId(task.getId()).stream()
            .anyMatch(c -> Objects.equals(c.getVideoProjectId(), context.project().getId())))
      return null;
    String source = task.getSourceReference();
    if ("financial-agent".equals(agent)
        && source != null
        && source.matches("video-production-cycle:[1-9][0-9]*")) {
      var cycle =
          videoCycles
              .findById(Long.parseLong(source.substring(source.indexOf(':') + 1)))
              .orElse(null);
      if (cycle != null
          && Objects.equals(cycle.getVideoProjectId(), context.project().getId())
          && Objects.equals(cycle.getAgentTaskId(), task.getId())) return null;
    }
    String activity = task.getProcessActivityId();
    boolean finalReview =
        task.getProcessDefinition() != null
            && "creative-production-approval".equals(task.getProcessDefinition().getProcessCode())
            && (("customer-agent".equals(agent) && "customer".equals(activity))
                || ("meta-ad-approver".equals(agent) && "commercial".equals(activity)));
    if (finalReview && !reviewVideos(context).isEmpty()) return null;
    return "Esta tarefa não está vinculada à preparação, ao parecer financeiro ou à mídia final"
        + " deste único vídeo; não consumir seu teto.";
  }

  /** Confere a mídia final do render fixado no recibo, incluindo sua montagem sem nova geração. */
  private Set<Long> reviewVideos(Protected context) {
    var binding = ledger.find(context.receipt().eventId(), "RENDER_BINDING");
    if (binding == null) return Set.of();
    long renderId = binding.request().path("jobId").asLong(-1);
    var accepted = new HashSet<Long>();
    for (var video :
        finalVideos.findReadyExperimentVideosForReview(
            com.marketinghub.experiment.video.ExperimentVideoStatus.READY,
            null,
            context.project().getProductId(),
            context.project().getExperimentId())) {
      var job = video.getSalesVideoJob();
      if (job == null
          || job.getProfile() == null
          || !PROTOCOL.equals(job.getSpendingContract())
          || !Objects.equals(context.project().getTenantId(), job.getTenantId())
          || !Objects.equals(context.project().getSalesVideoProfileId(), job.getProfile().getId()))
        continue;
      boolean sameRender =
          Objects.equals(job.getId(), renderId)
              || (job.getJobType() == com.marketinghub.salesvideo.SalesVideoJobType.POST_PRODUCTION
                  && job.getRetryOfJob() != null
                  && Objects.equals(job.getRetryOfJob().getId(), renderId));
      if (!sameRender) continue;
      var metadata = json.read(job.getMetadataJson());
      var result = json.read(video.getResponseJson());
      if (metadata.path("videoProjectId").asLong(-1) == context.project().getId()
          && context.project().getCampaignKey().equals(metadata.path("productVersion").asText())
          && result.at("/hls_delivery/sourceSha256").asText().matches("[a-f0-9]{64}"))
        accepted.add(video.getId());
    }
    return accepted;
  }

  /** Confirma o responsável real da tarefa sem dar autoridade de aprovação ao executor. */
  private AgentTask owned(String agent, Long taskId) {
    var task = tasks.findById(taskId).orElseThrow();
    require(
        task.getAssignedAgent() != null
            && Objects.equals(agent, task.getAssignedAgent().getAgentKey()),
        "Tarefa pertence a outro agente.");
    return task;
  }

  /** Lê handshake do transporte publicado; ele habilita apenas a solicitação de uma reserva. */
  private boolean boundedTransport() {
    return RequestContextHolder.getRequestAttributes()
            instanceof ServletRequestAttributes attributes
        && PROTOCOL.equals(attributes.getRequest().getHeader(HEADER));
  }

  /** Recusa contrato incompleto sem efeitos externos ou liberação de saldo. */
  private static void require(boolean valid, String message) {
    if (!valid) throw new IllegalArgumentException(message);
  }

  /** Agrupa somente identidades recuperadas do backend e seu recibo imutável. */
  private record Protected(
      VideoProject project,
      LearningSalesCycle cycle,
      SingleVideoBudgetResponse.Authorization receipt) {}
}

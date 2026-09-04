package com.marketinghub.salesvideo.service.providerpreflight;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.financialagent.service.ProviderRouteEfficiencyView;
import com.marketinghub.financialagent.service.StudioProviderTaskConsumptionQueryService;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoProviderModelRepository;
import com.marketinghub.repository.jpa.salesvideo.VideoCreditReservationRepository;
import com.marketinghub.repository.jpa.salesvideo.VideoProviderAccountRepository;
import com.marketinghub.repository.jpa.salesvideo.VideoProviderPreflightRepository;
import com.marketinghub.salesvideo.SalesVideoProviderModel;
import com.marketinghub.salesvideo.VideoCreditReservation;
import com.marketinghub.salesvideo.VideoProductionCycle;
import com.marketinghub.salesvideo.VideoProject;
import com.marketinghub.salesvideo.VideoProviderAccount;
import com.marketinghub.salesvideo.VideoProviderPreflight;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: governar snapshots, dry runs e reservas de contas agregadoras de vídeo. */
@Service
public class VideoProviderFinancialPreflightService {
  private static final Logger log =
      LoggerFactory.getLogger(VideoProviderFinancialPreflightService.class);
  private static final String RUNWAY_ACCOUNT_KEY = "RUNWAY_PRIMARY";
  private static final Duration SNAPSHOT_TTL = Duration.ofMinutes(5);
  private static final Duration EXPECTED_EXECUTOR_CLOCK_SKEW = Duration.ofSeconds(60);
  private static final Duration MAX_EXECUTOR_CLOCK_SKEW = Duration.ofMinutes(5);
  private static final Duration RESERVATION_TTL = Duration.ofMinutes(60);
  private static final Set<String> PROFILES = Set.of("DRAFT_INSTAGRAM", "FINAL_CAMPAIGN");
  private final VideoProviderAccountRepository accountRepository;
  private final VideoProviderPreflightRepository preflightRepository;
  private final VideoCreditReservationRepository reservationRepository;
  private final SalesVideoProviderModelRepository providerModelRepository;
  private final StudioProviderTaskConsumptionQueryService taskConsumptionQueryService;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  /** Configura as fontes canônicas da conta, do preflight e da reserva financeira. */
  @Autowired
  public VideoProviderFinancialPreflightService(
      VideoProviderAccountRepository accountRepository,
      VideoProviderPreflightRepository preflightRepository,
      VideoCreditReservationRepository reservationRepository,
      SalesVideoProviderModelRepository providerModelRepository,
      StudioProviderTaskConsumptionQueryService taskConsumptionQueryService,
      ObjectMapper objectMapper) {
    this(
        accountRepository,
        preflightRepository,
        reservationRepository,
        providerModelRepository,
        taskConsumptionQueryService,
        objectMapper,
        Clock.systemUTC());
  }

  /** Configura as fontes e o relógio controlável usado nos testes do contrato temporal. */
  VideoProviderFinancialPreflightService(
      VideoProviderAccountRepository accountRepository,
      VideoProviderPreflightRepository preflightRepository,
      VideoCreditReservationRepository reservationRepository,
      SalesVideoProviderModelRepository providerModelRepository,
      StudioProviderTaskConsumptionQueryService taskConsumptionQueryService,
      ObjectMapper objectMapper,
      Clock clock) {
    this.accountRepository = accountRepository;
    this.preflightRepository = preflightRepository;
    this.reservationRepository = reservationRepository;
    this.providerModelRepository = providerModelRepository;
    this.taskConsumptionQueryService = taskConsumptionQueryService;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  /** Abre o preflight idempotente do ciclo sem consultar ou consumir o agregador. */
  @Transactional
  public VideoProviderPreflight open(Long cycleId, String requestedProfile) {
    return preflightRepository
        .findByVideoProductionCycleId(cycleId)
        .orElseGet(
            () -> {
              VideoProviderAccount account = account(RUNWAY_ACCOUNT_KEY);
              Instant now = Instant.now(clock);
              VideoProviderPreflight preflight = new VideoProviderPreflight();
              preflight.setVideoProductionCycleId(cycleId);
              preflight.setProviderAccountId(account.getId());
              preflight.setStatus("PENDING");
              preflight.setProductionProfile(profile(requestedProfile));
              preflight.setSourceUrl(account.getSourceUrl());
              preflight.setCreatedAt(now);
              preflight.setUpdatedAt(now);
              return preflightRepository.save(preflight);
            });
  }

  /** Lista a fila canônica de preflight que o executor de vídeo pode consumir. */
  @Transactional(readOnly = true)
  public List<VideoProviderPreflight> pending() {
    return preflightRepository.findByStatusOrderByCreatedAtAsc("PENDING");
  }

  /** Monta o contrato do executor sem incorporar detalhes tecnológicos ao backend. */
  @Transactional(readOnly = true)
  public VideoProviderFinancialPreflightData.Pending pendingResponse(
      VideoProviderPreflight preflight, VideoProductionCycle cycle, VideoProject project) {
    VideoProviderAccount account =
        accountRepository
            .findById(preflight.getProviderAccountId())
            .orElseThrow(() -> conflict("Conta agregadora do preflight não encontrada."));
    int targetDuration = Math.max(2, project.getTargetDurationSeconds());
    int clipDuration = providerClipDuration(project.getProviderPlan());
    int clips = Math.max(1, (targetDuration + clipDuration - 1) / clipDuration);
    boolean productUgc = isProductUgc(project.getProviderPlan());
    return new VideoProviderFinancialPreflightData.Pending(
        preflight.getId(),
        cycle.getId(),
        account.getAggregatorName(),
        account.getAccountKey(),
        preflight.getProductionProfile(),
        project.getProviderPlan(),
        cycle.getBudgetLimitUsd().divide(account.getCreditUnitUsd(), 4, RoundingMode.DOWN),
        targetDuration,
        clipDuration,
        clips,
        productUgc
            ? ("DRAFT_INSTAGRAM".equals(preflight.getProductionProfile())
                ? "720:1280"
                : "1080:1920")
            : "9:16",
        productUgc && "FINAL_CAMPAIGN".equals(preflight.getProductionProfile()) ? "1080p" : "720p",
        false,
        project.getTitle(),
        project.getObjective(),
        project.getHookText(),
        project.getScriptText(),
        project.getScenePlan(),
        project.getCharacterBible(),
        project.getEnvironmentBible(),
        project.getObjectBible(),
        project.getVisualStyleGuide(),
        project.getContinuityRules(),
        project.getCaptionPlan(),
        project.getCtaText(),
        project.getCharacterPerformanceType(),
        project.getCharacterPerformanceUri(),
        project.getReferencePerformanceUri(),
        project.getPerformanceConsentEvidence(),
        project.getPerformanceRightsEvidence(),
        project.getEditingNotes(),
        project.getQualityGate(),
        cycle.getLearningObjective(),
        cycle.getSuccessCriterion());
  }

  /** Persiste o retorno do executor e valida saldo disponível descontando reservas concorrentes. */
  @Transactional
  public VideoProviderPreflight complete(
      VideoProductionCycle cycle, VideoProviderFinancialPreflightData.Result request) {
    VideoProviderAccount account = lockedAccount(request.accountKey());
    VideoProviderPreflight preflight = preflightForUpdate(cycle.getId());
    if (!"PENDING".equals(preflight.getStatus())) {
      if (sameCompletedResult(preflight, request)) return preflight;
      throw conflict("O preflight do ciclo já foi concluído com outro resultado.");
    }
    if (!account.getId().equals(preflight.getProviderAccountId())) {
      throw conflict("A conta informada não pertence ao preflight.");
    }
    if (!account.getSourceUrl().equals(request.sourceUrl().trim())) {
      throw badRequest("A fonte do snapshot diverge da API oficial configurada para a conta.");
    }
    Instant receivedAt = Instant.now(clock);
    validateObservationTime(cycle.getId(), request.observedAt(), receivedAt);
    releaseExpiredReservations(account, receivedAt);
    Instant expiresAt = receivedAt.plus(SNAPSHOT_TTL);
    String requestedStatus = request.status().trim().toUpperCase(Locale.ROOT);
    if (!Set.of("READY", "BLOCKED").contains(requestedStatus)) {
      throw badRequest("Estado de preflight inválido.");
    }
    applyRawResult(preflight, request, expiresAt, receivedAt);
    if ("READY".equals(requestedStatus)) {
      validateReadyResult(request);
      RouteTotals routes = validateContractualRoutes(request, cycle, account);
      updateOfficialAccountSnapshot(account, request, expiresAt, receivedAt);
      BigDecimal reserved = nonNull(account.getReservedCredits());
      BigDecimal available =
          request.officialBalanceCredits().subtract(reserved).max(BigDecimal.ZERO);
      preflight.setReservedCreditsSnapshot(reserved);
      preflight.setAvailableCreditsSnapshot(available);
      preflight.setEstimatedCostUsd(
          request.estimatedCredits().multiply(account.getCreditUnitUsd()));
      BigDecimal maximumCost = routes.maximumCredits().multiply(account.getCreditUnitUsd());
      if (maximumCost.compareTo(cycle.getBudgetLimitUsd()) > 0) {
        preflight.setStatus("READY_WITH_BLOCKER");
        preflight.setFailureCode("PROVIDER_ROUTER_CEILING_UNSAFE");
        preflight.setFailureDetail(
            "A soma dos tetos por geração ultrapassa o teto do ciclo; Plutus deve rejeitar sem comprar créditos.");
      } else if (!routes.unavailableModels().isEmpty()) {
        preflight.setStatus("READY_WITH_BLOCKER");
        preflight.setFailureCode("PROVIDER_ROUTE_NOT_HOMOLOGATED");
        preflight.setFailureDetail(
            "O Router escolheu modelo sem licença, qualidade, preço ou adapter integralmente homologados: "
                + String.join(", ", routes.unavailableModels())
                + ". Plutus deve rejeitar e orientar ajuste da allowlist.");
      } else if (StringUtils.hasText(request.failureCode())) {
        preflight.setStatus("READY_WITH_BLOCKER");
      } else if (available.compareTo(routes.maximumCredits()) < 0) {
        preflight.setStatus("READY_WITH_BLOCKER");
        preflight.setFailureCode("INSUFFICIENT_AVAILABLE_CREDITS");
        preflight.setFailureDetail(
            "Saldo oficial menos reservas ativas é insuficiente para cobrir o teto das rotas; Plutus deve orientar a recarga mínima sem efetuar compra.");
      } else {
        preflight.setStatus("READY");
      }
    } else {
      preflight.setStatus("BLOCKED");
      if (StringUtils.hasText(request.payloadSha256())) {
        validateReadyResult(request);
        validateContractualRoutes(request, cycle, account);
        updateOfficialAccountSnapshot(account, request, expiresAt, receivedAt);
      }
      account.setSnapshotStatus("BLOCKED");
      account.setSnapshotExpiresAt(receivedAt);
      account.setUpdatedAt(receivedAt);
      accountRepository.save(account);
    }
    return preflightRepository.save(preflight);
  }

  /** Reserva atomicamente o teto das rotas antes do parecer de Plutus e de qualquer consumo. */
  @Transactional
  public VideoCreditReservation reserve(VideoProductionCycle cycle) {
    VideoProviderAccount account = accountByCycleForUpdate(cycle.getId());
    Instant now = Instant.now(clock);
    releaseExpiredReservations(account, now);
    VideoCreditReservation existing =
        reservationRepository.findByVideoProductionCycleIdForUpdate(cycle.getId()).orElse(null);
    if (existing != null && Set.of("RESERVED", "CONSUMING").contains(existing.getStatus())) {
      return existing;
    }
    if (existing != null) {
      throw conflict("A reserva do ciclo já foi encerrada e não pode ser reutilizada.");
    }
    VideoProviderPreflight preflight = preflight(cycle.getId());
    if (!account.getId().equals(preflight.getProviderAccountId())) {
      throw conflict("A conta travada não pertence ao preflight do ciclo.");
    }
    if (!"READY".equals(preflight.getStatus())
        || preflight.getExpiresAt() == null
        || !preflight.getExpiresAt().isAfter(now)) {
      throw conflict("O dry run e o snapshot oficial precisam estar válidos antes da reserva.");
    }
    BigDecimal maximumCredits = maximumAuthorizedCredits(preflight.getSelectedRoutesJson());
    BigDecimal maximumCost = maximumCredits.multiply(account.getCreditUnitUsd());
    if (maximumCost.compareTo(cycle.getBudgetLimitUsd()) > 0) {
      throw conflict("O teto máximo das rotas ultrapassa o teto autorizado para o ciclo.");
    }
    requireFreshOfficialSnapshot(account, now);
    BigDecimal available =
        account.getOfficialBalanceCredits().subtract(nonNull(account.getReservedCredits()));
    if (available.compareTo(maximumCredits) < 0) {
      throw conflict("Créditos disponíveis foram reservados por outro ciclo.");
    }
    VideoCreditReservation reservation = new VideoCreditReservation();
    reservation.setVideoProductionCycleId(cycle.getId());
    reservation.setProviderPreflightId(preflight.getId());
    reservation.setProviderAccountId(account.getId());
    reservation.setStatus("RESERVED");
    reservation.setReservedCredits(maximumCredits);
    reservation.setReservedCostUsd(maximumCost);
    reservation.setExpiresAt(now.plus(RESERVATION_TTL));
    reservation.setReservedAt(now);
    reservation.setCreatedAt(now);
    reservation.setUpdatedAt(now);
    account.setReservedCredits(nonNull(account.getReservedCredits()).add(maximumCredits));
    account.setUpdatedAt(now);
    accountRepository.save(account);
    return reservationRepository.save(reservation);
  }

  /** Valida deterministicamente o parecer de Plutus contra o preflight oficial persistido. */
  @Transactional(readOnly = true)
  public void validateFinancialDecision(
      VideoProductionCycle cycle,
      VideoProviderFinancialPreflightData.FinancialDecision request,
      String decision) {
    VideoProviderPreflight preflight = preflight(cycle.getId());
    if (!List.of("READY", "READY_WITH_BLOCKER").contains(preflight.getStatus())) {
      throw conflict("Plutus não pode decidir sem preflight financeiro utilizável.");
    }
    VideoProviderAccount account =
        accountRepository
            .findById(preflight.getProviderAccountId())
            .orElseThrow(() -> conflict("Conta agregadora do preflight não encontrada."));
    String expectedRoute =
        routePrefix(preflight.getRouterConfigId()) + preflight.getRouterConfigId();
    if (!account.getAggregatorName().equalsIgnoreCase(request.recommendedAggregator().trim())
        || !expectedRoute.equalsIgnoreCase(request.recommendedRoute().trim())) {
      throw badRequest("A recomendação de Plutus diverge da conta ou da rota do dry run.");
    }
    if (request.estimatedCostUsd().compareTo(preflight.getEstimatedCostUsd()) != 0) {
      throw badRequest("O custo recomendado por Plutus diverge do dry run persistido.");
    }
    String action = request.creditAction().trim().toUpperCase(Locale.ROOT);
    if ("READY".equals(preflight.getStatus())) {
      if (!"NO_PURCHASE".equals(action)
          || positive(request.recommendedRechargeCredits())
          || StringUtils.hasText(request.rechargeUrl())) {
        throw badRequest("Saldo suficiente exige decisão sem compra ou recarga.");
      }
      if ("APPROVED".equals(decision) && !hasActiveReservation(cycle.getId())) {
        throw conflict("A aprovação de Plutus exige a reserva preventiva ainda vigente.");
      }
      return;
    }
    if (!"INSUFFICIENT_AVAILABLE_CREDITS".equals(preflight.getFailureCode())) {
      String expectedAction =
          "PROVIDER_QUOTA_UNKNOWN".equals(preflight.getFailureCode())
              ? "BLOCKED_UNKNOWN"
              : "NO_PURCHASE";
      if (!"REJECTED".equals(decision)
          || !expectedAction.equals(action)
          || positive(request.recommendedRechargeCredits())
          || StringUtils.hasText(request.rechargeUrl())) {
        throw badRequest(
            "O bloqueio do preflight exige rejeição com a ação de crédito correspondente e sem recarga.");
      }
      return;
    }
    BigDecimal recharge =
        maximumAuthorizedCredits(preflight.getSelectedRoutesJson())
            .subtract(nonNull(preflight.getAvailableCreditsSnapshot()))
            .max(BigDecimal.ZERO);
    if (!"REJECTED".equals(decision)
        || !"RECHARGE_REQUIRED".equals(action)
        || request.recommendedRechargeCredits() == null
        || request.recommendedRechargeCredits().compareTo(recharge) != 0
        || !java.util.Objects.equals(trim(account.getRechargeUrl()), trim(request.rechargeUrl()))) {
      throw badRequest("Saldo insuficiente exige bloqueio e recarga mínima exatamente calculada.");
    }
  }

  /** Exige uma reserva ativa antes de o backend criar qualquer job pago de Apolo. */
  @Transactional(readOnly = true)
  public VideoCreditReservation requireActiveReservation(Long cycleId) {
    VideoCreditReservation reservation =
        reservationRepository
            .findByVideoProductionCycleId(cycleId)
            .orElseThrow(() -> conflict("O ciclo não possui reserva de créditos."));
    if (!Set.of("RESERVED", "CONSUMING").contains(reservation.getStatus())
        || !reservation.getExpiresAt().isAfter(Instant.now(clock))) {
      throw conflict("A reserva de créditos do ciclo não está ativa.");
    }
    return reservation;
  }

  /** Informa se um ciclo legado ainda possui reserva válida sem lançar erro na reconciliação. */
  @Transactional(readOnly = true)
  public boolean hasActiveReservation(Long cycleId) {
    Instant now = Instant.now(clock);
    return reservationRepository
        .findByVideoProductionCycleId(cycleId)
        .filter(value -> Set.of("RESERVED", "CONSUMING").contains(value.getStatus()))
        .filter(value -> value.getExpiresAt() != null && value.getExpiresAt().isAfter(now))
        .isPresent();
  }

  /** Libera a reserva preventiva quando Plutus rejeita sem ter iniciado consumo externo. */
  @Transactional
  public void releaseUnusedReservation(Long cycleId) {
    VideoProviderAccount account = accountByCycleForUpdate(cycleId);
    VideoCreditReservation reservation =
        reservationRepository.findByVideoProductionCycleIdForUpdate(cycleId).orElse(null);
    if (reservation == null || "RELEASED".equals(reservation.getStatus())) return;
    if (!"RESERVED".equals(reservation.getStatus())) {
      throw conflict("Uma reserva com consumo iniciado não pode ser liberada como não utilizada.");
    }
    Instant now = Instant.now(clock);
    account.setReservedCredits(
        nonNull(account.getReservedCredits())
            .subtract(nonNull(reservation.getReservedCredits()))
            .max(BigDecimal.ZERO));
    account.setUpdatedAt(now);
    reservation.setStatus("RELEASED");
    reservation.setReleasedAt(now);
    reservation.setUpdatedAt(now);
    accountRepository.save(account);
    reservationRepository.save(reservation);
  }

  /** Atualiza o consumo observado sem liberar a parcela ainda necessária para o job corrente. */
  @Transactional
  public void observeConsumption(Long cycleId, BigDecimal credits, BigDecimal costUsd) {
    if (cycleId == null) return;
    reservationRepository
        .findByVideoProductionCycleIdForUpdate(cycleId)
        .filter(value -> Set.of("RESERVED", "CONSUMING").contains(value.getStatus()))
        .ifPresent(
            reservation -> {
              reservation.setStatus("CONSUMING");
              reservation.setActualCredits(nonNull(credits));
              reservation.setActualCostUsd(nonNull(costUsd));
              reservation.setUpdatedAt(Instant.now(clock));
              reservationRepository.save(reservation);
            });
  }

  /** Liquida a reserva no término do job e força nova consulta oficial antes de outro ciclo. */
  @Transactional
  public void settle(Long cycleId, BigDecimal credits, BigDecimal costUsd) {
    if (cycleId == null) return;
    VideoProviderAccount account =
        accountRepository.findByVideoProductionCycleIdForUpdate(cycleId).orElse(null);
    if (account == null) return;
    VideoCreditReservation reservation =
        reservationRepository.findByVideoProductionCycleIdForUpdate(cycleId).orElse(null);
    if (reservation == null || "SETTLED".equals(reservation.getStatus())) return;
    if (!Set.of("RESERVED", "CONSUMING").contains(reservation.getStatus())) return;
    if (!account.getId().equals(reservation.getProviderAccountId())) {
      throw conflict("A reserva não pertence à conta travada para o ciclo.");
    }
    Instant now = Instant.now(clock);
    account.setReservedCredits(
        nonNull(account.getReservedCredits())
            .subtract(reservation.getReservedCredits())
            .max(BigDecimal.ZERO));
    account.setSnapshotStatus("STALE_AFTER_CONSUMPTION");
    account.setSnapshotExpiresAt(now);
    account.setUpdatedAt(now);
    reservation.setStatus("SETTLED");
    reservation.setActualCredits(nonNull(credits));
    reservation.setActualCostUsd(nonNull(costUsd));
    reservation.setSettledAt(now);
    reservation.setUpdatedAt(now);
    accountRepository.save(account);
    reservationRepository.save(reservation);
  }

  /** Expõe o preflight e a reserva do ciclo para relatório e tomada de decisão. */
  @Transactional(readOnly = true)
  public VideoProviderFinancialPreflightData.Snapshot snapshot(Long cycleId) {
    VideoProviderPreflight preflight =
        preflightRepository.findByVideoProductionCycleId(cycleId).orElse(null);
    if (preflight == null) return null;
    VideoProviderAccount account =
        accountRepository.findById(preflight.getProviderAccountId()).orElse(null);
    VideoCreditReservation reservation =
        reservationRepository.findByVideoProductionCycleId(cycleId).orElse(null);
    BigDecimal maximumCredits =
        StringUtils.hasText(preflight.getSelectedRoutesJson())
            ? maximumAuthorizedCredits(preflight.getSelectedRoutesJson())
            : null;
    BigDecimal creditUnit = account == null ? null : account.getCreditUnitUsd();
    return new VideoProviderFinancialPreflightData.Snapshot(
        preflight.getId(),
        effectiveStatus(preflight, reservation),
        preflight.getProductionProfile(),
        account == null ? null : account.getAggregatorName(),
        account == null ? null : account.getAccountKey(),
        preflight.getRouterConfigId(),
        preflight.getPayloadSha256(),
        preflight.getSelectedRoutesJson(),
        preflight.getEstimatedCredits(),
        preflight.getEstimatedCostUsd(),
        maximumCredits,
        creditUnit == null || maximumCredits == null ? null : maximumCredits.multiply(creditUnit),
        preflight.getOfficialBalanceCredits(),
        preflight.getReservedCreditsSnapshot(),
        preflight.getAvailableCreditsSnapshot(),
        preflight.getMaxMonthlyCreditSpend(),
        preflight.getQuotaSnapshotJson(),
        preflight.getFailureCode(),
        preflight.getFailureDetail(),
        preflight.getSourceUrl(),
        account == null ? null : account.getRechargeUrl(),
        preflight.getObservedAt(),
        preflight.getExpiresAt(),
        reservationResponse(reservation));
  }

  /** Fornece a Plutus o contexto auditável do preflight e da eficiência histórica disponível. */
  @Transactional(readOnly = true)
  public Map<String, Object> financialContext(Long cycleId) {
    VideoProviderFinancialPreflightData.Snapshot snapshot = snapshot(cycleId);
    LinkedHashMap<String, Object> context = new LinkedHashMap<>();
    context.put("providerPreflight", snapshot);
    context.put(
        "decisionRule",
        "Bloquear se saldo, quota, preço, licença, payload ou rastreabilidade estiverem ausentes.");
    context.put("costPerApprovedMaterial", routeEfficiencyHistory());
    return context;
  }

  /** Devolve as requisições congeladas que o executor deverá repetir sem o sinal de dry run. */
  @Transactional(readOnly = true)
  public String executionRequests(Long cycleId) {
    VideoProviderPreflight preflight = preflight(cycleId);
    if (!StringUtils.hasText(preflight.getExecutionRequestsJson())) {
      throw conflict("O preflight não congelou as requisições de execução.");
    }
    return preflight.getExecutionRequestsJson();
  }

  /** Devolve o hash do payload congelado para verificação no executor. */
  @Transactional(readOnly = true)
  public String payloadSha256(Long cycleId) {
    return preflight(cycleId).getPayloadSha256();
  }

  /** Devolve a configuração de router validada no dry run. */
  @Transactional(readOnly = true)
  public String routerConfigId(Long cycleId) {
    return preflight(cycleId).getRouterConfigId();
  }

  /** Devolve as rotas auditadas para conferência da resposta faturável pelo executor. */
  @Transactional(readOnly = true)
  public String selectedRoutes(Long cycleId) {
    return preflight(cycleId).getSelectedRoutesJson();
  }

  /** Converte a reserva persistida em resposta sanitizada. */
  private VideoProviderFinancialPreflightData.Reservation reservationResponse(
      VideoCreditReservation reservation) {
    if (reservation == null) return null;
    return new VideoProviderFinancialPreflightData.Reservation(
        reservation.getId(),
        reservation.getStatus(),
        reservation.getReservedCredits(),
        reservation.getReservedCostUsd(),
        reservation.getActualCredits(),
        reservation.getActualCostUsd(),
        reservation.getExpiresAt(),
        reservation.getReservedAt(),
        reservation.getSettledAt(),
        reservation.getReleasedAt());
  }

  /** Persiste todos os campos brutos sanitizados antes de aplicar os gates do backend. */
  private void applyRawResult(
      VideoProviderPreflight preflight,
      VideoProviderFinancialPreflightData.Result request,
      Instant expiresAt,
      Instant receivedAt) {
    preflight.setRouterConfigId(trim(request.routerConfigId()));
    preflight.setPayloadSha256(trim(request.payloadSha256()));
    preflight.setExecutionRequestsJson(trim(request.executionRequestsJson()));
    preflight.setOrganizationSnapshotJson(trim(request.organizationSnapshotJson()));
    preflight.setRoutingResponseJson(trim(request.routingResponseJson()));
    preflight.setSelectedRoutesJson(trim(request.selectedRoutesJson()));
    preflight.setEstimatedCredits(request.estimatedCredits());
    preflight.setOfficialBalanceCredits(request.officialBalanceCredits());
    preflight.setMaxMonthlyCreditSpend(request.maxMonthlyCreditSpend());
    preflight.setQuotaSnapshotJson(trim(request.quotaSnapshotJson()));
    preflight.setFailureCode(trim(request.failureCode()));
    preflight.setFailureDetail(trim(request.failureDetail()));
    preflight.setSourceUrl(request.sourceUrl().trim());
    preflight.setObservedAt(request.observedAt());
    preflight.setExpiresAt(expiresAt);
    preflight.setUpdatedAt(receivedAt);
  }

  /** Exige um retorno completo e internamente consistente para o estado READY. */
  private void validateReadyResult(VideoProviderFinancialPreflightData.Result request) {
    if (!StringUtils.hasText(request.routerConfigId())
        || !StringUtils.hasText(request.payloadSha256())
        || !StringUtils.hasText(request.executionRequestsJson())
        || !StringUtils.hasText(request.organizationSnapshotJson())
        || !StringUtils.hasText(request.routingResponseJson())
        || !StringUtils.hasText(request.selectedRoutesJson())
        || request.estimatedCredits() == null
        || request.estimatedCredits().signum() <= 0
        || request.officialBalanceCredits() == null
        || request.maxMonthlyCreditSpend() == null
        || !StringUtils.hasText(request.quotaSnapshotJson())) {
      throw badRequest("Preflight READY exige saldo, quota, rotas, custo e payload auditáveis.");
    }
    validateJsonArray(request.executionRequestsJson(), "requisições do router");
    validateJsonArray(request.routingResponseJson(), "respostas do dry run");
    validateJsonArray(request.selectedRoutesJson(), "rotas selecionadas");
    validateJsonObject(request.organizationSnapshotJson(), "snapshot da organização");
    validateJsonObject(request.quotaSnapshotJson(), "snapshot de quotas");
    if (!StringUtils.hasText(request.usageSnapshotJson())) {
      throw badRequest("Preflight READY exige uso oficial auditável.");
    }
    validateJsonObject(request.usageSnapshotJson(), "snapshot de uso");
    String actualHash = sha256(request.executionRequestsJson());
    if (!actualHash.equalsIgnoreCase(request.payloadSha256().trim())) {
      throw badRequest("Hash do payload de execução diverge do dry run.");
    }
  }

  /** Usa o recebimento do backend como validade e recusa somente desvios de relógio anormais. */
  private void validateObservationTime(Long cycleId, Instant observedAt, Instant receivedAt) {
    if (observedAt == null) {
      throw badRequest("Snapshot oficial não informou o horário de observação.");
    }
    Duration skew = Duration.between(receivedAt, observedAt).abs();
    if (skew.compareTo(MAX_EXECUTOR_CLOCK_SKEW) > 0) {
      throw badRequest("Desvio de relógio do executor excede a janela segura do preflight.");
    }
    if (skew.compareTo(EXPECTED_EXECUTOR_CLOCK_SKEW) > 0) {
      log.warn(
          "Preflight recebido com desvio de relógio; cycleId={} observedAt={} receivedAt={} skewSeconds={}",
          cycleId,
          observedAt,
          receivedAt,
          skew.toSeconds());
    }
  }

  /** Confere a coerência entre requisições, respostas e rotas antes de aceitar o worker. */
  private RouteTotals validateContractualRoutes(
      VideoProviderFinancialPreflightData.Result request,
      VideoProductionCycle cycle,
      VideoProviderAccount account) {
    try {
      JsonNode requests = objectMapper.readTree(request.executionRequestsJson());
      JsonNode responses = objectMapper.readTree(request.routingResponseJson());
      JsonNode selected = objectMapper.readTree(request.selectedRoutesJson());
      JsonNode organization = objectMapper.readTree(request.organizationSnapshotJson());
      if (requests.size() != responses.size() || requests.size() != selected.size()) {
        throw badRequest("Preflight exige uma resposta e uma rota para cada requisição.");
      }
      if (!organization.path("creditBalance").isNumber()
          || !organization.path("tier").path("maxMonthlyCreditSpend").isIntegralNumber()
          || !organization.path("usage").isObject()
          || organization
                  .path("creditBalance")
                  .decimalValue()
                  .compareTo(request.officialBalanceCredits())
              != 0
          || organization.path("tier").path("maxMonthlyCreditSpend").longValue()
              != request.maxMonthlyCreditSpend()) {
        throw badRequest("Saldo ou limite informado diverge do snapshot oficial bruto.");
      }
      if (request.routerConfigId().trim().startsWith("product_ugc@")) {
        return validateProductUgcContract(request, requests, responses, selected, account);
      }
      BigDecimal estimated = BigDecimal.ZERO;
      BigDecimal maximum = BigDecimal.ZERO;
      java.util.LinkedHashSet<String> unavailableModels = new java.util.LinkedHashSet<>();
      for (int index = 0; index < requests.size(); index++) {
        JsonNode executionRequest = requests.get(index);
        JsonNode routingResponse = responses.get(index);
        JsonNode routing = routingResponse.path("routing");
        JsonNode route = selected.get(index);
        String configId = request.routerConfigId().trim();
        BigDecimal routeEstimate = routing.path("estimatedCost").path("credits").decimalValue();
        BigDecimal routeCeiling =
            routing.path("resolvedSettings").path("priceCeiling").decimalValue();
        if (!routingResponse.path("dryRun").isBoolean()
            || !routingResponse.path("dryRun").asBoolean()
            || !executionRequest.path("configId").asText().equals(configId)
            || executionRequest.has("dryRun")
            || !executionRequest.path("input").isObject()
            || !routing.path("configId").asText().equals(configId)
            || !routing.path("model").isTextual()
            || routing.path("model").asText().isBlank()
            || !routing.path("provider").isTextual()
            || routing.path("provider").asText().isBlank()
            || !routing.path("resolvedInput").isObject()
            || !routing.path("resolvedSettings").path("optimizeFor").isTextual()
            || !routing.path("resolvedSettings").path("priceCeiling").isNumber()
            || !routing.path("estimatedCost").path("credits").isNumber()
            || routeEstimate.signum() <= 0
            || routeCeiling.signum() <= 0
            || routeEstimate.compareTo(routeCeiling) > 0
            || !routing.path("model").asText().equals(route.path("model").asText())
            || !routing.path("provider").asText().equals(route.path("manufacturer").asText())
            || !account.getAggregatorName().equals(route.path("aggregator").asText())
            || !account.getAccountKey().equals(route.path("accountKey").asText())
            || !configId.equals(route.path("routerConfigId").asText())
            || !("RUNWAY_ROUTER:" + configId).equals(route.path("batchRouteId").asText())
            || !route.path("optimizeFor").isTextual()
            || !routing
                .path("resolvedSettings")
                .path("optimizeFor")
                .asText()
                .equals(route.path("optimizeFor").asText())
            || !route.path("estimatedCredits").isNumber()
            || !route.path("priceCeilingCredits").isNumber()
            || route.path("estimatedCredits").decimalValue().compareTo(routeEstimate) != 0
            || route.path("priceCeilingCredits").decimalValue().compareTo(routeCeiling) != 0) {
          throw badRequest("Rota do dry run diverge do payload, da conta ou da decisão do Router.");
        }
        estimated = estimated.add(routeEstimate);
        maximum = maximum.add(routeCeiling);
        String selectedModel = routing.path("model").asText();
        SalesVideoProviderModel catalogModel =
            providerModelRepository
                .findByAdapterKeyAndExternalModelId("RUNWAY", selectedModel)
                .orElse(null);
        if (catalogModel == null
            || !"ACTIVE".equals(catalogModel.getLifecycleStatus())
            || !catalogModel.isAdapterVerified()
            || !catalogModel.isPricingVerified()
            || !catalogModel.isCommercialLicenseVerified()
            || !catalogModel.isQualityGateVerified()) {
          unavailableModels.add(selectedModel);
        }
      }
      if (estimated.compareTo(request.estimatedCredits()) != 0) {
        throw badRequest("Custo total informado diverge das respostas do Router.");
      }
      return new RouteTotals(maximum, List.copyOf(unavailableModels));
    } catch (JsonProcessingException ex) {
      log.error("Falha ao validar contrato de rotas do preflight; cycleId={}", cycle.getId(), ex);
      throw badRequest("Preflight contém JSON de rota inválido.");
    }
  }

  /** Valida a receita Product UGC contra versão, payload e tarifa oficiais pinados. */
  private RouteTotals validateProductUgcContract(
      VideoProviderFinancialPreflightData.Result request,
      JsonNode requests,
      JsonNode responses,
      JsonNode selected,
      VideoProviderAccount account) {
    if (requests.size() != 1 || responses.size() != 1 || selected.size() != 1) {
      throw badRequest("Product UGC exige exatamente uma requisição, uma simulação e uma rota.");
    }
    String configId = request.routerConfigId().trim();
    JsonNode executionRequest = requests.get(0);
    JsonNode evidence = responses.get(0);
    JsonNode route = selected.get(0);
    int duration = executionRequest.path("duration").asInt(0);
    String ratio = executionRequest.path("ratio").asText();
    BigDecimal expectedCredits = productUgcCredits(duration, ratio);
    boolean valid =
        "product_ugc@2026-06".equals(configId)
            && "2026-06".equals(executionRequest.path("version").asText())
            && isHttpsText(executionRequest.path("characterImage").path("uri"))
            && isHttpsText(executionRequest.path("productImage").path("uri"))
            && executionRequest.path("productInfo").isTextual()
            && executionRequest.path("productInfo").asText().length() <= 2500
            && executionRequest.path("userConcept").isTextual()
            && executionRequest.path("userConcept").asText().length() <= 3500
            && duration >= 4
            && duration <= 15
            && Set.of("720:1280", "1080:1920").contains(ratio)
            && executionRequest.path("audio").isBoolean()
            && !executionRequest.path("audio").asBoolean()
            && "DETERMINISTIC_RATE_CARD".equals(evidence.path("simulation").asText())
            && "product_ugc".equals(evidence.path("recipe").asText())
            && "2026-06".equals(evidence.path("version").asText())
            && evidence.path("estimatedCost").path("credits").isNumber()
            && expectedCredits.compareTo(
                    evidence.path("estimatedCost").path("credits").decimalValue())
                == 0
            && "Runway".equals(route.path("manufacturer").asText())
            && "product_ugc".equals(route.path("model").asText())
            && account.getAggregatorName().equals(route.path("aggregator").asText())
            && account.getAccountKey().equals(route.path("accountKey").asText())
            && configId.equals(route.path("routerConfigId").asText())
            && ("RUNWAY_PRODUCT_UGC:" + configId).equals(route.path("batchRouteId").asText())
            && "QUALITY".equals(route.path("optimizeFor").asText())
            && route.path("estimatedCredits").isNumber()
            && route.path("priceCeilingCredits").isNumber()
            && expectedCredits.compareTo(route.path("estimatedCredits").decimalValue()) == 0
            && expectedCredits.compareTo(route.path("priceCeilingCredits").decimalValue()) == 0
            && validProductUgcReferenceEvidence(route.path("referenceImages"))
            && expectedCredits.compareTo(request.estimatedCredits()) == 0;
    if (!valid) {
      throw badRequest("Contrato Product UGC diverge da versão, referências ou tarifa pinadas.");
    }
    SalesVideoProviderModel catalogModel =
        providerModelRepository
            .findByAdapterKeyAndExternalModelId("RUNWAY", "product_ugc")
            .orElse(null);
    boolean unavailable =
        catalogModel == null
            || !"ACTIVE".equals(catalogModel.getLifecycleStatus())
            || !catalogModel.isAdapterVerified()
            || !catalogModel.isPricingVerified()
            || !catalogModel.isCommercialLicenseVerified()
            || !catalogModel.isQualityGateVerified();
    return new RouteTotals(expectedCredits, unavailable ? List.of("product_ugc") : List.of());
  }

  /** Exige provas raster imutáveis das duas referências antes de reservar créditos Product UGC. */
  private boolean validProductUgcReferenceEvidence(JsonNode references) {
    if (!references.isArray() || references.size() != 2) {
      return false;
    }
    Set<String> roles = new HashSet<>();
    for (JsonNode reference : references) {
      String role = reference.path("role").asText();
      String contentType = reference.path("contentType").asText();
      int width = reference.path("width").asInt(0);
      int height = reference.path("height").asInt(0);
      double ratio = height <= 0 ? 0 : width / (double) height;
      boolean valid =
          Set.of("CHARACTER_IMAGE", "PRODUCT_IMAGE").contains(role)
              && StringUtils.hasText(reference.path("sourceHost").asText())
              && Set.of("image/png", "image/jpeg").contains(contentType)
              && reference.path("contentLength").asLong(0) > 0
              && width > 0
              && height > 0
              && ratio >= 0.4
              && ratio <= 4.0
              && reference.path("sha256").asText().matches("[0-9a-f]{64}");
      if (!valid || !roles.add(role)) {
        return false;
      }
    }
    return roles.equals(Set.of("CHARACTER_IMAGE", "PRODUCT_IMAGE"));
  }

  /** Calcula os créditos da receita conforme duração e resolução oficiais da versão 2026-06. */
  private BigDecimal productUgcCredits(int duration, String ratio) {
    if (duration < 4 || duration > 15 || !Set.of("720:1280", "1080:1920").contains(ratio)) {
      return BigDecimal.valueOf(-1);
    }
    boolean fullHd = "1080:1920".equals(ratio);
    int base = fullHd ? 208 : 192;
    int additional = fullHd ? 40 : 36;
    return BigDecimal.valueOf(base + (long) additional * (duration - 4));
  }

  /** Confirma referência HTTPS preenchida no payload congelado. */
  private boolean isHttpsText(JsonNode node) {
    return node.isTextual() && node.asText().startsWith("https://");
  }

  /** Soma os tetos duros por geração preservados nas rotas validadas. */
  private BigDecimal maximumAuthorizedCredits(String selectedRoutesJson) {
    try {
      JsonNode routes = objectMapper.readTree(selectedRoutesJson);
      BigDecimal maximum = BigDecimal.ZERO;
      for (JsonNode route : routes) {
        if (!route.path("priceCeilingCredits").isNumber()
            || route.path("priceCeilingCredits").decimalValue().signum() <= 0) {
          throw conflict("Rota persistida não possui teto de créditos válido.");
        }
        maximum = maximum.add(route.path("priceCeilingCredits").decimalValue());
      }
      return maximum;
    } catch (JsonProcessingException ex) {
      log.error("Falha ao somar tetos das rotas do preflight", ex);
      throw conflict("Rotas persistidas do preflight contêm JSON inválido.");
    }
  }

  /** Atualiza a fotografia oficial da conta preservando reservas locais já existentes. */
  private void updateOfficialAccountSnapshot(
      VideoProviderAccount account,
      VideoProviderFinancialPreflightData.Result request,
      Instant expiresAt,
      Instant receivedAt) {
    account.setOfficialBalanceCredits(request.officialBalanceCredits());
    account.setMaxMonthlyCreditSpend(request.maxMonthlyCreditSpend());
    account.setQuotaSnapshotJson(request.quotaSnapshotJson());
    account.setUsageSnapshotJson(request.usageSnapshotJson());
    account.setSnapshotStatus("READY");
    account.setSnapshotObservedAt(request.observedAt());
    account.setSnapshotExpiresAt(expiresAt);
    account.setSourceUrl(request.sourceUrl().trim());
    account.setUpdatedAt(receivedAt);
    accountRepository.save(account);
  }

  /** Libera somente reservas vencidas que ainda não iniciaram consumo externo. */
  private void releaseExpiredReservations(VideoProviderAccount account, Instant now) {
    List<VideoCreditReservation> expired =
        reservationRepository.findByProviderAccountIdAndStatusAndExpiresAtLessThanEqual(
            account.getId(), "RESERVED", now);
    if (expired.isEmpty()) return;
    BigDecimal released = BigDecimal.ZERO;
    for (VideoCreditReservation reservation : expired) {
      released = released.add(nonNull(reservation.getReservedCredits()));
      reservation.setStatus("RELEASED");
      reservation.setReleasedAt(now);
      reservation.setUpdatedAt(now);
    }
    account.setReservedCredits(
        nonNull(account.getReservedCredits()).subtract(released).max(BigDecimal.ZERO));
    reservationRepository.saveAll(expired);
  }

  /** Impede que uma conta vencida seja usada apenas porque o dry run ainda está em memória. */
  private void requireFreshOfficialSnapshot(VideoProviderAccount account, Instant now) {
    if (!"READY".equals(account.getSnapshotStatus())
        || account.getOfficialBalanceCredits() == null
        || account.getSnapshotExpiresAt() == null
        || !account.getSnapshotExpiresAt().isAfter(now)) {
      throw conflict("O saldo oficial da conta agregadora está ausente ou vencido.");
    }
  }

  /** Indica visualmente que um resultado READY expirou e não pode mais autorizar consumo. */
  private String effectiveStatus(
      VideoProviderPreflight preflight, VideoCreditReservation reservation) {
    boolean activeReservation =
        reservation != null
            && Set.of("RESERVED", "CONSUMING").contains(reservation.getStatus())
            && reservation.getExpiresAt() != null
            && reservation.getExpiresAt().isAfter(Instant.now(clock));
    if (!activeReservation
        && List.of("READY", "READY_WITH_BLOCKER").contains(preflight.getStatus())
        && (preflight.getExpiresAt() == null
            || !preflight.getExpiresAt().isAfter(Instant.now(clock)))) {
      return "EXPIRED";
    }
    return preflight.getStatus();
  }

  /** Confirma repetição idempotente do mesmo callback já persistido. */
  private boolean sameCompletedResult(
      VideoProviderPreflight preflight, VideoProviderFinancialPreflightData.Result request) {
    boolean compatibleStatus =
        preflight.getStatus().equalsIgnoreCase(request.status())
            || ("READY_WITH_BLOCKER".equals(preflight.getStatus())
                && "READY".equalsIgnoreCase(request.status()));
    return compatibleStatus
        && java.util.Objects.equals(
            trim(preflight.getPayloadSha256()), trim(request.payloadSha256()))
        && java.util.Objects.equals(trim(preflight.getFailureCode()), trim(request.failureCode()));
  }

  /** Resume eficiência histórica somente com cobertura financeira e editorial explicitadas. */
  private List<Map<String, Object>> routeEfficiencyHistory() {
    List<ProviderRouteEfficiencyView> rows =
        taskConsumptionQueryService.summarizeEfficiencyByProvider("RUNWAY");
    return rows == null ? List.of() : rows.stream().map(this::routeEfficiency).toList();
  }

  /** Converte uma agregação SQL em base comparável para a decisão de Plutus. */
  private Map<String, Object> routeEfficiency(ProviderRouteEfficiencyView row) {
    long taskCount = row.taskCount();
    long settledTaskCount = row.settledTaskCount();
    BigDecimal knownCost = row.knownCostUsd();
    long evaluatedTaskCount = row.evaluatedTaskCount();
    BigDecimal utilizationPoints = row.utilizationPoints();
    BigDecimal equivalentApprovedMaterials =
        utilizationPoints.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
    boolean completeCoverage =
        taskCount > 0 && settledTaskCount == taskCount && evaluatedTaskCount == taskCount;
    LinkedHashMap<String, Object> result = new LinkedHashMap<>();
    result.put("model", row.model());
    result.put("taskCount", taskCount);
    result.put("settledTaskCount", settledTaskCount);
    result.put("evaluatedTaskCount", evaluatedTaskCount);
    result.put("knownCostUsd", knownCost);
    result.put("equivalentApprovedMaterials", equivalentApprovedMaterials);
    result.put("completeCoverage", completeCoverage);
    result.put(
        "costPerApprovedMaterialUsd",
        completeCoverage && equivalentApprovedMaterials.signum() > 0
            ? knownCost.divide(equivalentApprovedMaterials, 6, RoundingMode.HALF_UP)
            : null);
    return result;
  }

  /** Valida um array JSON antes de persistir o contrato auditável. */
  private void validateJsonArray(String value, String label) {
    try {
      JsonNode node = objectMapper.readTree(value);
      if (!node.isArray() || node.isEmpty())
        throw badRequest(label + " deve ser um array preenchido.");
    } catch (JsonProcessingException ex) {
      log.error("Falha ao validar {} do preflight", label, ex);
      throw badRequest(label + " contém JSON inválido.");
    }
  }

  /** Valida um objeto JSON antes de persistir o snapshot sanitizado. */
  private void validateJsonObject(String value, String label) {
    try {
      if (!objectMapper.readTree(value).isObject()) {
        throw badRequest(label + " deve ser um objeto JSON.");
      }
    } catch (JsonProcessingException ex) {
      log.error("Falha ao validar {} do preflight", label, ex);
      throw badRequest(label + " contém JSON inválido.");
    }
  }

  /** Calcula o SHA-256 estável das requisições exatas que sucederam no dry run. */
  private String sha256(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException ex) {
      log.error("SHA-256 indisponível ao validar preflight", ex);
      throw new IllegalStateException("SHA-256 indisponível.", ex);
    }
  }

  /** Resolve a duração máxima de clipe permitida pelo plano sem escolher o modelo externo. */
  private int providerClipDuration(String providerPlan) {
    String plan = providerPlan == null ? "" : providerPlan.toUpperCase(Locale.ROOT);
    if (plan.contains("(RUNWAY_PRODUCT_UGC)")) return 15;
    if (plan.contains("VEO_3_1")) return 8;
    if (plan.contains("SEEDANCE_2")) return 15;
    return 10;
  }

  /** Identifica a receita Product UGC sem inferir escolha a partir de texto comercial livre. */
  private boolean isProductUgc(String providerPlan) {
    return providerPlan != null
        && providerPlan.toUpperCase(Locale.ROOT).contains("(RUNWAY_PRODUCT_UGC)");
  }

  /** Resolve o prefixo financeiro da rota pinada ou do Model Router. */
  private String routePrefix(String configId) {
    return configId != null && configId.startsWith("product_ugc@")
        ? "RUNWAY_PRODUCT_UGC:"
        : "RUNWAY_ROUTER:";
  }

  /** Normaliza o perfil financeiro e impede opções implícitas de custo. */
  private String profile(String value) {
    String normalized = value == null ? "FINAL_CAMPAIGN" : value.trim().toUpperCase(Locale.ROOT);
    if (!PROFILES.contains(normalized)) throw badRequest("Perfil de produção inválido.");
    return normalized;
  }

  /** Localiza a conta sem lock para operações que não alteram o saldo reservado. */
  private VideoProviderAccount account(String key) {
    return accountRepository
        .findByAccountKey(key)
        .orElseThrow(() -> conflict("Conta agregadora não configurada: " + key));
  }

  /** Localiza a conta com lock para serializar callbacks e reservas concorrentes. */
  private VideoProviderAccount lockedAccount(String key) {
    return accountRepository
        .findByAccountKeyForUpdate(key)
        .orElseThrow(() -> conflict("Conta agregadora não configurada: " + key));
  }

  /** Trava a conta do ciclo como primeira leitura para serializar reservas concorrentes. */
  private VideoProviderAccount accountByCycleForUpdate(Long cycleId) {
    return accountRepository
        .findByVideoProductionCycleIdForUpdate(cycleId)
        .orElseThrow(() -> conflict("Conta agregadora do ciclo não encontrada."));
  }

  /** Localiza o preflight único do ciclo. */
  private VideoProviderPreflight preflight(Long cycleId) {
    return preflightRepository
        .findByVideoProductionCycleId(cycleId)
        .orElseThrow(() -> conflict("Preflight do ciclo não encontrado."));
  }

  /** Lê o preflight corrente sob lock para tornar callbacks concorrentes idempotentes. */
  private VideoProviderPreflight preflightForUpdate(Long cycleId) {
    return preflightRepository
        .findByVideoProductionCycleIdForUpdate(cycleId)
        .orElseThrow(() -> conflict("Preflight do ciclo não encontrado."));
  }

  /** Converte valor financeiro ausente em zero somente para acumuladores internos. */
  private BigDecimal nonNull(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

  /** Informa se uma recomendação opcional de recarga possui valor financeiro positivo. */
  private boolean positive(BigDecimal value) {
    return value != null && value.signum() > 0;
  }

  /** Remove espaços de um campo opcional preservando ausência real. */
  private String trim(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }

  /** Cria uma resposta 400 para payload externo inválido. */
  private ResponseStatusException badRequest(String message) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }

  /** Cria uma resposta 409 para estado financeiro incompatível com a operação. */
  private ResponseStatusException conflict(String message) {
    return new ResponseStatusException(HttpStatus.CONFLICT, message);
  }

  /** Representa o teto máximo e os modelos ainda não homologados do lote. */
  private record RouteTotals(BigDecimal maximumCredits, List<String> unavailableModels) {}
}

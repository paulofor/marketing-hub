package com.marketinghub.salesvideo.service.providerpreflight;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.marketinghub.salesvideo.VideoProviderAccount;
import com.marketinghub.salesvideo.VideoProviderPreflight;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: proteger saldo oficial, dry run, decisão de Plutus e reserva de vídeo. */
@ExtendWith(MockitoExtension.class)
class VideoProviderFinancialPreflightServiceTest {
  @Mock private VideoProviderAccountRepository accountRepository;
  @Mock private VideoProviderPreflightRepository preflightRepository;
  @Mock private VideoCreditReservationRepository reservationRepository;
  @Mock private SalesVideoProviderModelRepository providerModelRepository;
  @Mock private StudioProviderTaskConsumptionQueryService taskConsumptionQueryService;
  private VideoProviderFinancialPreflightService service;
  private VideoProviderAccount account;
  private VideoProviderPreflight preflight;

  /** Prepara uma conta e um preflight persistidos sem consultar a Runway real. */
  @BeforeEach
  void setUp() {
    service =
        new VideoProviderFinancialPreflightService(
            accountRepository,
            preflightRepository,
            reservationRepository,
            providerModelRepository,
            taskConsumptionQueryService,
            new ObjectMapper().findAndRegisterModules());
    account = account();
    preflight = preflight();
    lenient()
        .when(preflightRepository.findByVideoProductionCycleId(11L))
        .thenReturn(Optional.of(preflight));
    lenient()
        .when(preflightRepository.findByVideoProductionCycleIdForUpdate(11L))
        .thenReturn(Optional.of(preflight));
    lenient()
        .when(providerModelRepository.findByAdapterKeyAndExternalModelId("RUNWAY", "gen4_turbo"))
        .thenReturn(Optional.of(activeProviderModel()));
    lenient()
        .when(providerModelRepository.findByAdapterKeyAndExternalModelId("RUNWAY", "product_ugc"))
        .thenReturn(Optional.of(activeProductUgcModel()));
    lenient()
        .when(taskConsumptionQueryService.summarizeEfficiencyByProvider("RUNWAY"))
        .thenReturn(java.util.List.of());
  }

  /** Persiste saldo e custo oficiais e desconta reservas concorrentes do saldo utilizável. */
  @Test
  void shouldCompleteReadyPreflightWithAvailableCredits() {
    when(accountRepository.findByAccountKeyForUpdate("RUNWAY_PRIMARY"))
        .thenReturn(Optional.of(account));
    when(preflightRepository.save(any(VideoProviderPreflight.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    VideoProviderPreflight result = service.complete(cycle(), readyResult("25.0000", "80.0000"));

    assertThat(result.getStatus()).isEqualTo("READY");
    assertThat(result.getEstimatedCostUsd()).isEqualByComparingTo("0.250000");
    assertThat(result.getReservedCreditsSnapshot()).isEqualByComparingTo("10.0000");
    assertThat(result.getAvailableCreditsSnapshot()).isEqualByComparingTo("70.0000");
    assertThat(account.getOfficialBalanceCredits()).isEqualByComparingTo("80.0000");
    assertThat(account.getSnapshotStatus()).isEqualTo("READY");
    verify(accountRepository).save(account);
  }

  /** Mantém o snapshot utilizável para Plutus, mas bloqueia geração quando falta saldo. */
  @Test
  void shouldExposeExactRechargeNeedWhenAvailableBalanceIsInsufficient() {
    when(accountRepository.findByAccountKeyForUpdate("RUNWAY_PRIMARY"))
        .thenReturn(Optional.of(account));
    when(accountRepository.findById(5L)).thenReturn(Optional.of(account));
    when(preflightRepository.save(any(VideoProviderPreflight.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    VideoProviderPreflight result = service.complete(cycle(), readyResult("25.0000", "30.0000"));

    assertThat(result.getStatus()).isEqualTo("READY_WITH_BLOCKER");
    assertThat(result.getFailureCode()).isEqualTo("INSUFFICIENT_AVAILABLE_CREDITS");
    assertThat(result.getAvailableCreditsSnapshot()).isEqualByComparingTo("20.0000");
    service.validateFinancialDecision(
        cycle(), decision("RECHARGE_REQUIRED", "10.0000", account.getRechargeUrl()), "REJECTED");
    assertThatThrownBy(
            () ->
                service.validateFinancialDecision(
                    cycle(), decision("NO_PURCHASE", "0", null), "APPROVED"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("400");
  }

  /** Encaminha a Plutus um bloqueio quando o Router escolhe modelo ainda não homologado. */
  @Test
  void shouldBlockSelectedModelWithoutCommercialGates() {
    SalesVideoProviderModel model = activeProviderModel();
    model.setLifecycleStatus("HOMOLOGATION");
    model.setQualityGateVerified(false);
    when(providerModelRepository.findByAdapterKeyAndExternalModelId("RUNWAY", "gen4_turbo"))
        .thenReturn(Optional.of(model));
    when(accountRepository.findByAccountKeyForUpdate("RUNWAY_PRIMARY"))
        .thenReturn(Optional.of(account));
    when(accountRepository.findById(5L)).thenReturn(Optional.of(account));
    when(preflightRepository.save(any(VideoProviderPreflight.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    VideoProviderPreflight result = service.complete(cycle(), readyResult("25.0000", "80.0000"));

    assertThat(result.getStatus()).isEqualTo("READY_WITH_BLOCKER");
    assertThat(result.getFailureCode()).isEqualTo("PROVIDER_ROUTE_NOT_HOMOLOGATED");
    assertThat(result.getFailureDetail()).contains("gen4_turbo", "allowlist");
    assertThatCode(
            () ->
                service.validateFinancialDecision(
                    cycle(), decision("NO_PURCHASE", "0", null), "REJECTED"))
        .doesNotThrowAnyException();
  }

  /** Reserva BLOCKED_UNKNOWN somente ao caso em que a quota oficial permaneceu desconhecida. */
  @Test
  void shouldRequireUnknownActionOnlyForUnknownQuota() {
    preflight.setStatus("READY_WITH_BLOCKER");
    preflight.setFailureCode("PROVIDER_QUOTA_UNKNOWN");
    preflight.setRouterConfigId("marketing-hub-campaign-final-v1");
    preflight.setEstimatedCostUsd(new BigDecimal("0.250000"));
    preflight.setSelectedRoutesJson(selectedRoutes("25.0000", "30.0000"));
    when(accountRepository.findById(5L)).thenReturn(Optional.of(account));

    assertThatCode(
            () ->
                service.validateFinancialDecision(
                    cycle(), decision("BLOCKED_UNKNOWN", "0", null), "REJECTED"))
        .doesNotThrowAnyException();
    assertThatThrownBy(
            () ->
                service.validateFinancialDecision(
                    cycle(), decision("NO_PURCHASE", "0", null), "REJECTED"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("400");
  }

  /** Rejeita callback cujo payload faturável não corresponde ao hash do dry run. */
  @Test
  void shouldRejectTamperedExecutionPayload() {
    when(accountRepository.findByAccountKeyForUpdate("RUNWAY_PRIMARY"))
        .thenReturn(Optional.of(account));
    VideoProviderFinancialPreflightData.Result request = readyResult("25.0000", "80.0000");
    request =
        new VideoProviderFinancialPreflightData.Result(
            request.status(),
            request.accountKey(),
            request.routerConfigId(),
            "0".repeat(64),
            request.executionRequestsJson(),
            request.organizationSnapshotJson(),
            request.routingResponseJson(),
            request.selectedRoutesJson(),
            request.estimatedCredits(),
            request.officialBalanceCredits(),
            request.maxMonthlyCreditSpend(),
            request.quotaSnapshotJson(),
            request.usageSnapshotJson(),
            request.failureCode(),
            request.failureDetail(),
            request.sourceUrl(),
            request.observedAt());

    VideoProviderFinancialPreflightData.Result tampered = request;
    assertThatThrownBy(() -> service.complete(cycle(), tampered))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Hash do payload");
    verify(accountRepository, never()).save(any(VideoProviderAccount.class));
  }

  /** Recusa uma resposta paga ou ambígua apresentada como se fosse simulação sem cobrança. */
  @Test
  void shouldRejectRoutingResponseWithoutDryRunProof() {
    when(accountRepository.findByAccountKeyForUpdate("RUNWAY_PRIMARY"))
        .thenReturn(Optional.of(account));
    VideoProviderFinancialPreflightData.Result request = readyResult("25.0000", "80.0000");
    VideoProviderFinancialPreflightData.Result unproved =
        withRoutingResponse(
            request, request.routingResponseJson().replace("\"dryRun\":true", "\"dryRun\":false"));

    assertThatThrownBy(() -> service.complete(cycle(), unproved))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Rota do dry run diverge");
    verify(accountRepository, never()).save(any(VideoProviderAccount.class));
  }

  /** Aceita Product UGC somente com payload, tarifa e rota pinados em 2026-06. */
  @Test
  void shouldValidatePinnedProductUgcRateCardWithoutPretendingDryRun() {
    when(accountRepository.findByAccountKeyForUpdate("RUNWAY_PRIMARY"))
        .thenReturn(Optional.of(account));
    when(accountRepository.findById(5L)).thenReturn(Optional.of(account));
    when(preflightRepository.save(any(VideoProviderPreflight.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    VideoProviderPreflight result = service.complete(productUgcCycle(), productUgcResult());

    assertThat(result.getStatus()).isEqualTo("READY");
    assertThat(result.getRouterConfigId()).isEqualTo("product_ugc@2026-06");
    assertThat(result.getEstimatedCredits()).isEqualByComparingTo("648");
    assertThat(result.getEstimatedCostUsd()).isEqualByComparingTo("6.480000");
    assertThat(result.getAvailableCreditsSnapshot()).isEqualByComparingTo("2010");
    assertThatCode(
            () ->
                service.validateFinancialDecision(
                    productUgcCycle(),
                    new VideoProviderFinancialPreflightData.FinancialDecision(
                        "Runway",
                        "RUNWAY_PRODUCT_UGC:product_ugc@2026-06",
                        new BigDecimal("6.480000"),
                        "NO_PURCHASE",
                        BigDecimal.ZERO,
                        null),
                    "REJECTED"))
        .doesNotThrowAnyException();
  }

  /** Recusa referência que não tenha sido comprovada como imagem raster antes da reserva. */
  @Test
  void shouldRejectProductUgcWithoutValidRasterReferenceEvidence() {
    when(accountRepository.findByAccountKeyForUpdate("RUNWAY_PRIMARY"))
        .thenReturn(Optional.of(account));
    VideoProviderFinancialPreflightData.Result valid = productUgcResult();
    VideoProviderFinancialPreflightData.Result invalid =
        new VideoProviderFinancialPreflightData.Result(
            valid.status(),
            valid.accountKey(),
            valid.routerConfigId(),
            valid.payloadSha256(),
            valid.executionRequestsJson(),
            valid.organizationSnapshotJson(),
            valid.routingResponseJson(),
            valid.selectedRoutesJson().replace("image/png", "text/html"),
            valid.estimatedCredits(),
            valid.officialBalanceCredits(),
            valid.maxMonthlyCreditSpend(),
            valid.quotaSnapshotJson(),
            valid.usageSnapshotJson(),
            valid.failureCode(),
            valid.failureDetail(),
            valid.sourceUrl(),
            valid.observedAt());

    assertThatThrownBy(() -> service.complete(productUgcCycle(), invalid))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Contrato Product UGC diverge");
    verify(accountRepository, never()).save(any(VideoProviderAccount.class));
  }

  /** Reserva uma única vez sob lock e impede que outra reserva use saldo já comprometido. */
  @Test
  void shouldReserveCreditsIdempotentlyAndUnderAccountLock() {
    preflight.setStatus("READY");
    preflight.setEstimatedCredits(new BigDecimal("25.0000"));
    preflight.setEstimatedCostUsd(new BigDecimal("0.250000"));
    preflight.setSelectedRoutesJson(selectedRoutes("25.0000", "30.0000"));
    preflight.setExpiresAt(Instant.now().plusSeconds(120));
    account.setOfficialBalanceCredits(new BigDecimal("80.0000"));
    account.setSnapshotStatus("READY");
    account.setSnapshotExpiresAt(Instant.now().plusSeconds(120));
    when(reservationRepository.findByVideoProductionCycleIdForUpdate(11L))
        .thenReturn(Optional.empty());
    when(accountRepository.findByVideoProductionCycleIdForUpdate(11L))
        .thenReturn(Optional.of(account));
    when(reservationRepository.save(any(VideoCreditReservation.class)))
        .thenAnswer(
            invocation -> {
              VideoCreditReservation reservation = invocation.getArgument(0);
              reservation.setId(91L);
              return reservation;
            });

    VideoCreditReservation result = service.reserve(cycle());

    assertThat(result.getStatus()).isEqualTo("RESERVED");
    assertThat(result.getReservedCredits()).isEqualByComparingTo("30.0000");
    assertThat(account.getReservedCredits()).isEqualByComparingTo("40.0000");
    InOrder order = inOrder(accountRepository, reservationRepository);
    order.verify(accountRepository).findByVideoProductionCycleIdForUpdate(11L);
    order.verify(reservationRepository).findByVideoProductionCycleIdForUpdate(11L);

    when(reservationRepository.findByVideoProductionCycleIdForUpdate(11L))
        .thenReturn(Optional.of(result));
    assertThat(service.reserve(cycle())).isSameAs(result);
  }

  /** Libera sob lock a reserva vencida sem consumo antes de calcular o saldo disponível. */
  @Test
  void shouldReleaseExpiredUnusedReservationBeforeNewSnapshot() {
    VideoCreditReservation expired = new VideoCreditReservation();
    expired.setId(90L);
    expired.setProviderAccountId(5L);
    expired.setStatus("RESERVED");
    expired.setReservedCredits(new BigDecimal("20.0000"));
    expired.setExpiresAt(Instant.now().minusSeconds(1));
    account.setReservedCredits(new BigDecimal("30.0000"));
    when(accountRepository.findByAccountKeyForUpdate("RUNWAY_PRIMARY"))
        .thenReturn(Optional.of(account));
    when(reservationRepository.findByProviderAccountIdAndStatusAndExpiresAtLessThanEqual(
            org.mockito.ArgumentMatchers.eq(5L),
            org.mockito.ArgumentMatchers.eq("RESERVED"),
            any(Instant.class)))
        .thenReturn(List.of(expired));
    when(preflightRepository.save(any(VideoProviderPreflight.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    VideoProviderPreflight result = service.complete(cycle(), readyResult("25.0000", "80.0000"));

    assertThat(expired.getStatus()).isEqualTo("RELEASED");
    assertThat(expired.getReleasedAt()).isNotNull();
    assertThat(account.getReservedCredits()).isEqualByComparingTo("10.0000");
    assertThat(result.getAvailableCreditsSnapshot()).isEqualByComparingTo("70.0000");
    verify(reservationRepository).saveAll(List.of(expired));
  }

  /** Libera a reserva, preserva o custo real e invalida o saldo após consumo externo. */
  @Test
  void shouldSettleReservationAndRequireANewOfficialSnapshot() {
    VideoCreditReservation reservation = new VideoCreditReservation();
    reservation.setId(91L);
    reservation.setProviderAccountId(5L);
    reservation.setStatus("CONSUMING");
    reservation.setReservedCredits(new BigDecimal("25.0000"));
    when(reservationRepository.findByVideoProductionCycleIdForUpdate(11L))
        .thenReturn(Optional.of(reservation));
    when(accountRepository.findByVideoProductionCycleIdForUpdate(11L))
        .thenReturn(Optional.of(account));

    service.settle(11L, new BigDecimal("22.0000"), new BigDecimal("0.220000"));

    assertThat(reservation.getStatus()).isEqualTo("SETTLED");
    assertThat(reservation.getActualCredits()).isEqualByComparingTo("22.0000");
    assertThat(account.getReservedCredits()).isEqualByComparingTo("0");
    assertThat(account.getSnapshotStatus()).isEqualTo("STALE_AFTER_CONSUMPTION");
    verify(accountRepository).save(account);
    verify(reservationRepository).save(reservation);
  }

  /** Devolve à conta os créditos preventivos quando Plutus rejeita antes do provider. */
  @Test
  void shouldReleaseUnusedReservationAfterFinancialRejection() {
    VideoCreditReservation reservation = new VideoCreditReservation();
    reservation.setId(91L);
    reservation.setProviderAccountId(5L);
    reservation.setStatus("RESERVED");
    reservation.setReservedCredits(new BigDecimal("30.0000"));
    account.setReservedCredits(new BigDecimal("40.0000"));
    when(accountRepository.findByVideoProductionCycleIdForUpdate(11L))
        .thenReturn(Optional.of(account));
    when(reservationRepository.findByVideoProductionCycleIdForUpdate(11L))
        .thenReturn(Optional.of(reservation));

    service.releaseUnusedReservation(11L);

    assertThat(reservation.getStatus()).isEqualTo("RELEASED");
    assertThat(reservation.getReleasedAt()).isNotNull();
    assertThat(account.getReservedCredits()).isEqualByComparingTo("10.0000");
    verify(accountRepository).save(account);
    verify(reservationRepository).save(reservation);
  }

  /** Mantém o preflight autorizável enquanto a reserva criada do snapshot ainda estiver ativa. */
  @Test
  void shouldKeepReadyStatusWhilePreventiveReservationIsActive() {
    preflight.setStatus("READY");
    preflight.setExpiresAt(Instant.now().minusSeconds(1));
    preflight.setSelectedRoutesJson(selectedRoutes("25.0000", "30.0000"));
    VideoCreditReservation reservation = new VideoCreditReservation();
    reservation.setId(91L);
    reservation.setStatus("RESERVED");
    reservation.setExpiresAt(Instant.now().plusSeconds(120));
    when(accountRepository.findById(5L)).thenReturn(Optional.of(account));
    when(reservationRepository.findByVideoProductionCycleId(11L))
        .thenReturn(Optional.of(reservation));

    VideoProviderFinancialPreflightData.Snapshot snapshot = service.snapshot(11L);

    assertThat(snapshot.status()).isEqualTo("READY");
    assertThat(snapshot.reservation().status()).isEqualTo("RESERVED");
  }

  /** Só calcula custo por material aprovado quando custo e revisão cobrem todas as tasks. */
  @Test
  void shouldExposeCostPerApprovedMaterialOnlyWithCompleteHistory() {
    preflight.setSelectedRoutesJson(selectedRoutes("25.0000", "30.0000"));
    when(accountRepository.findById(5L)).thenReturn(Optional.of(account));
    when(taskConsumptionQueryService.summarizeEfficiencyByProvider("RUNWAY"))
        .thenReturn(
            List.of(
                new ProviderRouteEfficiencyView(
                    "gen4_turbo", 2L, 2L, new BigDecimal("3.000000"), 2L, new BigDecimal("150")),
                new ProviderRouteEfficiencyView(
                    "veo3.1", 2L, 1L, new BigDecimal("5.000000"), 2L, new BigDecimal("200"))));

    Map<String, Object> context = service.financialContext(11L);

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> history =
        (List<Map<String, Object>>) context.get("costPerApprovedMaterial");
    assertThat(history.get(0))
        .containsEntry("completeCoverage", true)
        .containsEntry("costPerApprovedMaterialUsd", new BigDecimal("2.000000"));
    assertThat(history.get(1))
        .containsEntry("completeCoverage", false)
        .containsEntry("costPerApprovedMaterialUsd", null);
  }

  /** Cria o ciclo mínimo usado pelos gates financeiros. */
  private VideoProductionCycle cycle() {
    VideoProductionCycle cycle = new VideoProductionCycle();
    cycle.setId(11L);
    cycle.setBudgetLimitUsd(new BigDecimal("2.00"));
    return cycle;
  }

  /** Cria ciclo cujo teto cobre uma receita premium, sem torná-lo meta de gasto. */
  private VideoProductionCycle productUgcCycle() {
    VideoProductionCycle cycle = cycle();
    cycle.setBudgetLimitUsd(new BigDecimal("7.00"));
    return cycle;
  }

  /** Cria a conta Runway com conversão oficial de um centavo por crédito. */
  private VideoProviderAccount account() {
    VideoProviderAccount value = new VideoProviderAccount();
    value.setId(5L);
    value.setAggregatorName("Runway");
    value.setAccountKey("RUNWAY_PRIMARY");
    value.setCreditUnitUsd(new BigDecimal("0.010000"));
    value.setReservedCredits(new BigDecimal("10.0000"));
    value.setSourceUrl("https://api.dev.runwayml.com/v1/organization");
    value.setRechargeUrl("https://dev.runwayml.com/");
    return value;
  }

  /** Cria a curadoria completa exigida para um modelo selecionado pelo Router. */
  private SalesVideoProviderModel activeProviderModel() {
    SalesVideoProviderModel model = new SalesVideoProviderModel();
    model.setAdapterKey("RUNWAY");
    model.setExternalModelId("gen4_turbo");
    model.setLifecycleStatus("ACTIVE");
    model.setAdapterVerified(true);
    model.setPricingVerified(true);
    model.setCommercialLicenseVerified(true);
    model.setQualityGateVerified(true);
    return model;
  }

  /** Cria a curadoria integral da receita Product UGC versionada. */
  private SalesVideoProviderModel activeProductUgcModel() {
    SalesVideoProviderModel model = activeProviderModel();
    model.setExternalModelId("product_ugc");
    return model;
  }

  /** Cria o preflight pendente associado à conta simulada. */
  private VideoProviderPreflight preflight() {
    VideoProviderPreflight value = new VideoProviderPreflight();
    value.setId(31L);
    value.setVideoProductionCycleId(11L);
    value.setProviderAccountId(5L);
    value.setStatus("PENDING");
    value.setProductionProfile("FINAL_CAMPAIGN");
    return value;
  }

  /** Monta um resultado READY internamente coerente e recente. */
  private VideoProviderFinancialPreflightData.Result readyResult(
      String estimatedCredits, String officialBalance) {
    String requests =
        "[{\"configId\":\"marketing-hub-campaign-final-v1\",\"input\":{\"duration\":10}}]";
    String routes = selectedRoutes(estimatedCredits, "30.0000");
    return new VideoProviderFinancialPreflightData.Result(
        "READY",
        "RUNWAY_PRIMARY",
        "marketing-hub-campaign-final-v1",
        sha256(requests),
        requests,
        "{\"creditBalance\":"
            + officialBalance
            + ",\"tier\":{\"maxMonthlyCreditSpend\":5000},\"usage\":{\"models\":{}}}",
        "[{\"dryRun\":true,\"routing\":{\"provider\":\"Runway\",\"model\":\"gen4_turbo\","
            + "\"configId\":\"marketing-hub-campaign-final-v1\","
            + "\"resolvedInput\":{\"duration\":10},"
            + "\"resolvedSettings\":{\"optimizeFor\":\"quality\",\"priceCeiling\":30.0000},"
            + "\"estimatedCost\":{\"credits\":"
            + estimatedCredits
            + "}}}]",
        routes,
        new BigDecimal(estimatedCredits),
        new BigDecimal(officialBalance),
        5000L,
        "{\"models\":[]}",
        "{\"models\":{}}",
        null,
        null,
        "https://api.dev.runwayml.com/v1/organization",
        Instant.now());
  }

  /** Monta um preflight Product UGC coerente com a tarifa oficial de quinze segundos em 1080p. */
  private VideoProviderFinancialPreflightData.Result productUgcResult() {
    String requests =
        "[{\"version\":\"2026-06\",\"characterImage\":{\"uri\":\"https://assets.example/creator.png\"},"
            + "\"productImage\":{\"uri\":\"https://assets.example/musa.png\"},"
            + "\"productInfo\":\"Experiência digital MUSA\",\"userConcept\":\"Tomada estável\","
            + "\"duration\":15,\"ratio\":\"1080:1920\",\"audio\":false}]";
    String response =
        "[{\"simulation\":\"DETERMINISTIC_RATE_CARD\",\"recipe\":\"product_ugc\","
            + "\"version\":\"2026-06\",\"estimatedCost\":{\"credits\":648}}]";
    String routes =
        "[{\"manufacturer\":\"Runway\",\"model\":\"product_ugc\","
            + "\"aggregator\":\"Runway\",\"accountKey\":\"RUNWAY_PRIMARY\","
            + "\"routerConfigId\":\"product_ugc@2026-06\","
            + "\"batchRouteId\":\"RUNWAY_PRODUCT_UGC:product_ugc@2026-06\","
            + "\"optimizeFor\":\"QUALITY\",\"estimatedCredits\":648,"
            + "\"priceCeilingCredits\":648,\"referenceImages\":["
            + "{\"role\":\"CHARACTER_IMAGE\",\"sourceHost\":\"assets.example\","
            + "\"contentType\":\"image/png\",\"contentLength\":1000,"
            + "\"width\":1080,\"height\":1920,"
            + "\"sha256\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\"},"
            + "{\"role\":\"PRODUCT_IMAGE\",\"sourceHost\":\"assets.example\","
            + "\"contentType\":\"image/png\",\"contentLength\":900,"
            + "\"width\":1080,\"height\":1920,"
            + "\"sha256\":\"bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb\"}]}]";
    return new VideoProviderFinancialPreflightData.Result(
        "READY",
        "RUNWAY_PRIMARY",
        "product_ugc@2026-06",
        sha256(requests),
        requests,
        "{\"creditBalance\":2020,\"tier\":{\"maxMonthlyCreditSpend\":5000},\"usage\":{\"models\":{}}}",
        response,
        routes,
        new BigDecimal("648"),
        new BigDecimal("2020"),
        5000L,
        "{\"recipe\":\"product_ugc\",\"status\":\"ACCOUNT_SNAPSHOT_READY\"}",
        "{\"models\":{}}",
        null,
        null,
        "https://api.dev.runwayml.com/v1/organization",
        Instant.now());
  }

  /** Substitui somente a resposta de roteamento preservando o restante do callback simulado. */
  private VideoProviderFinancialPreflightData.Result withRoutingResponse(
      VideoProviderFinancialPreflightData.Result request, String routingResponseJson) {
    return new VideoProviderFinancialPreflightData.Result(
        request.status(),
        request.accountKey(),
        request.routerConfigId(),
        request.payloadSha256(),
        request.executionRequestsJson(),
        request.organizationSnapshotJson(),
        routingResponseJson,
        request.selectedRoutesJson(),
        request.estimatedCredits(),
        request.officialBalanceCredits(),
        request.maxMonthlyCreditSpend(),
        request.quotaSnapshotJson(),
        request.usageSnapshotJson(),
        request.failureCode(),
        request.failureDetail(),
        request.sourceUrl(),
        request.observedAt());
  }

  /** Monta a rota persistida com custo previsto e teto duro separados. */
  private String selectedRoutes(String estimatedCredits, String maximumCredits) {
    return "[{\"manufacturer\":\"Runway\",\"model\":\"gen4_turbo\","
        + "\"aggregator\":\"Runway\",\"accountKey\":\"RUNWAY_PRIMARY\","
        + "\"routerConfigId\":\"marketing-hub-campaign-final-v1\","
        + "\"batchRouteId\":\"RUNWAY_ROUTER:marketing-hub-campaign-final-v1\","
        + "\"optimizeFor\":\"quality\",\"estimatedCredits\":"
        + estimatedCredits
        + ",\"priceCeilingCredits\":"
        + maximumCredits
        + "}]";
  }

  /** Monta a saída estruturada de Plutus para o cenário informado. */
  private VideoProviderFinancialPreflightData.FinancialDecision decision(
      String action, String rechargeCredits, String rechargeUrl) {
    return new VideoProviderFinancialPreflightData.FinancialDecision(
        "Runway",
        "RUNWAY_ROUTER:marketing-hub-campaign-final-v1",
        new BigDecimal("0.250000"),
        action,
        new BigDecimal(rechargeCredits),
        rechargeUrl);
  }

  /** Calcula o hash esperado pelo contrato de repetição exata do payload. */
  private String sha256(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(ex);
    }
  }
}

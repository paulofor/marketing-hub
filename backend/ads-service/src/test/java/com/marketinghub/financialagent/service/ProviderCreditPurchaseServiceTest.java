package com.marketinghub.financialagent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.financialagent.StudioCostLedgerEntry;
import com.marketinghub.financialagent.StudioProviderCreditPurchase;
import com.marketinghub.financialagent.service.registerProviderCreditPurchase.RegisterProviderCreditPurchaseRequest;
import com.marketinghub.repository.jpa.financialagent.StudioCostLedgerEntryRepository;
import com.marketinghub.repository.jpa.financialagent.StudioProviderCreditPurchaseRepository;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoJobEventRepository;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoJobRepository;
import com.marketinghub.salesvideo.SalesVideoJob;
import com.marketinghub.salesvideo.SalesVideoJobEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Responsabilidade: proteger a separação entre recarga pré-paga e custo consumido do Estúdio. */
class ProviderCreditPurchaseServiceTest {
  /** Confirma o registro auditável da compra comprovada pelo usuário. */
  @Test
  void deveRegistrarCompraDeCreditosRunway() {
    StudioProviderCreditPurchaseRepository repository =
        mock(StudioProviderCreditPurchaseRepository.class);
    Instant purchasedAt = Instant.parse("2026-08-07T22:57:00Z");
    BigDecimal amount = new BigDecimal("10.00");
    when(repository.findByProviderAndPurchasedAtAndAmountAndCurrencyAndCreditsPurchased(
            "RUNWAY", purchasedAt, amount, "USD", 1000))
        .thenReturn(Optional.empty());
    when(repository.save(org.mockito.ArgumentMatchers.any()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    new ProviderCreditPurchaseService(repository)
        .register(
            "runway",
            new RegisterProviderCreditPurchaseRequest(
                purchasedAt, amount, "usd", 1000, "Fatura Runway 07/08/2026"));

    ArgumentCaptor<StudioProviderCreditPurchase> captor =
        ArgumentCaptor.forClass(StudioProviderCreditPurchase.class);
    verify(repository).save(captor.capture());
    assertThat(captor.getValue().getProvider()).isEqualTo("RUNWAY");
    assertThat(captor.getValue().getAmount()).isEqualByComparingTo("10.00");
    assertThat(captor.getValue().getCreditsPurchased()).isEqualTo(1000);
  }

  /** Confirma que um reenvio atualiza o mesmo lançamento em vez de duplicá-lo. */
  @Test
  void deveReutilizarCompraEquivalente() {
    StudioProviderCreditPurchaseRepository repository =
        mock(StudioProviderCreditPurchaseRepository.class);
    Instant purchasedAt = Instant.parse("2026-08-07T22:57:00Z");
    BigDecimal amount = new BigDecimal("10.00");
    StudioProviderCreditPurchase existing = new StudioProviderCreditPurchase();
    existing.setId(85L);
    when(repository.findByProviderAndPurchasedAtAndAmountAndCurrencyAndCreditsPurchased(
            "RUNWAY", purchasedAt, amount, "USD", 1000))
        .thenReturn(Optional.of(existing));
    when(repository.save(existing)).thenReturn(existing);

    var response =
        new ProviderCreditPurchaseService(repository)
            .register(
                "RUNWAY",
                new RegisterProviderCreditPurchaseRequest(purchasedAt, amount, "USD", 1000, null));

    assertThat(response.id()).isEqualTo(85L);
    verify(repository).save(existing);
  }

  /** Confirma saldo estimado e capacidade sem misturar compra com custo consumido. */
  @Test
  void deveConsolidarSaldoRunwayNoModuloFinanceiro() {
    StudioProviderCreditPurchaseRepository purchases =
        mock(StudioProviderCreditPurchaseRepository.class);
    StudioCostLedgerEntryRepository ledger = mock(StudioCostLedgerEntryRepository.class);
    SalesVideoJobRepository jobs = mock(SalesVideoJobRepository.class);
    SalesVideoJobEventRepository events = mock(SalesVideoJobEventRepository.class);
    StudioProviderCreditPurchase purchase = new StudioProviderCreditPurchase();
    purchase.setProvider("RUNWAY");
    purchase.setPurchasedAt(Instant.parse("2026-08-13T12:00:00Z"));
    purchase.setCreditsPurchased(1000);
    StudioCostLedgerEntry consumption = new StudioCostLedgerEntry();
    consumption.setEstimatedCostUsd(new BigDecimal("2.00"));
    consumption.setStatus("VIDEO_READY");
    when(purchases.findDistinctProviders()).thenReturn(List.of("RUNWAY"));
    when(purchases.findByProviderFamily("RUNWAY")).thenReturn(List.of(purchase));
    when(ledger.findByProviderFamily("RUNWAY")).thenReturn(List.of(consumption));
    when(jobs.findRecentCreditFailures(
            org.mockito.ArgumentMatchers.eq("RUNWAY"), org.mockito.ArgumentMatchers.any()))
        .thenReturn(List.of());
    when(events.findAcceptedSceneEvents("RUNWAY")).thenReturn(List.of());
    when(events.findExplicitAcceptedSceneEvents("RUNWAY")).thenReturn(List.of());

    var balance =
        new ProviderCreditPurchaseService(purchases, ledger, jobs, events)
            .listVideoProviderBalances()
            .getFirst();

    assertThat(balance.status()).isEqualTo("AVAILABLE");
    assertThat(balance.estimatedConsumedCredits()).isEqualTo(200L);
    assertThat(balance.estimatedAvailableCredits()).isEqualTo(800L);
    assertThat(balance.estimatedReferenceClips()).isEqualTo(16L);
  }

  /** Confirma que a recusa real posterior à recarga prevalece sobre a estimativa. */
  @Test
  void deveSinalizarDivergenciaQuandoRunwayRecusaCredito() {
    StudioProviderCreditPurchaseRepository purchases =
        mock(StudioProviderCreditPurchaseRepository.class);
    StudioCostLedgerEntryRepository ledger = mock(StudioCostLedgerEntryRepository.class);
    SalesVideoJobRepository jobs = mock(SalesVideoJobRepository.class);
    SalesVideoJobEventRepository events = mock(SalesVideoJobEventRepository.class);
    StudioProviderCreditPurchase purchase = new StudioProviderCreditPurchase();
    purchase.setPurchasedAt(Instant.parse("2026-08-13T12:00:00Z"));
    purchase.setCreditsPurchased(20);
    SalesVideoJob failure = new SalesVideoJob();
    failure.setId(20993L);
    failure.setFinishedAt(Instant.parse("2026-08-13T16:50:00Z"));
    failure.setFailureDetail("You do not have enough credits to run this task");
    when(purchases.findDistinctProviders()).thenReturn(List.of("RUNWAY"));
    when(purchases.findByProviderFamily("RUNWAY")).thenReturn(List.of(purchase));
    when(ledger.findByProviderFamily("RUNWAY")).thenReturn(List.of());
    when(jobs.findRecentCreditFailures(
            org.mockito.ArgumentMatchers.eq("RUNWAY"), org.mockito.ArgumentMatchers.any()))
        .thenReturn(List.of(failure));
    when(events.findAcceptedSceneEvents("RUNWAY")).thenReturn(List.of());
    when(events.findExplicitAcceptedSceneEvents("RUNWAY")).thenReturn(List.of());

    var balance =
        new ProviderCreditPurchaseService(purchases, ledger, jobs, events)
            .listVideoProviderBalances()
            .getFirst();

    assertThat(balance.status()).isEqualTo("DIVERGENT_PROVIDER_REJECTION");
    assertThat(balance.lastCreditFailureJobId()).isEqualTo(20993L);
    assertThat(balance.estimatedReferenceClips()).isZero();
  }

  /** Conta uma cena uma única vez e preserva o task id explícito da Runway. */
  @Test
  void deveDeduplicarEExporSolicitacoesDeCena() {
    StudioProviderCreditPurchaseRepository purchases =
        mock(StudioProviderCreditPurchaseRepository.class);
    StudioCostLedgerEntryRepository ledger = mock(StudioCostLedgerEntryRepository.class);
    SalesVideoJobRepository jobs = mock(SalesVideoJobRepository.class);
    SalesVideoJobEventRepository events = mock(SalesVideoJobEventRepository.class);
    SalesVideoJob job = new SalesVideoJob();
    job.setId(21105L);
    job.setMetadataJson("{\"videoProductionCycleId\":6}");
    SalesVideoJobEvent legacy = new SalesVideoJobEvent();
    legacy.setJob(job);
    legacy.setMessage("Runway processando cena 1/3");
    legacy.setCreatedAt(Instant.parse("2026-08-13T18:10:35Z"));
    SalesVideoJobEvent explicit = new SalesVideoJobEvent();
    explicit.setJob(job);
    explicit.setMessage("Runway aceitou cena 1/3; taskId=task-abc");
    explicit.setDetailsJson(
        "{\"eventType\":\"PROVIDER_TASK_ACCEPTED\",\"model\":\"seedance2_5\",\"durationSeconds\":10,\"estimatedCredits\":300,\"estimatedCostUsd\":3.00}");
    explicit.setCreatedAt(Instant.parse("2026-08-13T18:10:36Z"));
    SalesVideoJobEvent settled = new SalesVideoJobEvent();
    settled.setJob(job);
    settled.setMessage("Runway liquidou cena 1/3; taskId=task-abc");
    settled.setDetailsJson(
        "{\"eventType\":\"PROVIDER_TASK_SETTLED\",\"model\":\"seedance2_5\",\"durationSeconds\":10,\"billedCredits\":300,\"billedCostUsd\":3.00,\"settlementStatus\":\"CONTRACTUAL_CHARGE\",\"settlementBasis\":\"CONTRACTUAL_RATE_CARD\",\"billingEvidence\":\"PROVIDER_RATE_CARD_AND_TASK_SUCCESS\"}");
    settled.setCreatedAt(Instant.parse("2026-08-13T18:11:00Z"));
    when(purchases.findDistinctProviders()).thenReturn(List.of());
    when(purchases.findByProviderFamily("RUNWAY")).thenReturn(List.of());
    when(ledger.findByProviderFamily("RUNWAY")).thenReturn(List.of());
    when(jobs.findRecentCreditFailures(
            org.mockito.ArgumentMatchers.eq("RUNWAY"), org.mockito.ArgumentMatchers.any()))
        .thenReturn(List.of());
    when(events.findAcceptedSceneEvents("RUNWAY")).thenReturn(List.of(legacy));
    when(events.findExplicitAcceptedSceneEvents("RUNWAY")).thenReturn(List.of(explicit));
    when(events.findSettledSceneEvents("RUNWAY")).thenReturn(List.of(settled));

    var balance =
        new ProviderCreditPurchaseService(purchases, ledger, jobs, events)
            .listVideoProviderBalances()
            .getFirst();

    assertThat(balance.acceptedSceneRequests()).isEqualTo(1);
    assertThat(balance.sceneRequests().getFirst().productionCycleId()).isEqualTo(6L);
    assertThat(balance.sceneRequests().getFirst().providerTaskId()).isEqualTo("task-abc");
    assertThat(balance.sceneRequests().getFirst().estimatedCredits()).isEqualTo(300);
    assertThat(balance.sceneRequests().getFirst().estimatedCostUsd()).isEqualByComparingTo("3.00");
    assertThat(balance.sceneRequests().getFirst().billedCredits()).isEqualTo(300);
    assertThat(balance.sceneRequests().getFirst().settlementStatus())
        .isEqualTo("CONTRACTUAL_CHARGE");
    assertThat(balance.sceneRequests().getFirst().settlementBasis())
        .isEqualTo("CONTRACTUAL_RATE_CARD");
    assertThat(balance.sceneRequests().getFirst().acceptedAt())
        .isEqualTo(Instant.parse("2026-08-13T18:10:36Z"));
  }
}

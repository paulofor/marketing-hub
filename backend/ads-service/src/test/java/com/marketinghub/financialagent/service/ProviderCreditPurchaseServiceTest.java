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
import com.marketinghub.repository.jpa.salesvideo.SalesVideoJobRepository;
import com.marketinghub.salesvideo.SalesVideoJob;
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

    var balance =
        new ProviderCreditPurchaseService(purchases, ledger, jobs)
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

    var balance =
        new ProviderCreditPurchaseService(purchases, ledger, jobs)
            .listVideoProviderBalances()
            .getFirst();

    assertThat(balance.status()).isEqualTo("DIVERGENT_PROVIDER_REJECTION");
    assertThat(balance.lastCreditFailureJobId()).isEqualTo(20993L);
    assertThat(balance.estimatedReferenceClips()).isZero();
  }
}

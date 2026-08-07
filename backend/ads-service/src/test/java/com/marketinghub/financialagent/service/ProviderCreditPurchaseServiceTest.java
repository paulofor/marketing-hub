package com.marketinghub.financialagent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.financialagent.StudioProviderCreditPurchase;
import com.marketinghub.financialagent.service.registerProviderCreditPurchase.RegisterProviderCreditPurchaseRequest;
import com.marketinghub.repository.jpa.financialagent.StudioProviderCreditPurchaseRepository;
import java.math.BigDecimal;
import java.time.Instant;
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
}

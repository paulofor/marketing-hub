package com.marketinghub.pde.paidvisual.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.marketinghub.pde.paidvisual.v1.service.contract.PaidVisualContract.*;
import com.marketinghub.pde.visualpersonalization.v1.service.contract.VisualPreparationContract.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.server.ResponseStatusException;

/**
 * Responsabilidade: comprovar pagamento, acesso, isolamento, limites e recuperação de entregas
 * reais locais.
 */
class PaidVisualServiceTest {
  private PaidVisualFixture f;
  private final Instant paidAt =
      Instant.now().minusSeconds(60).truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
  private final Instant occasionAt = Instant.now().plusSeconds(2 * 86400L);

  /** Isola cada cenário e usa apenas simuladores locais. */
  @BeforeEach
  void setup() {
    f = new PaidVisualFixture();
  }

  /** Confere o caso original e outra identidade sem tratamento especial por nome ou ID. */
  @ParameterizedTest
  @ValueSource(longs = {97, 709})
  void paymentAccessDeliveryAndRefundRemainBound(Long id) {
    var payment = payment(id, "approved");
    f.service.payment(payment);
    f.service.payment(payment);
    assertThat(f.ordersMap).hasSize(1);
    f.service.login(id, new Login("buyer@example.test"));
    String token = f.deliveredToken;
    var command = command(false);
    var result = f.service.submit(token, command);
    assertThat(result.path("deliveries").size()).isEqualTo(1);
    f.service.submit(token, command);
    assertThat(f.imagesMap).hasSize(1);
    var row = f.imagesMap.values().iterator().next();
    assertThatThrownBy(() -> f.visual.session(row.getJobId(), token))
        .isInstanceOf(ResponseStatusException.class);
    assertThat(f.execution.claim(row.getJobId()).path("status").asText()).isEqualTo("PDE_RUNNING");
    assertThat(f.service.authorizeGeneration(row)).isEqualByComparingTo("0.50");
    assertThatThrownBy(() -> f.service.submit(token, command(true)))
        .isInstanceOf(ResponseStatusException.class);
    row.setStatus("PDE_COMPLETED");
    f.service.submit(token, command(true));
    assertThat(f.imagesMap).hasSize(2);
    f.service.submit(token, command(true));
    assertThat(f.imagesMap).hasSize(2);
    assertThatThrownBy(
            () ->
                f.service.submit(
                    token,
                    new Submit(
                        "another-operation-key", true, command.occasionAt(), command.input())))
        .isInstanceOf(ResponseStatusException.class);
    f.service.payment(payment(id, "refunded"));
    f.service.payment(payment);
    assertThat(f.ordersMap.get("payment-" + id).getPaymentStatus()).isEqualTo("refunded");
    assertThatThrownBy(() -> f.service.session(token)).isInstanceOf(ResponseStatusException.class);
    assertThatThrownBy(() -> f.service.authorizeGeneration(row))
        .isInstanceOf(ResponseStatusException.class);
    verifyNoInteractions(f.ledger);
  }

  /** Bloqueia valor, versão, moeda, estado e ambiente de teste divergentes. */
  @Test
  void rejectsUnverifiedCommercialIdentity() {
    var p = payment(97L, "approved");
    for (String status : List.of("pending", "rejected", "cancelled"))
      assertThatThrownBy(() -> f.service.payment(payment(97L, status)))
          .isInstanceOf(ResponseStatusException.class);
    assertThatThrownBy(
            () ->
                f.service.payment(
                    new Payment(
                        p.paymentId(),
                        p.status(),
                        BigDecimal.ONE,
                        p.currency(),
                        p.email(),
                        p.externalReference(),
                        p.dateApproved(),
                        p.metadata())))
        .isInstanceOf(ResponseStatusException.class);
    assertThatThrownBy(
            () ->
                f.service.payment(
                    new Payment(
                        p.paymentId(),
                        p.status(),
                        p.amount(),
                        "USD",
                        p.email(),
                        p.externalReference(),
                        p.dateApproved(),
                        p.metadata())))
        .isInstanceOf(ResponseStatusException.class);
    var meta = new HashMap<>(p.metadata());
    meta.put("experienceVersion", "private-v3");
    assertThatThrownBy(
            () ->
                f.service.payment(
                    new Payment(
                        p.paymentId(),
                        p.status(),
                        p.amount(),
                        p.currency(),
                        p.email(),
                        p.externalReference(),
                        p.dateApproved(),
                        meta)))
        .isInstanceOf(ResponseStatusException.class);
    assertThatThrownBy(
            () ->
                f.service.payment(
                    new Payment(
                        p.paymentId(),
                        p.status(),
                        p.amount(),
                        p.currency(),
                        "test@sandbox.local",
                        p.externalReference(),
                        p.dateApproved(),
                        p.metadata())))
        .isInstanceOf(ResponseStatusException.class);
    assertThat(f.ordersMap).isEmpty();
  }

  /** Não enumera compradoras, limita reenvio e não libera dados com outro bearer. */
  @Test
  void accessRecoveryIsOpaqueAndBound() {
    var missing = f.service.login(97L, new Login("missing@example.test"));
    verifyNoInteractions(f.mail);
    f.service.payment(payment(97L, "approved"));
    var found = f.service.login(97L, new Login("buyer@example.test"));
    assertThat(found).isEqualTo(missing);
    String token = f.deliveredToken;
    f.service.login(97L, new Login("buyer@example.test"));
    verify(f.mail, times(1)).send(anyString(), anyString(), anyString());
    assertThatThrownBy(() -> f.service.session("invalid-token".repeat(4)))
        .isInstanceOf(ResponseStatusException.class);
    assertThatThrownBy(() -> f.service.authorizeReplay(token, "pde-visual-v1-other"))
        .isInstanceOf(ResponseStatusException.class);
    f.ordersMap.values().iterator().next().setExpiresAt(Instant.now().minusSeconds(1));
    assertThatThrownBy(() -> f.service.session(token)).isInstanceOf(ResponseStatusException.class);
  }

  /** Recusa callback que muda a compradora mesmo quando o identificador financeiro foi repetido. */
  @Test
  void callbackCannotReassignOwnership() {
    var p = payment(97L, "approved");
    f.service.payment(p);
    assertThatThrownBy(
            () ->
                f.service.payment(
                    new Payment(
                        p.paymentId(),
                        p.status(),
                        p.amount(),
                        p.currency(),
                        "another@example.test",
                        p.externalReference(),
                        p.dateApproved(),
                        p.metadata())))
        .isInstanceOf(ResponseStatusException.class);
    assertThat(f.ordersMap.get(p.paymentId()).getBuyerEmail()).isEqualTo("buyer@example.test");
  }

  /** Cria confirmação com datas estáveis para comparar callbacks duplicados e reembolsos. */
  private Payment payment(Long id, String status) {
    return new Payment(
        "payment-" + id,
        status,
        new BigDecimal("79.00"),
        "BRL",
        "buyer@example.test",
        "produto-" + id,
        paidAt,
        Map.of(
            "productId",
            id + 100,
            "experimentId",
            id,
            "deliveryContract",
            PaidVisualPolicy.VERSION,
            "experienceVersion",
            "commercial-v1",
            "providerLiveMode",
            true));
  }

  /** Cria entrada limitada e idempotente para uma ocasião posterior. */
  private Submit command(boolean regeneration) {
    return new Submit(
        regeneration ? "regeneration-operation" : "initial-operation-key",
        regeneration,
        occasionAt,
        new Input(
            "Jantar informal",
            List.of("camisa branca", "calça preta", "tênis branco"),
            "Conforto",
            "Sem salto"));
  }
}

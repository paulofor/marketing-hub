package com.marketinghub.pde.paidvisual.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.marketinghub.pde.paidvisual.v1.controller.PaidVisualController;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Responsabilidade: validar a fronteira HTTP financeira e impedir liberações pelo navegador
 * público.
 */
class PaidVisualControllerTest {
  /** Exige segredo interno, aceita o DTO real de pagamentos e nunca devolve e-mail ou token. */
  @Test
  void verifiedNotificationMatchesRealPaymentsContract() throws Exception {
    var f = new PaidVisualFixture();
    var mvc =
        MockMvcBuilders.standaloneSetup(
                new PaidVisualController(f.service, f.execution, "local-payments"))
            .build();
    String body =
        """
        {"paymentId":"mp-1","paymentStatus":"approved","buyerEmail":"buyer@example.test","amount":79,"currency":"BRL",
        "externalReference":"produto-97","dateApproved":"%s","metadata":{"productId":197,"experimentId":97,"deliveryContract":"PDE_PAID_VISUAL_V1","experienceVersion":"commercial-v1","providerLiveMode":true}}
        """
            .formatted(Instant.now().minusSeconds(60));
    mvc.perform(
            post("/api/pde/paid-visual/v1/internal/payments")
                .contentType("application/json")
                .content(body))
        .andExpect(status().isUnauthorized());
    assertThat(f.ordersMap).isEmpty();
    mvc.perform(
            post("/api/pde/paid-visual/v1/internal/payments")
                .header("X-PDE-Internal-Token", "local-payments")
                .contentType("application/json")
                .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("approved"))
        .andExpect(header().string("Cache-Control", "private, no-store"));
    assertThat(f.ordersMap).hasSize(1);
    mvc.perform(
            get("/api/pde/paid-visual/v1/session").header("X-PDE-Access-Token", "random".repeat(8)))
        .andExpect(status().isUnauthorized());
  }
}

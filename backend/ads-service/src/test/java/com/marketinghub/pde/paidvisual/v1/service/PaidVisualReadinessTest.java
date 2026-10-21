package com.marketinghub.pde.paidvisual.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.pde.PdeProductionSlot;
import java.net.http.*;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Responsabilidade: impedir homologação da área privada ou de outra entrega como versão paga. */
class PaidVisualReadinessTest {
  /** Confere duas identidades e bloqueia divergência, falta de acesso e condições incompletas. */
  @Test
  @SuppressWarnings("unchecked")
  void checksActualPaidSurfaceWithoutRequiringLegacySampleRoutes() throws Exception {
    for (long id : List.of(97L, 709L)) {
      var json = new ObjectMapper();
      var client = mock(HttpClient.class);
      var offer =
          json.createObjectNode()
              .put("contractVersion", PaidVisualPolicy.VERSION)
              .put("productSlug", "product-" + id)
              .put("productVersion", "paid-v1")
              .put("experimentId", id)
              .put("accessDays", 7)
              .put("maxRegenerations", 1)
              .put("accessDeliveryReady", true);
      var slot =
          PdeProductionSlot.builder()
              .productSlug("product-" + id)
              .experienceVersion("paid-v1")
              .sourceExperimentId(id)
              .publicUrl("https://example.test/paid-visual.html?experiment=" + id)
              .build();
      when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
          .thenAnswer(
              invocation -> {
                var request = invocation.getArgument(0, HttpRequest.class);
                HttpResponse<String> response = mock(HttpResponse.class);
                when(response.statusCode()).thenReturn(200);
                String path = request.uri().getPath();
                when(response.body())
                    .thenReturn(
                        path.endsWith("/offer")
                            ? offer.toString()
                            : path.equals("/healthz")
                                ? "{\"status\":\"UP\"}"
                                : "<meta content=\"PDE_PAID_VISUAL_V1\"><script type=\"module\"></script>");
                return response;
              });
      var service = new PaidVisualReadiness(client, json);
      assertThat(service.failure(slot, false)).isNull();
      assertThat(service.failure(slot, true)).contains("Checkout");
      offer
          .put("checkoutPrepared", true)
          .put("supportEmail", "support@example.test")
          .put("termsUrl", "https://example.test/terms")
          .put("privacyUrl", "https://example.test/privacy")
          .put("refundPolicyUrl", "https://example.test/refund");
      assertThat(service.failure(slot, true)).isNull();
      offer.put("accessDeliveryReady", false);
      assertThat(service.failure(slot, false)).contains("e-mail");
      offer.put("accessDeliveryReady", true).put("productVersion", "another-version");
      assertThat(service.failure(slot, false)).contains("versão");
      slot.setPublicUrl("https://example.test");
      assertThat(service.failure(slot, false)).contains("área de entrega");
    }
  }
}

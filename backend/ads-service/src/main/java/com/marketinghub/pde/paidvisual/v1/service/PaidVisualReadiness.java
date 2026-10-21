package com.marketinghub.pde.paidvisual.v1.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.pde.PdeProductionSlot;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.List;

/**
 * Responsabilidade: conferir a superfície paga publicada sem confundi-la com a jornada de amostra.
 */
public final class PaidVisualReadiness {
  private final HttpClient client;
  private final ObjectMapper json;

  /** Reutiliza o transporte limitado do validador de versões. */
  public PaidVisualReadiness(HttpClient client, ObjectMapper json) {
    this.client = client;
    this.json = json;
  }

  /**
   * Confere identidade, acesso e entrada; a conclusão comercial exige suporte, políticas e
   * checkout.
   */
  public String failure(PdeProductionSlot slot, boolean complete)
      throws IOException, InterruptedException {
    URI page = URI.create(slot.getPublicUrl());
    if (!page.getPath().endsWith("/paid-visual.html"))
      return "A versão paga precisa apontar para sua área de entrega.";
    var health = get(page.resolve("/healthz"));
    if (health.statusCode() != 200 || !health.body().contains("UP"))
      return "A superfície paga não respondeu saudável.";
    var response =
        get(
            page.resolve(
                "/api/pde/paid-visual/v1/experiments/" + slot.getSourceExperimentId() + "/offer"));
    if (response.statusCode() != 200)
      return "O contrato pago público não respondeu: HTTP " + response.statusCode();
    var offer = json.readTree(response.body());
    if (!PaidVisualPolicy.VERSION.equals(offer.path("contractVersion").asText())
        || !slot.getProductSlug().equals(offer.path("productSlug").asText())
        || !slot.getExperienceVersion().equals(offer.path("productVersion").asText())
        || offer.path("experimentId").asLong() != slot.getSourceExperimentId()
        || offer.path("accessDays").asInt() != 7
        || offer.path("maxRegenerations").asInt() != 1)
      return "A entrega publicada não corresponde ao produto, versão ou limites do slot.";
    if (!offer.path("accessDeliveryReady").asBoolean())
      return "O envio do acesso por e-mail ainda não está configurado para entrega.";
    var entry = get(page);
    if (entry.statusCode() != 200
        || !entry.body().contains("PDE_PAID_VISUAL_V1")
        || !entry.body().contains("type=\"module\""))
      return "A página não entrega a aplicação paga esperada.";
    if (complete) {
      if (!offer.path("checkoutPrepared").asBoolean()
          || !offer.path("supportEmail").asText().contains("@"))
        return "Checkout e canal de suporte ainda precisam ser preparados e revisados.";
      for (String key : List.of("termsUrl", "privacyUrl", "refundPolicyUrl")) {
        String url = offer.path(key).asText();
        if (!url.startsWith("https://") || get(URI.create(url)).statusCode() != 200)
          return "Política comercial indisponível: " + key;
      }
    }
    return null;
  }

  /** Consulta somente contratos e páginas, sem pagamento, login, geração ou mutação. */
  private HttpResponse<String> get(URI uri) throws IOException, InterruptedException {
    return client.send(
        HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(12)).GET().build(),
        HttpResponse.BodyHandlers.ofString());
  }
}

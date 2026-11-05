package com.marketinghub.salesvideo.service.clipplan;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;

/** Responsabilidade: validar contrato HTTP real de planejamento sem acesso a produção ou render. */
@org.springframework.test.context.ActiveProfiles("studio-local")
@SpringBootTest(
    classes = StudioLocalApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StudioHttpContractTest {
  @Autowired private TestRestTemplate http;

  /** Confere catálogo novo sem ativação, duração integral, custo e validação antes de gerar. */
  @Test
  void exposesGovernedCatalogAndPreviewsWithoutGeneration() {
    var catalog = http.getForObject("/api/sales-videos/provider-models", JsonNode.class);
    assertThat(catalog.toString()).contains("kling_4", "seedance2_5", "aleph2", "2026-11-05.v1");
    assertThat(catalog.findValuesAsText("lifecycleStatus")).contains("DRAFT");
    var response =
        http.postForEntity(
            "/api/sales-videos/studio/clip-plan",
            new StudioClipPlanRequest("RUNWAY", 32, "720p", false, 0, 0),
            JsonNode.class);
    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody().path("finalSeconds").asInt()).isEqualTo(32);
    assertThat(response.getBody().path("scenes")).hasSize(4);
    assertThat(response.getBody().path("productionAllowed").asBoolean()).isFalse();
    var invalid =
        http.postForEntity(
            "/api/sales-videos/studio/clip-plan",
            new StudioClipPlanRequest("", 0, "720p", false, 0, 0),
            JsonNode.class);
    assertThat(invalid.getStatusCode().value()).isEqualTo(400);
    var missing =
        http.postForEntity(
            "/api/sales-videos/studio/clip-plan",
            new StudioClipPlanRequest("UNKNOWN", 32, "720p", false, 0, 0),
            JsonNode.class);
    assertThat(missing.getStatusCode().value()).isEqualTo(400);
    assertThat(missing.getBody().path("message").asText()).contains("contrato compartilhado");
  }
}

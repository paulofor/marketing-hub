package com.marketinghub.pde.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.monitoring.dto.PostDeployPdeProductionSlotRequestDto;
import com.marketinghub.pde.PdeProductionSlot;
import com.marketinghub.pde.PdeProductionSlotStatus;
import com.marketinghub.repository.jpa.pde.PdeProductionSlotRepository;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Responsabilidade: validar regras de cadastro de versões produtivas PDE por produto. */
@ExtendWith(MockitoExtension.class)
class PdeProductionSlotServiceTest {

  @Mock private PdeProductionSlotRepository repository;

  @Mock private HttpClient httpClient;

  /** Deve normalizar domínio e URL ao salvar uma versão PDE do produto. */
  @Test
  void savesProductPdeProductionSlotWithNormalizedDomain() {
    PdeProductionSlotService service =
        new PdeProductionSlotService(repository, httpClient, new ObjectMapper());
    when(repository.findByProductSlugAndSlotCode("metodo-musa-7-dias", "v2"))
        .thenReturn(Optional.empty());
    when(repository.save(org.mockito.ArgumentMatchers.any(PdeProductionSlot.class)))
        .thenAnswer(
            invocation -> {
              PdeProductionSlot slot = invocation.getArgument(0);
              slot.setId(2L);
              slot.setCreatedAt(Instant.parse("2026-07-24T10:00:00Z"));
              slot.setUpdatedAt(Instant.parse("2026-07-24T10:00:00Z"));
              return slot;
            });

    var response =
        service.saveProductionSlot(
            "metodo-musa-7-dias",
            71L,
            new PostDeployPdeProductionSlotRequestDto(
                "v2",
                null,
                "https://v2.clubemusa.com.br/",
                null,
                null,
                "musa-pde-entry-v5-estrada-desejo",
                null,
                PdeProductionSlotStatus.PLANNED,
                null,
                "Hipotese 2"));

    assertThat(response.id()).isEqualTo(2L);
    assertThat(response.productSlug()).isEqualTo("metodo-musa-7-dias");
    assertThat(response.domain()).isEqualTo("v2.clubemusa.com.br");
    assertThat(response.publicUrl()).isEqualTo("https://v2.clubemusa.com.br");
    assertThat(response.targetEnvironment()).isEqualTo("production-v2");
    assertThat(response.sourceExperimentId()).isEqualTo(71L);
  }

  /** Deve reprovar slot produtivo quando o health público não confirma a aplicação. */
  @Test
  void recordsFailedValidationWhenPublicHealthDoesNotRespondUp() throws Exception {
    PdeProductionSlot slot =
        PdeProductionSlot.builder()
            .id(2L)
            .slotCode("v2")
            .productSlug("metodo-musa-7-dias")
            .domain("v2.clubemusa.com.br")
            .publicUrl("https://v2.clubemusa.com.br")
            .experienceVersion("musa-pde-entry-v5-estrada-desejo")
            .targetEnvironment("production-v2")
            .status(PdeProductionSlotStatus.PLANNED)
            .createdAt(Instant.parse("2026-07-24T10:00:00Z"))
            .updatedAt(Instant.parse("2026-07-24T10:00:00Z"))
            .build();
    PdeProductionSlotService service =
        new PdeProductionSlotService(repository, httpClient, new ObjectMapper());
    when(repository.findByProductSlugAndSlotCode("metodo-musa-7-dias", "v2"))
        .thenReturn(Optional.of(slot));
    HttpResponse<String> healthResponse = response(404);
    when(httpClient.send(
            org.mockito.ArgumentMatchers.any(HttpRequest.class),
            org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
        .thenReturn(healthResponse);
    when(repository.save(org.mockito.ArgumentMatchers.any(PdeProductionSlot.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var response = service.validateProductionSlot("metodo-musa-7-dias", "v2");

    assertThat(response.validationStatus()).isEqualTo("FAILED");
    assertThat(response.validationHttpStatus()).isEqualTo(404);
    assertThat(response.validationSummary()).isEqualTo("Health público não respondeu como UP");
  }

  /** Deve reprovar versão com vídeo quando o manifesto HLS público entrega HTML por fallback. */
  @Test
  void recordsFailedValidationWhenVersionedHlsManifestIsHtmlFallback() throws Exception {
    PdeProductionSlot slot =
        PdeProductionSlot.builder()
            .id(6L)
            .slotCode("v6")
            .productSlug("metodo-musa-7-dias")
            .domain("v6.clubemusa.com.br")
            .publicUrl("https://v6.clubemusa.com.br")
            .experienceVersion("musa-pde-entry-v6-video-motivacional")
            .targetEnvironment("production-v6")
            .status(PdeProductionSlotStatus.PLANNED)
            .createdAt(Instant.parse("2026-07-24T10:00:00Z"))
            .updatedAt(Instant.parse("2026-07-24T10:00:00Z"))
            .build();
    PdeProductionSlotService service =
        new PdeProductionSlotService(repository, httpClient, new ObjectMapper());
    when(repository.findByProductSlugAndSlotCode("metodo-musa-7-dias", "v6"))
        .thenReturn(Optional.of(slot));
    HttpResponse<String> healthResponse = response(200, "{\"status\":\"UP\"}");
    HttpResponse<String> contractResponse =
        response(
            200,
            "{\"slug\":\"metodo-musa-7-dias\",\"healthPath\":\"/\",\"requiredTexts\":[\"CTA\"]}");
    HttpResponse<String> pageResponse =
        response(200, "<html><body>CTA<script type=\"module\"></script></body></html>");
    HttpResponse<Void> manifestHeadResponse = response(200, headers("text/html", "510"));
    org.mockito.Mockito.doReturn(healthResponse)
        .doReturn(contractResponse)
        .doReturn(pageResponse)
        .doReturn(manifestHeadResponse)
        .when(httpClient)
        .send(
            org.mockito.ArgumentMatchers.any(HttpRequest.class),
            org.mockito.ArgumentMatchers.any());
    when(repository.save(org.mockito.ArgumentMatchers.any(PdeProductionSlot.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var response = service.validateProductionSlot("metodo-musa-7-dias", "v6");

    assertThat(response.validationStatus()).isEqualTo("FAILED");
    assertThat(response.validationSummary())
        .isEqualTo("HLS obrigatório da versão PDE não foi entregue");
    assertThat(response.validationDetail())
        .contains("Content-Type do manifesto recebido: text/html");
  }

  /** Deve aprovar versão quando o manifesto HLS e o primeiro segmento respondem corretamente. */
  @Test
  void recordsOkValidationWhenVersionedHlsStreamIsAvailable() throws Exception {
    PdeProductionSlot slot =
        PdeProductionSlot.builder()
            .id(7L)
            .slotCode("v6")
            .productSlug("metodo-musa-7-dias")
            .domain("v6.clubemusa.com.br")
            .publicUrl("https://v6.clubemusa.com.br")
            .experienceVersion("musa-pde-entry-v6-video-motivacional")
            .targetEnvironment("production-v6")
            .status(PdeProductionSlotStatus.ACTIVE)
            .createdAt(Instant.parse("2026-07-24T10:00:00Z"))
            .updatedAt(Instant.parse("2026-07-24T10:00:00Z"))
            .build();
    PdeProductionSlotService service =
        new PdeProductionSlotService(repository, httpClient, new ObjectMapper());
    when(repository.findByProductSlugAndSlotCode("metodo-musa-7-dias", "v6"))
        .thenReturn(Optional.of(slot));
    HttpResponse<String> healthResponse = response(200, "{\"status\":\"UP\"}");
    HttpResponse<String> contractResponse =
        response(
            200,
            "{\"slug\":\"metodo-musa-7-dias\",\"healthPath\":\"/\",\"requiredTexts\":[\"CTA\"],\"forbiddenTexts\":[\"Application error\"]}");
    HttpResponse<String> pageResponse =
        response(200, "<html><body>CTA<script type=\"module\"></script></body></html>");
    HttpResponse<Void> manifestHeadResponse =
        response(200, headers("application/vnd.apple.mpegurl", "180"));
    HttpResponse<String> manifestResponse =
        response(200, "#EXTM3U\n#EXTINF:4.000000,\nsegment-000.ts\n#EXT-X-ENDLIST\n");
    HttpResponse<Void> segmentHeadResponse = response(200, headers("video/mp2t", "204800"));
    org.mockito.Mockito.doReturn(healthResponse)
        .doReturn(contractResponse)
        .doReturn(pageResponse)
        .doReturn(manifestHeadResponse)
        .doReturn(manifestResponse)
        .doReturn(segmentHeadResponse)
        .when(httpClient)
        .send(
            org.mockito.ArgumentMatchers.any(HttpRequest.class),
            org.mockito.ArgumentMatchers.any());
    when(repository.save(org.mockito.ArgumentMatchers.any(PdeProductionSlot.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var response = service.validateProductionSlot("metodo-musa-7-dias", "v6");

    assertThat(response.validationStatus()).isEqualTo("OK");
    assertThat(response.validationSummary()).isEqualTo("URL produtiva validada");
  }

  /** Cria resposta HTTP simulada para validação de contrato do slot. */
  @SuppressWarnings("unchecked")
  private HttpResponse<String> response(int statusCode) {
    HttpResponse<String> response = org.mockito.Mockito.mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(statusCode);
    return response;
  }

  /** Cria resposta HTTP textual simulada para validações com corpo. */
  @SuppressWarnings("unchecked")
  private HttpResponse<String> response(int statusCode, String body) {
    HttpResponse<String> response = org.mockito.Mockito.mock(HttpResponse.class);
    org.mockito.Mockito.lenient().when(response.statusCode()).thenReturn(statusCode);
    org.mockito.Mockito.lenient().when(response.body()).thenReturn(body);
    return response;
  }

  /** Cria resposta HTTP sem corpo simulada para validações de ativo. */
  @SuppressWarnings("unchecked")
  private HttpResponse<Void> response(int statusCode, HttpHeaders headers) {
    HttpResponse<Void> response = org.mockito.Mockito.mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(statusCode);
    when(response.headers()).thenReturn(headers);
    return response;
  }

  /** Cria cabeçalhos HTTP simulados para validar ativos versionados. */
  private HttpHeaders headers(String contentType, String contentLength) {
    return HttpHeaders.of(
        Map.of("content-type", List.of(contentType), "content-length", List.of(contentLength)),
        (name, value) -> true);
  }
}

package com.marketinghub.pde.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.monitoring.dto.PostDeployPdeProductionSlotRequestDto;
import com.marketinghub.experiment.video.ExperimentVideoAsset;
import com.marketinghub.experiment.video.ExperimentVideoReviewStatus;
import com.marketinghub.experiment.video.ExperimentVideoSlot;
import com.marketinghub.experiment.video.ExperimentVideoStatus;
import com.marketinghub.pde.PdeProductionSlot;
import com.marketinghub.pde.PdeProductionSlotStatus;
import com.marketinghub.repository.jpa.experiment.video.ExperimentVideoAssetRepository;
import com.marketinghub.repository.jpa.pde.PdeProductionSlotRepository;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Responsabilidade: validar regras de cadastro de versões produtivas PDE por produto. */
@ExtendWith(MockitoExtension.class)
class PdeProductionSlotServiceTest {

  @Mock private PdeProductionSlotRepository repository;

  @Mock private ExperimentVideoAssetRepository videoAssetRepository;

  @Mock private HttpClient httpClient;

  /** Deve normalizar domínio e URL ao salvar uma versão PDE do produto. */
  @Test
  void savesProductPdeProductionSlotWithNormalizedDomain() {
    PdeProductionSlotService service =
        new PdeProductionSlotService(
            repository, videoAssetRepository, httpClient, new ObjectMapper());
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
                "Hipotese 2",
                null,
                null));

    assertThat(response.id()).isEqualTo(2L);
    assertThat(response.productSlug()).isEqualTo("metodo-musa-7-dias");
    assertThat(response.domain()).isEqualTo("v2.clubemusa.com.br");
    assertThat(response.publicUrl()).isEqualTo("https://v2.clubemusa.com.br");
    assertThat(response.targetEnvironment()).isEqualTo("production-v2");
    assertThat(response.sourceExperimentId()).isEqualTo(71L);
  }

  /** Deve resolver vídeo HLS pelo token de versão antes do experimento de origem. */
  @Test
  void listsPdeVideosByVersionTokenBeforeSourceExperiment() {
    PdeProductionSlot v2 =
        PdeProductionSlot.builder()
            .id(2L)
            .slotCode("v2")
            .productSlug("metodo-musa-7-dias")
            .domain("v2.clubemusa.com.br")
            .publicUrl("https://v2.clubemusa.com.br")
            .experienceVersion("musa-pde-entry-v5-estrada-desejo")
            .targetEnvironment("production-v2")
            .status(PdeProductionSlotStatus.PAUSED)
            .sourceExperimentId(68L)
            .build();
    PdeProductionSlot v6 =
        PdeProductionSlot.builder()
            .id(4L)
            .slotCode("v6")
            .productSlug("metodo-musa-7-dias")
            .domain("v6.clubemusa.com.br")
            .publicUrl("https://v6.clubemusa.com.br")
            .experienceVersion("musa-pde-entry-v6-video-motivacional")
            .targetEnvironment("production-v6")
            .status(PdeProductionSlotStatus.ACTIVE)
            .sourceExperimentId(76L)
            .build();
    ExperimentVideoAsset video =
        ExperimentVideoAsset.builder()
            .id(23L)
            .experiment(Experiment.builder().id(68L).build())
            .slot(ExperimentVideoSlot.LANDING_HERO)
            .objective("Microexperiência visível")
            .primaryMetric("DIAGNOSTIC_STARTED")
            .provider("HEYGEN")
            .model("avatar")
            .status(ExperimentVideoStatus.READY)
            .reviewStatus(ExperimentVideoReviewStatus.APPROVED)
            .hlsPlaybackUrl("/assets/hls/musa-v6-microexperiencia-visivel/index.m3u8")
            .requiredForRelease(true)
            .build();
    PdeProductionSlotService service =
        new PdeProductionSlotService(
            repository, videoAssetRepository, httpClient, new ObjectMapper());
    when(repository.findByProductSlugOrderBySlotCodeAsc("metodo-musa-7-dias"))
        .thenReturn(List.of(v2, v6));
    when(videoAssetRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(video));

    var response = service.listProductionSlotVideosForProduct("metodo-musa-7-dias");

    assertThat(response).hasSize(2);
    assertThat(response.get(0).slot().slotCode()).isEqualTo("v2");
    assertThat(response.get(0).videos()).isEmpty();
    assertThat(response.get(1).slot().slotCode()).isEqualTo("v6");
    assertThat(response.get(1).videos()).extracting("id").containsExactly(23L);
    assertThat(response.get(1).videos())
        .extracting("assignmentSource")
        .containsExactly("VERSION_TOKEN");
    assertThat(response.get(1).alerts()).hasSize(1);
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
        new PdeProductionSlotService(
            repository, videoAssetRepository, httpClient, new ObjectMapper());
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

  /** Deve aprovar v6 sem exigir HLS quando não há vídeo comercial aprovado para a versão. */
  @Test
  void recordsOkValidationForMusaV6WithoutSlideGeneratedHlsRequirement() throws Exception {
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
        new PdeProductionSlotService(
            repository, videoAssetRepository, httpClient, new ObjectMapper());
    when(repository.findByProductSlugAndSlotCode("metodo-musa-7-dias", "v6"))
        .thenReturn(Optional.of(slot));
    HttpResponse<String> healthResponse = response(200, "{\"status\":\"UP\"}");
    HttpResponse<String> contractResponse =
        response(
            200,
            "{\"slug\":\"metodo-musa-7-dias\",\"healthPath\":\"/\",\"requiredTexts\":[\"CTA\"]}");
    HttpResponse<String> pageResponse =
        response(200, "<html><body>CTA<script type=\"module\"></script></body></html>");
    org.mockito.Mockito.doReturn(healthResponse)
        .doReturn(contractResponse)
        .doReturn(pageResponse)
        .when(httpClient)
        .send(
            org.mockito.ArgumentMatchers.any(HttpRequest.class),
            org.mockito.ArgumentMatchers.any());
    when(repository.save(org.mockito.ArgumentMatchers.any(PdeProductionSlot.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var response = service.validateProductionSlot("metodo-musa-7-dias", "v6");

    assertThat(response.validationStatus()).isEqualTo("OK");
    assertThat(response.validationSummary()).isEqualTo("URL produtiva validada");
    org.mockito.Mockito.verify(httpClient, org.mockito.Mockito.times(3))
        .send(
            org.mockito.ArgumentMatchers.any(HttpRequest.class),
            org.mockito.ArgumentMatchers.any());
  }

  /** Deve aprovar copy pública renderizada por bundle JavaScript em entrada SPA. */
  @Test
  void recordsOkValidationWhenRequiredCopyIsInsideSpaScriptBundle() throws Exception {
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
        new PdeProductionSlotService(
            repository, videoAssetRepository, httpClient, new ObjectMapper());
    when(repository.findByProductSlugAndSlotCode("metodo-musa-7-dias", "v6"))
        .thenReturn(Optional.of(slot));
    HttpResponse<String> healthResponse = response(200, "{\"status\":\"UP\"}");
    HttpResponse<String> contractResponse =
        response(
            200,
            "{\"slug\":\"metodo-musa-7-dias\",\"healthPath\":\"/\",\"requiredTexts\":[\"Descubra o detalhe\"],\"forbiddenTexts\":[\"Application error\"]}");
    HttpResponse<String> pageResponse =
        response(
            200,
            "<html><body><div id=\"root\"></div><script type=\"module\" src=\"/assets/index.js\"></script></body></html>");
    HttpResponse<String> scriptResponse = response(200, "const title = 'Descubra o detalhe';");
    org.mockito.Mockito.doReturn(healthResponse)
        .doReturn(contractResponse)
        .doReturn(pageResponse)
        .doReturn(scriptResponse)
        .when(httpClient)
        .send(
            org.mockito.ArgumentMatchers.any(HttpRequest.class),
            org.mockito.ArgumentMatchers.any());
    when(repository.save(org.mockito.ArgumentMatchers.any(PdeProductionSlot.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var response = service.validateProductionSlot("metodo-musa-7-dias", "v6");

    assertThat(response.validationStatus()).isEqualTo("OK");
    assertThat(response.validationSummary()).isEqualTo("URL produtiva validada");
    org.mockito.Mockito.verify(httpClient, org.mockito.Mockito.times(4))
        .send(
            org.mockito.ArgumentMatchers.any(HttpRequest.class),
            org.mockito.ArgumentMatchers.any());
  }

  /** Deve ignorar termos técnicos internos do bundle que não aparecem no HTML público. */
  @Test
  void recordsOkValidationWhenForbiddenTechnicalTextExistsOnlyInsideSpaScriptBundle()
      throws Exception {
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
        new PdeProductionSlotService(
            repository, videoAssetRepository, httpClient, new ObjectMapper());
    when(repository.findByProductSlugAndSlotCode("metodo-musa-7-dias", "v6"))
        .thenReturn(Optional.of(slot));
    HttpResponse<String> healthResponse = response(200, "{\"status\":\"UP\"}");
    HttpResponse<String> contractResponse =
        response(
            200,
            "{\"slug\":\"metodo-musa-7-dias\",\"healthPath\":\"/\",\"requiredTexts\":[\"Descubra o detalhe\"],\"forbiddenTexts\":[\"schema\",\"Application error\"]}");
    HttpResponse<String> pageResponse =
        response(
            200,
            "<html><body><div id=\"root\"></div><script type=\"module\" src=\"/assets/index.js\"></script></body></html>");
    HttpResponse<String> scriptResponse =
        response(200, "const title = 'Descubra o detalhe'; const schema = {};");
    org.mockito.Mockito.doReturn(healthResponse)
        .doReturn(contractResponse)
        .doReturn(pageResponse)
        .doReturn(scriptResponse)
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
}

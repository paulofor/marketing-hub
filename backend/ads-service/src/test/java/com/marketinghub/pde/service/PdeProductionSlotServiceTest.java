package com.marketinghub.pde.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import com.marketinghub.pde.service.publishslotcontract.PublishPdeProductionSlotContractRequest;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
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
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: validar regras de cadastro de versões produtivas PDE por produto. */
@ExtendWith(MockitoExtension.class)
class PdeProductionSlotServiceTest {

  @Mock private PdeProductionSlotRepository repository;

  @Mock private ExperimentVideoAssetRepository videoAssetRepository;

  @Mock private ExperimentRepository experimentRepository;

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
                "estrada-desejo",
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
    assertThat(response.layoutKey()).isEqualTo("estrada-desejo");
    assertThat(response.targetEnvironment()).isEqualTo("production-v2");
    assertThat(response.sourceExperimentId()).isEqualTo(71L);
  }

  /** Deve aceitar domínio corporativo neutro para um PDE que não seja o MUSA. */
  @Test
  void acceptsCorporateDomainForNonMusaProduct() {
    PdeProductionSlotService service =
        new PdeProductionSlotService(
            repository, videoAssetRepository, httpClient, new ObjectMapper());
    when(repository.findByProductSlugAndSlotCode("kit-whatsapp-pronto", "v1"))
        .thenReturn(Optional.empty());
    when(repository.save(org.mockito.ArgumentMatchers.any(PdeProductionSlot.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var response =
        service.saveProductionSlot(
            "kit-whatsapp-pronto",
            89L,
            new PostDeployPdeProductionSlotRequestDto(
                "v1",
                null,
                "kit-whatsapp-pronto.digicomdigital.com.br",
                null,
                null,
                "kit-whatsapp-pronto-pde-v1",
                "assisted-service-v1",
                null,
                PdeProductionSlotStatus.PLANNED,
                null,
                "Entrega assistida em 48 horas",
                null,
                null));

    assertThat(response.domain()).isEqualTo("kit-whatsapp-pronto.digicomdigital.com.br");
    assertThat(response.publicUrl()).isEqualTo("https://kit-whatsapp-pronto.digicomdigital.com.br");
  }

  /** Deve publicar contrato preenchendo a identidade independente de versão e layout do slot. */
  @Test
  void publishesSlotContractWithVersionAndLayoutIdentity() throws Exception {
    PdeProductionSlot slot =
        PdeProductionSlot.builder()
            .id(6L)
            .slotCode("v6")
            .productSlug("metodo-musa-7-dias")
            .domain("v6.clubemusa.com.br")
            .publicUrl("https://v6.clubemusa.com.br")
            .experienceVersion("musa-v6-teste-publicado")
            .layoutKey("layout-custom-v6")
            .targetEnvironment("production-v6")
            .status(PdeProductionSlotStatus.ACTIVE)
            .createdAt(Instant.parse("2026-07-30T10:00:00Z"))
            .updatedAt(Instant.parse("2026-07-30T10:00:00Z"))
            .build();
    ObjectMapper mapper = new ObjectMapper();
    PdeProductionSlotService service =
        new PdeProductionSlotService(repository, videoAssetRepository, httpClient, mapper);
    when(repository.findByProductSlugAndSlotCode("metodo-musa-7-dias", "v6"))
        .thenReturn(Optional.of(slot));
    when(repository.save(org.mockito.ArgumentMatchers.any(PdeProductionSlot.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var response =
        service.publishProductionSlotContract(
            "metodo-musa-7-dias",
            "v6",
            new PublishPdeProductionSlotContractRequest(
                "{\"slug\":\"metodo-musa-7-dias\",\"name\":\"MUSA v6\"}", "Marketing Hub"));

    var published = mapper.readTree(response.publishedExperienceJson());
    assertThat(response.layoutKey()).isEqualTo("layout-custom-v6");
    assertThat(published.get("experienceVersion").asText()).isEqualTo("musa-v6-teste-publicado");
    assertThat(published.get("layoutKey").asText()).isEqualTo("layout-custom-v6");
  }

  /** Deve bloquear publicação da v12 enquanto ela for candidata ou usar vínculos incompletos. */
  @Test
  void blocksV12PublicationBeforeHomologation() {
    PdeProductionSlot slot = v12Slot(PdeProductionSlotStatus.CANDIDATE);
    PdeProductionSlotService service =
        new PdeProductionSlotService(
            repository, videoAssetRepository, experimentRepository, httpClient, new ObjectMapper());
    when(repository.findByProductSlugAndSlotCode("metodo-musa-7-dias", "v8"))
        .thenReturn(Optional.of(slot));
    when(experimentRepository.findById(92L)).thenReturn(Optional.of(v12Experiment()));
    when(videoAssetRepository.findByExperimentIdOrderByCreatedAtDesc(92L)).thenReturn(v12Videos());

    assertThatThrownBy(
            () ->
                service.publishProductionSlotContract(
                    "metodo-musa-7-dias",
                    "v8",
                    new PublishPdeProductionSlotContractRequest(v12Contract(), "Marketing Hub")))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Concluir a homologação comercial da v12");
  }

  /** Deve publicar somente o snapshot v12 que coincide com oferta, vídeos, kit e destino. */
  @Test
  void publishesHomologatedV12WithExactCommercialBindings() {
    PdeProductionSlot slot = v12Slot(PdeProductionSlotStatus.READY);
    PdeProductionSlotService service =
        new PdeProductionSlotService(
            repository, videoAssetRepository, experimentRepository, httpClient, new ObjectMapper());
    when(repository.findByProductSlugAndSlotCode("metodo-musa-7-dias", "v8"))
        .thenReturn(Optional.of(slot));
    when(experimentRepository.findById(92L)).thenReturn(Optional.of(v12Experiment()));
    when(videoAssetRepository.findByExperimentIdOrderByCreatedAtDesc(92L)).thenReturn(v12Videos());
    when(repository.save(org.mockito.ArgumentMatchers.any(PdeProductionSlot.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var result =
        service.publishProductionSlotContract(
            "metodo-musa-7-dias",
            "v8",
            new PublishPdeProductionSlotContractRequest(v12Contract(), "Marketing Hub"));

    assertThat(result.publishedExperienceJson()).contains("/materials/musa-v12/");
    assertThat(result.publishedExperienceJson()).doesNotContain("/materials/musa-v7/");
    assertThat(result.publishedBy()).isEqualTo("Marketing Hub");
  }

  /** Deve concluir a preparação sem publicar o contrato ou ativar a campanha. */
  @Test
  void preparesAlignedV12ForPublicationWithoutActivatingIt() {
    PdeProductionSlot slot = v12Slot(PdeProductionSlotStatus.CANDIDATE);
    PdeProductionSlotService service =
        new PdeProductionSlotService(
            repository, videoAssetRepository, experimentRepository, httpClient, new ObjectMapper());
    when(repository.findByProductSlugAndSlotCode("metodo-musa-7-dias", "v8"))
        .thenReturn(Optional.of(slot));
    when(experimentRepository.findById(92L)).thenReturn(Optional.of(v12Experiment()));
    when(videoAssetRepository.findByExperimentIdOrderByCreatedAtDesc(92L)).thenReturn(v12Videos());
    when(repository.save(org.mockito.ArgumentMatchers.any(PdeProductionSlot.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var result = service.prepareProductionSlotForPublication("metodo-musa-7-dias", "v8");

    assertThat(result.status()).isEqualTo(PdeProductionSlotStatus.READY);
    assertThat(result.publishedExperienceJson()).isNull();
    assertThat(slot.getStatus()).isEqualTo(PdeProductionSlotStatus.READY);
  }

  /** Deve exigir nova homologação quando o rascunho validado da v12 for alterado. */
  @Test
  void clearsV12ValidationEvidenceWhenCandidateArtifactChanges() {
    PdeProductionSlot slot = v12Slot(PdeProductionSlotStatus.CANDIDATE);
    PdeProductionSlotService service =
        new PdeProductionSlotService(
            repository, videoAssetRepository, experimentRepository, httpClient, new ObjectMapper());
    when(repository.findByProductSlugAndSlotCode("metodo-musa-7-dias", "v8"))
        .thenReturn(Optional.of(slot));
    when(repository.save(org.mockito.ArgumentMatchers.any(PdeProductionSlot.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    String changedContract = v12Contract().replace("primeiro ajuste MUSA", "ajuste MUSA");
    var result =
        service.saveProductionSlot(
            "metodo-musa-7-dias",
            92L,
            new PostDeployPdeProductionSlotRequestDto(
                "v8",
                null,
                "v8.clubemusa.com.br",
                "https://v8.clubemusa.com.br",
                null,
                "musa-pde-entry-v12-primeiro-ajuste-aplicavel",
                "espelho-antes-de-sair",
                "production-v8",
                PdeProductionSlotStatus.CANDIDATE,
                92L,
                slot.getNotes(),
                changedContract,
                null));

    assertThat(result.validationStatus()).isNull();
    assertThat(result.validationCheckedAt()).isNull();
    assertThat(result.validationSummary()).isNull();
  }

  /** Deve rejeitar um vídeo aprovado quando ele não for o ativo #41 escolhido para o teste. */
  @Test
  void blocksV12PreparationWithAnotherApprovedAdVideo() {
    PdeProductionSlot slot = v12Slot(PdeProductionSlotStatus.CANDIDATE);
    PdeProductionSlotService service =
        new PdeProductionSlotService(
            repository, videoAssetRepository, experimentRepository, httpClient, new ObjectMapper());
    when(repository.findByProductSlugAndSlotCode("metodo-musa-7-dias", "v8"))
        .thenReturn(Optional.of(slot));
    when(experimentRepository.findById(92L)).thenReturn(Optional.of(v12Experiment()));
    when(videoAssetRepository.findByExperimentIdOrderByCreatedAtDesc(92L))
        .thenReturn(
            List.of(
                ExperimentVideoAsset.builder()
                    .id(99L)
                    .slot(ExperimentVideoSlot.AD)
                    .status(ExperimentVideoStatus.READY)
                    .reviewStatus(ExperimentVideoReviewStatus.APPROVED)
                    .hlsPlaybackUrl("https://cdn.example/outro-anuncio.m3u8")
                    .build(),
                v12Videos().get(1)));

    assertThatThrownBy(
            () -> service.prepareProductionSlotForPublication("metodo-musa-7-dias", "v8"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("vídeo de anúncio #41");
  }

  /** Deve entregar o rascunho da candidata exata ao preflight sem publicá-lo. */
  @Test
  void returnsCandidateDraftForAuthenticatedPreflightService() {
    PdeProductionSlot slot = v12Slot(PdeProductionSlotStatus.CANDIDATE);
    PdeProductionSlotService service =
        new PdeProductionSlotService(
            repository, videoAssetRepository, httpClient, new ObjectMapper());
    when(repository.findByProductSlugAndSlotCode("metodo-musa-7-dias", "v8"))
        .thenReturn(Optional.of(slot));

    String contract =
        service.findValidationExperienceJson(
            "metodo-musa-7-dias", "v8", "musa-pde-entry-v12-primeiro-ajuste-aplicavel");

    assertThat(contract).contains("musa-pde-entry-v12-primeiro-ajuste-aplicavel");
    assertThat(slot.getPublishedExperienceJson()).isNull();
    assertThat(slot.getStatus()).isEqualTo(PdeProductionSlotStatus.CANDIDATE);
  }

  /** Deve recusar preflight de versão pausada mesmo quando ela ainda possui rascunho. */
  @Test
  void rejectsPausedCandidateDraftForPreflight() {
    PdeProductionSlot slot = v12Slot(PdeProductionSlotStatus.PAUSED);
    PdeProductionSlotService service =
        new PdeProductionSlotService(
            repository, videoAssetRepository, httpClient, new ObjectMapper());
    when(repository.findByProductSlugAndSlotCode("metodo-musa-7-dias", "v8"))
        .thenReturn(Optional.of(slot));

    assertThatThrownBy(
            () ->
                service.findValidationExperienceJson(
                    "metodo-musa-7-dias", "v8", "musa-pde-entry-v12-primeiro-ajuste-aplicavel"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("não está disponível para preflight");
  }

  /** Monta o slot canônico da v12 sem reutilizar a identidade publicada da v7. */
  private PdeProductionSlot v12Slot(PdeProductionSlotStatus status) {
    return PdeProductionSlot.builder()
        .id(8L)
        .slotCode("v8")
        .productSlug("metodo-musa-7-dias")
        .domain("v8.clubemusa.com.br")
        .publicUrl("https://v8.clubemusa.com.br")
        .experienceVersion("musa-pde-entry-v12-primeiro-ajuste-aplicavel")
        .layoutKey("espelho-antes-de-sair")
        .targetEnvironment("production-v8")
        .status(status)
        .sourceExperimentId(92L)
        .draftExperienceJson(v12Contract())
        .validationStatus("OK")
        .createdAt(Instant.parse("2026-09-16T00:00:00Z"))
        .updatedAt(Instant.parse("2026-09-16T00:00:00Z"))
        .build();
  }

  /** Monta a oferta persistida que deve coincidir com o contrato candidato. */
  private Experiment v12Experiment() {
    return Experiment.builder()
        .id(92L)
        .product(com.marketinghub.product.Product.builder().slug("metodo-musa-7-dias").build())
        .unitPrice(new java.math.BigDecimal("67"))
        .primaryCta("Ver meu primeiro ajuste MUSA")
        .commercialCheckoutUrl("https://go.pepper.com.br/owm6x")
        .build();
  }

  /** Monta os dois vídeos obrigatórios já aprovados para o mesmo experimento. */
  private List<ExperimentVideoAsset> v12Videos() {
    return List.of(
        ExperimentVideoAsset.builder()
            .id(41L)
            .slot(ExperimentVideoSlot.AD)
            .status(ExperimentVideoStatus.READY)
            .reviewStatus(ExperimentVideoReviewStatus.APPROVED)
            .hlsPlaybackUrl("https://cdn.example/ad-v12.m3u8")
            .build(),
        ExperimentVideoAsset.builder()
            .id(42L)
            .slot(ExperimentVideoSlot.LANDING_HERO)
            .status(ExperimentVideoStatus.READY)
            .reviewStatus(ExperimentVideoReviewStatus.APPROVED)
            .hlsPlaybackUrl("https://cdn.example/hero-v12.m3u8")
            .build());
  }

  /** Declara o contrato mínimo completo usado no gate comercial da v12. */
  private String v12Contract() {
    return """
        {
          "slug":"metodo-musa-7-dias",
          "experienceVersion":"musa-pde-entry-v12-primeiro-ajuste-aplicavel",
          "layoutKey":"espelho-antes-de-sair",
          "commercialBinding":{"experimentId":92,"primaryCta":"Ver meu primeiro ajuste MUSA","priceBrl":67,"billingModel":"ONE_TIME"},
          "commercialCheckout":{"provider":"PEPPER","checkoutUrl":"https://go.pepper.com.br/owm6x","priceBrl":67,"currency":"BRL","billingModel":"ONE_TIME"},
          "supportMaterials":[{"url":"/materials/musa-v12/mapa-dos-7-sinais.html"}],
          "heroVideos":[{"experimentVideoAssetId":42,"experienceVersion":"musa-pde-entry-v12-primeiro-ajuste-aplicavel","status":"READY","reviewStatus":"APPROVED","hlsPlaybackUrl":"https://cdn.example/hero-v12.m3u8"}]
        }
        """;
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

  /** Deve listar vídeo HLS publicado no contrato PDE mesmo sem ativo persistido do experimento. */
  @Test
  void listsPublishedContractHeroVideoWithoutExperimentAsset() {
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
            .publishedExperienceJson(
                """
                {
                  "heroVideos": [{
                    "source": "MARKETING_HUB_MANAGED_HLS",
                    "status": "READY",
                    "reviewStatus": "APPROVED",
                    "assetId": 1935,
                    "salesVideoJobId": 20462,
                    "salesVideoProfileId": 35,
                    "hlsPlaybackUrl": "/assets/hls/musa-v6-microexperiencia-visivel/index.m3u8"
                  }]
                }
                """)
            .build();
    PdeProductionSlotService service =
        new PdeProductionSlotService(
            repository, videoAssetRepository, httpClient, new ObjectMapper());
    when(repository.findByProductSlugOrderBySlotCodeAsc("metodo-musa-7-dias"))
        .thenReturn(List.of(v6));
    when(videoAssetRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

    var response = service.listProductionSlotVideosForProduct("metodo-musa-7-dias");

    assertThat(response).hasSize(1);
    assertThat(response.getFirst().slot().slotCode()).isEqualTo("v6");
    assertThat(response.getFirst().videos()).hasSize(1);
    assertThat(response.getFirst().videos().getFirst().assignmentSource())
        .isEqualTo("PUBLISHED_CONTRACT");
    assertThat(response.getFirst().videos().getFirst().assetId()).isEqualTo(1935L);
    assertThat(response.getFirst().videos().getFirst().experimentId()).isEqualTo(76L);
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

  /** Deve exigir oferta real com preço e checkout antes de aprovar um produto PDE comercial. */
  @Test
  void recordsOkValidationForCommercialPdeWithAttributedOffer() throws Exception {
    PdeProductionSlot slot =
        PdeProductionSlot.builder()
            .id(7L)
            .slotCode("v1")
            .productSlug("kit-whatsapp-pronto")
            .domain("kit-whatsapp-pronto.digicomdigital.com.br")
            .publicUrl("https://kit-whatsapp-pronto.digicomdigital.com.br")
            .experienceVersion("kit-whatsapp-pronto-pde-v1")
            .targetEnvironment("production-v1")
            .status(PdeProductionSlotStatus.ACTIVE)
            .sourceExperimentId(89L)
            .createdAt(Instant.parse("2026-08-22T10:00:00Z"))
            .updatedAt(Instant.parse("2026-08-22T10:00:00Z"))
            .build();
    PdeProductionSlotService service =
        new PdeProductionSlotService(
            repository, videoAssetRepository, httpClient, new ObjectMapper());
    when(repository.findByProductSlugAndSlotCode("kit-whatsapp-pronto", "v1"))
        .thenReturn(Optional.of(slot));
    HttpResponse<String> healthResponse = response(200, "{\"status\":\"UP\"}");
    HttpResponse<String> contractResponse =
        response(
            200,
            "{\"slug\":\"kit-whatsapp-pronto\",\"healthPath\":\"/\",\"commercialOfferPath\":\"/api/pde/products/kit-whatsapp-pronto/commercial-offer\",\"integrationContractPath\":\"/api/pde/products/kit-whatsapp-pronto/integration-contract\",\"requiredTexts\":[\"Quero meu atendimento sob medida\"]}");
    HttpResponse<String> offerResponse =
        response(
            200,
            "{\"productSlug\":\"kit-whatsapp-pronto\",\"experimentId\":89,\"priceBrl\":349,\"promise\":\"Implantação personalizada em até 48 horas\",\"primaryCta\":\"Quero meu atendimento sob medida\",\"checkoutUrl\":\"https://pay.example/kit\",\"supplierDisplayName\":\"Digicom Digital\",\"supplierRegistrationNumber\":\"00.000.000/0001-00\",\"supportEmail\":\"teste@sandbox.local\",\"termsUrl\":\"https://kit-whatsapp-pronto.digicomdigital.com.br/terms\",\"privacyUrl\":\"https://kit-whatsapp-pronto.digicomdigital.com.br/privacy\",\"refundPolicyUrl\":\"https://kit-whatsapp-pronto.digicomdigital.com.br/refund-policy\"}");
    HttpResponse<String> integrationResponse =
        response(
            200,
            "{\"productSlug\":\"kit-whatsapp-pronto\",\"experienceVersion\":\"kit-whatsapp-pronto-pde-v1\",\"contractVersion\":\"PDE_COMMERCIAL_JOURNEY_EVENTS_V1\",\"eventsPath\":\"/api/pde/access/events\",\"analyticsSummaryPath\":\"/api/pde/access/analytics/{productSlug}/summary\",\"loginPath\":\"/api/pde/access/login-link\",\"workspacePathTemplate\":\"/api/pde/access/workspace\",\"missionCompletionPathTemplate\":\"/api/pde/access/missions/{missionId}/complete\",\"requiredEventTypes\":[\"PAGE_VIEW\",\"VALUE_MOMENT\",\"CTA_VIEWED\",\"CHECKOUT_STARTED\",\"PURCHASE_COMPLETED\",\"ACCESS_RELEASED\",\"MISSION_COMPLETED\",\"FIRST_USE\",\"REFUND_CONFIRMED\"],\"correlationKeys\":[\"eventId\",\"productSlug\",\"experienceVersion\",\"sessionId\",\"visitorId\",\"accessReferenceHash\"],\"sourceOfTruth\":\"pde_funnel_event\",\"testTrafficPolicy\":\"trafficQuality=INTERNAL_QA\"}");
    HttpResponse<String> pageResponse =
        response(
            200,
            "<html><body><div id=\"root\"></div><script type=\"module\" src=\"/assets/index.js\"></script></body></html>");
    HttpResponse<String> scriptResponse =
        response(200, "const cta = 'Quero meu atendimento sob medida';");
    org.mockito.Mockito.doReturn(healthResponse)
        .doReturn(contractResponse)
        .doReturn(offerResponse)
        .doReturn(integrationResponse)
        .doReturn(pageResponse)
        .doReturn(scriptResponse)
        .when(httpClient)
        .send(
            org.mockito.ArgumentMatchers.any(HttpRequest.class),
            org.mockito.ArgumentMatchers.any());
    when(repository.save(org.mockito.ArgumentMatchers.any(PdeProductionSlot.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var response = service.validateProductionSlot("kit-whatsapp-pronto", "v1");

    assertThat(response.validationStatus()).isEqualTo("OK");
    assertThat(response.validationSummary()).isEqualTo("URL produtiva validada");
    org.mockito.Mockito.verify(httpClient, org.mockito.Mockito.times(6))
        .send(
            org.mockito.ArgumentMatchers.any(HttpRequest.class),
            org.mockito.ArgumentMatchers.any());
  }

  /** Deve rejeitar contrato público que exponha token bruto como chave de correlação. */
  @Test
  void rejectsCommercialPdeThatDeclaresRawAccessToken() throws Exception {
    PdeProductionSlot slot =
        PdeProductionSlot.builder()
            .id(8L)
            .slotCode("v8")
            .productSlug("metodo-musa-7-dias")
            .domain("v8.clubemusa.com.br")
            .publicUrl("https://v8.clubemusa.com.br")
            .backendUrl("https://v8.clubemusa.com.br/api")
            .experienceVersion("musa-pde-entry-v12-primeiro-ajuste-aplicavel")
            .targetEnvironment("production-v8")
            .status(PdeProductionSlotStatus.ACTIVE)
            .sourceExperimentId(92L)
            .createdAt(Instant.parse("2026-09-15T10:00:00Z"))
            .updatedAt(Instant.parse("2026-09-15T10:00:00Z"))
            .build();
    PdeProductionSlotService service =
        new PdeProductionSlotService(
            repository, videoAssetRepository, httpClient, new ObjectMapper());
    when(repository.findByProductSlugAndSlotCode("metodo-musa-7-dias", "v8"))
        .thenReturn(Optional.of(slot));
    HttpResponse<String> healthResponse = response(200, "{\"status\":\"UP\"}");
    HttpResponse<String> contractResponse =
        response(
            200,
            "{\"slug\":\"metodo-musa-7-dias\",\"healthPath\":\"/\",\"commercialOfferPath\":\"/api/pde/products/metodo-musa-7-dias/commercial-offer\",\"integrationContractPath\":\"/api/pde/products/metodo-musa-7-dias/integration-contract\",\"requiredTexts\":[\"Quero continuar\"]}");
    HttpResponse<String> offerResponse =
        response(
            200,
            "{\"productSlug\":\"metodo-musa-7-dias\",\"experimentId\":92,\"priceBrl\":67,\"promise\":\"Primeiro ajuste aplicável\",\"primaryCta\":\"Quero continuar\",\"checkoutUrl\":\"https://pay.example/musa\",\"supplierDisplayName\":\"Digicom Digital\",\"supplierRegistrationNumber\":\"00.000.000/0001-00\",\"supportEmail\":\"teste@sandbox.local\",\"termsUrl\":\"https://v8.clubemusa.com.br/terms\",\"privacyUrl\":\"https://v8.clubemusa.com.br/privacy\",\"refundPolicyUrl\":\"https://v8.clubemusa.com.br/refund-policy\"}");
    HttpResponse<String> integrationResponse =
        response(
            200,
            "{\"productSlug\":\"metodo-musa-7-dias\",\"experienceVersion\":\"musa-pde-entry-v12-primeiro-ajuste-aplicavel\",\"contractVersion\":\"PDE_COMMERCIAL_JOURNEY_EVENTS_V1\",\"eventsPath\":\"/api/pde/access/events\",\"analyticsSummaryPath\":\"/api/pde/access/analytics/{productSlug}/summary\",\"loginPath\":\"/api/pde/access/login-link\",\"workspacePathTemplate\":\"/api/pde/access/workspace\",\"missionCompletionPathTemplate\":\"/api/pde/access/missions/{missionId}/complete\",\"requiredEventTypes\":[\"PAGE_VIEW\",\"VALUE_MOMENT\",\"CTA_VIEWED\",\"CHECKOUT_STARTED\",\"PURCHASE_COMPLETED\",\"ACCESS_RELEASED\",\"MISSION_COMPLETED\",\"FIRST_USE\",\"REFUND_CONFIRMED\"],\"correlationKeys\":[\"eventId\",\"productSlug\",\"experienceVersion\",\"sessionId\",\"visitorId\",\"accessReferenceHash\",\"accessToken\"],\"sourceOfTruth\":\"pde_funnel_event\",\"testTrafficPolicy\":\"trafficQuality=INTERNAL_QA\"}");
    org.mockito.Mockito.doReturn(healthResponse)
        .doReturn(contractResponse)
        .doReturn(offerResponse)
        .doReturn(integrationResponse)
        .when(httpClient)
        .send(
            org.mockito.ArgumentMatchers.any(HttpRequest.class),
            org.mockito.ArgumentMatchers.any());
    when(repository.save(org.mockito.ArgumentMatchers.any(PdeProductionSlot.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var response = service.validateProductionSlot("metodo-musa-7-dias", "v8");

    assertThat(response.validationStatus()).isEqualTo("FAILED");
    assertThat(response.validationSummary())
        .isEqualTo("Contrato de integração da jornada está incompleto");
    assertThat(response.validationDetail()).contains("correlação");
  }

  /**
   * Deve bloquear PDE comercial que anuncia checkout sem declarar o contrato de eventos e acesso.
   */
  @Test
  void rejectsCommercialPdeWithoutJourneyIntegrationContractPath() throws Exception {
    PdeProductionSlot slot =
        PdeProductionSlot.builder()
            .id(7L)
            .slotCode("v1")
            .productSlug("kit-whatsapp-pronto")
            .domain("kit-whatsapp-pronto.digicomdigital.com.br")
            .publicUrl("https://kit-whatsapp-pronto.digicomdigital.com.br")
            .experienceVersion("kit-whatsapp-pronto-pde-v1")
            .targetEnvironment("production-v1")
            .status(PdeProductionSlotStatus.ACTIVE)
            .sourceExperimentId(89L)
            .build();
    PdeProductionSlotService service =
        new PdeProductionSlotService(
            repository, videoAssetRepository, httpClient, new ObjectMapper());
    when(repository.findByProductSlugAndSlotCode("kit-whatsapp-pronto", "v1"))
        .thenReturn(Optional.of(slot));
    HttpResponse<String> healthResponse = response(200, "{\"status\":\"UP\"}");
    HttpResponse<String> contractResponse =
        response(
            200,
            "{\"slug\":\"kit-whatsapp-pronto\",\"healthPath\":\"/\",\"commercialOfferPath\":\"/api/pde/products/kit-whatsapp-pronto/commercial-offer\",\"requiredTexts\":[\"CTA\"]}");
    org.mockito.Mockito.doReturn(healthResponse)
        .doReturn(contractResponse)
        .when(httpClient)
        .send(
            org.mockito.ArgumentMatchers.any(HttpRequest.class),
            org.mockito.ArgumentMatchers.any());
    when(repository.save(org.mockito.ArgumentMatchers.any(PdeProductionSlot.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var response = service.validateProductionSlot("kit-whatsapp-pronto", "v1");

    assertThat(response.validationStatus()).isEqualTo("FAILED");
    assertThat(response.validationSummary())
        .isEqualTo("Contrato público não declara integração da jornada");
    org.mockito.Mockito.verify(httpClient, org.mockito.Mockito.times(2))
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

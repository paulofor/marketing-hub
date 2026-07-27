package com.marketinghub.experiment.video.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.LandingPage;
import com.marketinghub.experiment.video.ExperimentVideoAsset;
import com.marketinghub.experiment.video.ExperimentVideoReviewStatus;
import com.marketinghub.experiment.video.ExperimentVideoSlot;
import com.marketinghub.experiment.video.ExperimentVideoStatus;
import com.marketinghub.experiment.video.dto.CreateExperimentVideoAssetRequest;
import com.marketinghub.experiment.video.dto.ExperimentVideoAssetDto;
import com.marketinghub.experiment.video.dto.RequestExperimentVeoVideoRequest;
import com.marketinghub.experiment.video.dto.RequestExperimentVideoPostProductionRequest;
import com.marketinghub.experiment.video.dto.RequestPlannedExperimentVideoRenderRequest;
import com.marketinghub.experiment.video.dto.UpdateExperimentVideoAssetRequest;
import com.marketinghub.media.Asset;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experiment.LandingPageRepository;
import com.marketinghub.repository.jpa.experiment.video.ExperimentVideoAssetRepository;
import com.marketinghub.repository.jpa.media.AssetRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import com.marketinghub.repository.jpa.salesvideo.LandingVideoSlotRepository;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoJobRepository;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoProfileRepository;
import com.marketinghub.salesvideo.LandingVideoSlot;
import com.marketinghub.salesvideo.SalesVideoExecutionMode;
import com.marketinghub.salesvideo.SalesVideoJob;
import com.marketinghub.salesvideo.SalesVideoJobType;
import com.marketinghub.salesvideo.SalesVideoProfile;
import com.marketinghub.salesvideo.SalesVideoScript;
import com.marketinghub.salesvideo.SalesVideoScriptStatus;
import com.marketinghub.salesvideo.SalesVideoStatus;
import com.marketinghub.salesvideo.dto.CreateSalesVideoProfileRequest;
import com.marketinghub.salesvideo.dto.RequestVideoRenderRequest;
import com.marketinghub.salesvideo.dto.SalesVideoJobDto;
import com.marketinghub.salesvideo.dto.SalesVideoProfileDto;
import com.marketinghub.salesvideo.service.SalesVideoJobService;
import com.marketinghub.salesvideo.service.SalesVideoProductionCostCalculator;
import com.marketinghub.salesvideo.service.SalesVideoService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

/** Valida o registro de vídeos como ativos comerciais vinculados ao experimento. */
@ExtendWith(MockitoExtension.class)
class ExperimentVideoAssetServiceTest {
  @Mock private ExperimentVideoAssetRepository repository;
  @Mock private ExperimentRepository experimentRepository;
  @Mock private SalesVideoProfileRepository profileRepository;
  @Mock private SalesVideoJobRepository jobRepository;
  @Mock private AssetRepository assetRepository;
  @Mock private LandingVideoSlotRepository landingVideoSlotRepository;
  @Mock private ProductRepository productRepository;
  @Mock private LandingPageRepository landingPageRepository;
  @Mock private SalesVideoService salesVideoService;
  @Mock private SalesVideoJobService salesVideoJobService;

  private ExperimentVideoAssetService service;

  /** Inicializa o serviço com repositórios simulados para testes unitários. */
  @BeforeEach
  void setUp() {
    service =
        new ExperimentVideoAssetService(
            repository,
            experimentRepository,
            profileRepository,
            jobRepository,
            assetRepository,
            landingVideoSlotRepository,
            productRepository,
            landingPageRepository,
            salesVideoService,
            salesVideoJobService,
            new SalesVideoProductionCostCalculator());
  }

  /** Garante que um vídeo novo recebe estados padrão quando criado para o experimento. */
  @Test
  void shouldCreatePlannedVideoAssetForExperiment() {
    Experiment experiment = Experiment.builder().id(39L).build();
    given(experimentRepository.findById(39L)).willReturn(Optional.of(experiment));
    given(repository.save(any(ExperimentVideoAsset.class)))
        .willAnswer(
            invocation -> {
              ExperimentVideoAsset saved = invocation.getArgument(0);
              saved.setId(7L);
              return saved;
            });
    CreateExperimentVideoAssetRequest request =
        new CreateExperimentVideoAssetRequest(
            ExperimentVideoSlot.LANDING_HERO,
            "Aumentar envio do formulario",
            "form_submit_rate",
            "Agenda cheia no WhatsApp ainda pode estar vulneravel.",
            "Gerar video vertical curto",
            "VEO",
            "veo-3.1-generate-preview",
            null,
            null,
            null,
            8,
            null,
            "9:16",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            true,
            null,
            null,
            null,
            null);

    ExperimentVideoAssetDto dto = service.create(39L, request);

    assertThat(dto.id()).isEqualTo(7L);
    assertThat(dto.status()).isEqualTo(ExperimentVideoStatus.PLANNED);
    assertThat(dto.reviewStatus()).isEqualTo(ExperimentVideoReviewStatus.PENDING);
    assertThat(dto.requiredForRelease()).isTrue();
  }

  /** Garante que o Hub cria o fluxo VEO curto completo a partir do experimento. */
  @Test
  void shouldRequestVeoRenderFromExperiment() {
    MarketNiche niche = MarketNiche.builder().id(31L).name("Beleza").build();
    Experiment experiment =
        Experiment.builder()
            .id(61L)
            .niche(niche)
            .name("Guia de elegancia")
            .singlePain("Aparencia comum mesmo gastando")
            .funnelPromise("Parecer mais elegante sem gastar muito")
            .build();
    Product product = Product.builder().id(3L).marketNiche(niche).build();
    SalesVideoProfile profile = SalesVideoProfile.builder().id(12L).product(product).build();
    SalesVideoJob job = SalesVideoJob.builder().id(10108L).profile(profile).build();
    SalesVideoProfileDto profileDto = new SalesVideoProfileDto();
    profileDto.setId(12L);
    SalesVideoJobDto jobDto = new SalesVideoJobDto();
    jobDto.setId(10108L);
    RequestExperimentVeoVideoRequest request =
        new RequestExperimentVeoVideoRequest(
            ExperimentVideoSlot.LANDING_HERO,
            "Video VEO experimento 61",
            "Aumentar conversao da pagina",
            "checkout_start_rate",
            "Consultora",
            "premium acessivel",
            "natural",
            "pt-BR",
            8,
            "Mostre como pequenos detalhes mudam a percepcao de elegancia.",
            "Voce parece comum mesmo se arrumando?",
            "Quero o guia",
            null,
            "Retrato vertical de consultora brasileira elegante, natural, premium acessivel.",
            "gpt-image-2",
            "img-123",
            7001L,
            "https://cdn.test/musa-personagem.png",
            "VEO",
            SalesVideoExecutionMode.TEST,
            "time@marketinghub.io",
            true);
    given(experimentRepository.findById(61L)).willReturn(Optional.of(experiment));
    given(productRepository.findFirstByMarketNiche_IdOrderByCreatedAtDesc(31L))
        .willReturn(Optional.of(product));
    given(landingPageRepository.findByExperimentId(61L)).willReturn(List.of());
    given(salesVideoService.createProfile(any(), any())).willReturn(profileDto);
    given(salesVideoService.requestRender(any(), any())).willReturn(jobDto);
    given(profileRepository.findById(12L)).willReturn(Optional.of(profile));
    given(jobRepository.findById(10108L)).willReturn(Optional.of(job));
    given(repository.save(any(ExperimentVideoAsset.class)))
        .willAnswer(
            invocation -> {
              ExperimentVideoAsset saved = invocation.getArgument(0);
              saved.setId(77L);
              return saved;
            });

    ExperimentVideoAssetDto dto = service.requestVeoRender(61L, request);

    assertThat(dto.id()).isEqualTo(77L);
    assertThat(dto.status()).isEqualTo(ExperimentVideoStatus.GENERATING);
    assertThat(dto.reviewStatus()).isEqualTo(ExperimentVideoReviewStatus.PENDING);
    assertThat(dto.provider()).isEqualTo("VEO");
    assertThat(dto.salesVideoProfileId()).isEqualTo(12L);
    assertThat(dto.salesVideoJobId()).isEqualTo(10108L);
    assertThat(dto.requiredForRelease()).isTrue();
    assertThat(dto.cost()).isEqualByComparingTo("3.2000");
    assertThat(dto.prompt())
        .contains("gpt-image-2", "Retrato vertical de consultora brasileira elegante");
  }

  /** Bloqueia pedido direto de VEO quando a duração passa do limite nativo do provider. */
  @Test
  void shouldRejectVeoRenderAboveEightSecondsFromExperiment() {
    MarketNiche niche = MarketNiche.builder().id(31L).name("Beleza").build();
    Experiment experiment =
        Experiment.builder()
            .id(61L)
            .niche(niche)
            .name("Guia de elegancia")
            .singlePain("Aparencia comum mesmo gastando")
            .funnelPromise("Parecer mais elegante sem gastar muito")
            .build();
    RequestExperimentVeoVideoRequest request =
        new RequestExperimentVeoVideoRequest(
            ExperimentVideoSlot.LANDING_HERO,
            "Video VEO experimento 61",
            "Aumentar conversao da pagina",
            "checkout_start_rate",
            "Consultora",
            "premium acessivel",
            "natural",
            "pt-BR",
            30,
            "Mostre como pequenos detalhes mudam a percepcao de elegancia.",
            "Voce parece comum mesmo se arrumando?",
            "Quero o guia",
            null,
            "Retrato vertical de consultora brasileira elegante, natural, premium acessivel.",
            "gpt-image-2",
            "img-123",
            7001L,
            "https://cdn.test/musa-personagem.png",
            "VEO",
            SalesVideoExecutionMode.TEST,
            "time@marketinghub.io",
            true);
    given(experimentRepository.findById(61L)).willReturn(Optional.of(experiment));

    ResponseStatusException ex =
        assertThrows(ResponseStatusException.class, () -> service.requestVeoRender(61L, request));

    assertThat(ex.getReason()).contains("VEO aceita no máximo 8 segundos");
  }

  /** Bloqueia criação de ativo planejado quando a duração excede o limite do provider. */
  @Test
  void shouldRejectPlannedVideoAssetAboveProviderLimit() {
    Experiment experiment = Experiment.builder().id(39L).build();
    given(experimentRepository.findById(39L)).willReturn(Optional.of(experiment));
    CreateExperimentVideoAssetRequest request =
        new CreateExperimentVideoAssetRequest(
            ExperimentVideoSlot.AD,
            "Aumentar cliques",
            "ctr",
            "Roteiro curto",
            "Prompt curto",
            "KLING_3_0",
            "kling-v3",
            null,
            null,
            null,
            15,
            null,
            "9:16",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            true,
            null,
            null,
            null,
            null);

    ResponseStatusException ex =
        assertThrows(ResponseStatusException.class, () -> service.create(39L, request));

    assertThat(ex.getReason()).contains("Kling aceita no máximo 10 segundos");
  }

  /** Bloqueia ativo planejado Runway quando a duração excede o limite direto do provider. */
  @Test
  void shouldRejectPlannedRunwayVideoAssetAboveProviderLimit() {
    Experiment experiment = Experiment.builder().id(39L).build();
    given(experimentRepository.findById(39L)).willReturn(Optional.of(experiment));
    CreateExperimentVideoAssetRequest request =
        new CreateExperimentVideoAssetRequest(
            ExperimentVideoSlot.AD,
            "Aumentar cliques",
            "ctr",
            "Roteiro curto",
            "Prompt curto",
            "RUNWAY",
            "gen4.5",
            null,
            null,
            null,
            15,
            null,
            "9:16",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            true,
            null,
            null,
            null,
            null);

    ResponseStatusException ex =
        assertThrows(ResponseStatusException.class, () -> service.create(39L, request));

    assertThat(ex.getReason()).contains("Runway aceita no máximo 10 segundos");
  }

  /** Garante que ativos planejados viram jobs de render sem criar ativos duplicados. */
  @Test
  void shouldRequestRenderForExistingPlannedVideoAssets() {
    MarketNiche niche = MarketNiche.builder().id(31L).name("Beleza").build();
    Experiment experiment =
        Experiment.builder()
            .id(68L)
            .niche(niche)
            .name("Metodo MUSA")
            .singlePain("Imagem sem presenca")
            .funnelPromise("Parecer mais elegante em 7 dias")
            .build();
    Product product = Product.builder().id(3L).marketNiche(niche).build();
    SalesVideoProfile profile = SalesVideoProfile.builder().id(12L).product(product).build();
    SalesVideoJob job = SalesVideoJob.builder().id(20431L).profile(profile).build();
    ExperimentVideoAsset plannedAsset =
        ExperimentVideoAsset.builder()
            .id(5L)
            .experiment(experiment)
            .slot(ExperimentVideoSlot.LANDING_HERO)
            .objective("Aumentar clique no diagnostico")
            .primaryMetric("diagnostico iniciado")
            .script("Clipe 1 - Dor do espelho.")
            .prompt("Video vertical 9:16, 8 segundos, estilo editorial realista.")
            .provider("LUMA_RAY_3_2")
            .model("luma-ray-3.2")
            .durationSeconds(8)
            .aspectRatio("9:16")
            .status(ExperimentVideoStatus.PLANNED)
            .reviewStatus(ExperimentVideoReviewStatus.PENDING)
            .requiredForRelease(true)
            .build();
    SalesVideoProfileDto profileDto = new SalesVideoProfileDto();
    profileDto.setId(12L);
    SalesVideoJobDto jobDto = new SalesVideoJobDto();
    jobDto.setId(20431L);
    given(experimentRepository.findById(68L)).willReturn(Optional.of(experiment));
    given(productRepository.findFirstByMarketNiche_IdOrderByCreatedAtDesc(31L))
        .willReturn(Optional.of(product));
    given(landingPageRepository.findByExperimentId(68L)).willReturn(List.of());
    given(repository.findByExperimentIdOrderByCreatedAtDesc(68L)).willReturn(List.of(plannedAsset));
    given(salesVideoService.createProfile(any(), any())).willReturn(profileDto);
    given(salesVideoService.requestRender(any(), any())).willReturn(jobDto);
    given(profileRepository.findById(12L)).willReturn(Optional.of(profile));
    given(jobRepository.findById(20431L)).willReturn(Optional.of(job));
    given(repository.save(any(ExperimentVideoAsset.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    List<ExperimentVideoAssetDto> result =
        service.requestPlannedRenders(
            68L,
            new RequestPlannedExperimentVideoRenderRequest(
                "time@marketinghub.io",
                SalesVideoExecutionMode.TEST,
                null,
                "editorial premium acessivel",
                "natural",
                "pt-BR",
                true));

    assertThat(result).hasSize(1);
    assertThat(result.get(0).id()).isEqualTo(5L);
    assertThat(result.get(0).status()).isEqualTo(ExperimentVideoStatus.GENERATING);
    assertThat(result.get(0).provider()).isEqualTo("LUMA_RAY_3_2");
    assertThat(result.get(0).durationSeconds()).isEqualTo(30);
    assertThat(result.get(0).salesVideoProfileId()).isEqualTo(12L);
    assertThat(result.get(0).salesVideoJobId()).isEqualTo(20431L);
    ArgumentCaptor<CreateSalesVideoProfileRequest> profileRequest =
        ArgumentCaptor.forClass(CreateSalesVideoProfileRequest.class);
    verify(salesVideoService).createProfile(any(), profileRequest.capture());
    assertThat(profileRequest.getValue().getTargetDurationSeconds()).isEqualTo(30);
    ArgumentCaptor<RequestVideoRenderRequest> renderRequest =
        ArgumentCaptor.forClass(RequestVideoRenderRequest.class);
    verify(salesVideoService).requestRender(any(), renderRequest.capture());
    assertThat(renderRequest.getValue().getMetadataJson())
        .contains("\"durationSeconds\":30")
        .contains("\"funnelRole\":\"LANDING_HERO\"")
        .contains("\"recommendedPaidTrafficDerivativeSeconds\":[10,15]")
        .contains("\"generation_strategy\":\"OPENAI_IMAGE_TO_LUMA_VIDEO\"")
        .contains("\"enabled\":true")
        .contains("\"reference_image_count\":2")
        .contains("no flickering")
        .contains("lighting oscillation");
    ArgumentCaptor<ExperimentVideoAsset> savedAsset =
        ArgumentCaptor.forClass(ExperimentVideoAsset.class);
    verify(repository).save(savedAsset.capture());
    assertThat(savedAsset.getValue().getId()).isEqualTo(5L);
    assertThat(savedAsset.getValue().getDurationSeconds()).isEqualTo(30);
  }

  /** Permite reprocessar o mesmo ativo quando o job anterior falhou no executor de vídeo. */
  @Test
  void shouldRequestNewRenderForAssetWithFailedSalesVideoJob() {
    MarketNiche niche = MarketNiche.builder().id(31L).name("Beleza").build();
    Experiment experiment = Experiment.builder().id(68L).niche(niche).name("Metodo MUSA").build();
    Product product = Product.builder().id(3L).marketNiche(niche).build();
    SalesVideoProfile oldProfile = SalesVideoProfile.builder().id(12L).product(product).build();
    SalesVideoJob failedJob =
        SalesVideoJob.builder()
            .id(20431L)
            .profile(oldProfile)
            .status(SalesVideoStatus.VIDEO_FAILED)
            .build();
    SalesVideoProfile newProfile = SalesVideoProfile.builder().id(13L).product(product).build();
    SalesVideoJob newJob = SalesVideoJob.builder().id(20435L).profile(newProfile).build();
    ExperimentVideoAsset asset =
        ExperimentVideoAsset.builder()
            .id(5L)
            .experiment(experiment)
            .slot(ExperimentVideoSlot.LANDING_HERO)
            .objective("Aumentar clique no diagnostico")
            .primaryMetric("diagnostico iniciado")
            .script("Clipe 1 - Dor do espelho.")
            .prompt("Video vertical 9:16, 8 segundos, estilo editorial realista.")
            .provider("LUMA_RAY_3_2")
            .model("luma-ray-3.2")
            .durationSeconds(8)
            .aspectRatio("9:16")
            .status(ExperimentVideoStatus.GENERATING)
            .reviewStatus(ExperimentVideoReviewStatus.PENDING)
            .salesVideoProfile(oldProfile)
            .salesVideoJob(failedJob)
            .requiredForRelease(true)
            .build();
    SalesVideoProfileDto profileDto = new SalesVideoProfileDto();
    profileDto.setId(13L);
    SalesVideoJobDto jobDto = new SalesVideoJobDto();
    jobDto.setId(20435L);
    given(experimentRepository.findById(68L)).willReturn(Optional.of(experiment));
    given(productRepository.findFirstByMarketNiche_IdOrderByCreatedAtDesc(31L))
        .willReturn(Optional.of(product));
    given(landingPageRepository.findByExperimentId(68L)).willReturn(List.of());
    given(repository.findByExperimentIdOrderByCreatedAtDesc(68L)).willReturn(List.of(asset));
    given(salesVideoService.createProfile(any(), any())).willReturn(profileDto);
    given(salesVideoService.requestRender(any(), any())).willReturn(jobDto);
    given(profileRepository.findById(13L)).willReturn(Optional.of(newProfile));
    given(jobRepository.findById(20435L)).willReturn(Optional.of(newJob));
    given(repository.save(any(ExperimentVideoAsset.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    List<ExperimentVideoAssetDto> result =
        service.requestPlannedRenders(
            68L,
            new RequestPlannedExperimentVideoRenderRequest(
                "time@marketinghub.io",
                SalesVideoExecutionMode.TEST,
                null,
                "editorial premium acessivel",
                "natural",
                "pt-BR",
                true));

    assertThat(result).hasSize(1);
    assertThat(result.get(0).id()).isEqualTo(5L);
    assertThat(result.get(0).status()).isEqualTo(ExperimentVideoStatus.GENERATING);
    assertThat(result.get(0).durationSeconds()).isEqualTo(30);
    assertThat(result.get(0).salesVideoProfileId()).isEqualTo(13L);
    assertThat(result.get(0).salesVideoJobId()).isEqualTo(20435L);
  }

  /**
   * Garante que vídeo pronto vira job de pós-produção com fonte, voz, legenda e trilha auditáveis.
   */
  @Test
  void shouldRequestPostProductionForReadyVideoAssets() {
    Experiment experiment =
        Experiment.builder()
            .id(68L)
            .name("Metodo MUSA")
            .primaryCta("Ver meu plano MUSA de 7 dias")
            .build();
    Product product = Product.builder().id(3L).build();
    SalesVideoScript script =
        SalesVideoScript.builder()
            .id(101L)
            .version(1)
            .status(SalesVideoScriptStatus.APPROVED)
            .scriptText("Roteiro aprovado")
            .build();
    SalesVideoProfile profile = SalesVideoProfile.builder().id(12L).product(product).build();
    profile.getScripts().add(script);
    SalesVideoJob renderJob =
        SalesVideoJob.builder()
            .id(20442L)
            .profile(profile)
            .jobType(SalesVideoJobType.RENDER)
            .status(SalesVideoStatus.VIDEO_READY)
            .build();
    Asset sourceAsset = Asset.builder().id(9001L).url("https://cdn.test/musa-raw.mp4").build();
    ExperimentVideoAsset readyAsset =
        ExperimentVideoAsset.builder()
            .id(5L)
            .experiment(experiment)
            .slot(ExperimentVideoSlot.LANDING_HERO)
            .objective("Aumentar clique no diagnostico")
            .primaryMetric("diagnostico iniciado")
            .script("Dor do espelho")
            .provider("LUMA_RAY_3_2")
            .model("ray-3.2")
            .status(ExperimentVideoStatus.READY)
            .assetUrl("https://cdn.test/musa-raw.mp4")
            .durationSeconds(30)
            .reviewStatus(ExperimentVideoReviewStatus.PENDING)
            .salesVideoProfile(profile)
            .salesVideoJob(renderJob)
            .asset(sourceAsset)
            .requiredForRelease(true)
            .build();
    SalesVideoJob postJob =
        SalesVideoJob.builder()
            .id(20500L)
            .profile(profile)
            .jobType(SalesVideoJobType.POST_PRODUCTION)
            .status(SalesVideoStatus.VIDEO_REQUESTED)
            .build();
    given(experimentRepository.findById(68L)).willReturn(Optional.of(experiment));
    given(repository.findByExperimentIdOrderByCreatedAtDesc(68L)).willReturn(List.of(readyAsset));
    given(salesVideoJobService.createJob(any(), any(), any(), any(), any(), any(), any()))
        .willReturn(postJob);
    given(jobRepository.save(postJob)).willReturn(postJob);
    given(repository.save(any(ExperimentVideoAsset.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    List<ExperimentVideoAssetDto> result =
        service.requestPostProduction(
            68L,
            new RequestExperimentVideoPostProductionRequest(
                "time@marketinghub.io",
                SalesVideoExecutionMode.TEST,
                null,
                null,
                null,
                "LANDING_HERO_FINAL",
                true));

    assertThat(result).hasSize(1);
    assertThat(result.get(0).id()).isEqualTo(5L);
    assertThat(result.get(0).status()).isEqualTo(ExperimentVideoStatus.GENERATING);
    assertThat(result.get(0).provider()).isEqualTo("MUSA_POST_PRODUCTION");
    assertThat(result.get(0).salesVideoJobId()).isEqualTo(20500L);
    assertThat(postJob.getMetadataJson())
        .contains("\"artifactType\":\"experiment.videoPostProductionRequest.v1\"")
        .contains("\"sourceVideoUrl\":\"https://cdn.test/musa-raw.mp4\"")
        .contains("\"voiceOverScript\"")
        .contains("\"captionText\"")
        .contains("\"soundtrackStyle\"")
        .contains("\"createShortDerivatives\":true");
  }

  /**
   * Garante que a landing de outro experimento não pode contaminar o aprendizado do funil atual.
   */
  @Test
  void shouldRejectLandingVideoSlotFromAnotherExperiment() {
    Experiment experiment = Experiment.builder().id(39L).build();
    Experiment anotherExperiment = Experiment.builder().id(40L).build();
    LandingPage landingPage = LandingPage.builder().id(3L).experiment(anotherExperiment).build();
    LandingVideoSlot slot = LandingVideoSlot.builder().id(12L).landingPage(landingPage).build();
    given(experimentRepository.findById(39L)).willReturn(Optional.of(experiment));
    given(landingVideoSlotRepository.findById(12L)).willReturn(Optional.of(slot));
    CreateExperimentVideoAssetRequest request =
        new CreateExperimentVideoAssetRequest(
            ExperimentVideoSlot.LANDING_HERO,
            "Aumentar envio",
            "form_submit_rate",
            null,
            null,
            "VEO",
            "veo-3.1-generate-preview",
            ExperimentVideoStatus.READY,
            "https://cdn.test/video.mp4",
            null,
            8,
            true,
            "9:16",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            ExperimentVideoReviewStatus.APPROVED,
            true,
            null,
            null,
            null,
            12L);

    assertThrows(ResponseStatusException.class, () -> service.create(39L, request));
  }

  /** Garante que status operacional e revisão humana podem liberar o vídeo obrigatório. */
  @Test
  void shouldUpdateReviewStatusAndReadyState() {
    Experiment experiment = Experiment.builder().id(39L).build();
    ExperimentVideoAsset videoAsset =
        ExperimentVideoAsset.builder()
            .id(5L)
            .experiment(experiment)
            .slot(ExperimentVideoSlot.LANDING_HERO)
            .objective("Aumentar envio")
            .primaryMetric("form_submit_rate")
            .provider("VEO")
            .model("veo-3.1-generate-preview")
            .status(ExperimentVideoStatus.GENERATING)
            .reviewStatus(ExperimentVideoReviewStatus.PENDING)
            .requiredForRelease(true)
            .build();
    given(experimentRepository.findById(39L)).willReturn(Optional.of(experiment));
    given(repository.findById(5L)).willReturn(Optional.of(videoAsset));
    given(repository.save(videoAsset)).willReturn(videoAsset);

    ExperimentVideoAssetDto dto =
        service.update(
            39L,
            5L,
            new UpdateExperimentVideoAssetRequest(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                ExperimentVideoStatus.READY,
                "https://cdn.test/video.mp4",
                null,
                null,
                true,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                ExperimentVideoReviewStatus.APPROVED,
                null,
                "aprovador@marketinghub.local",
                null,
                null,
                null,
                null,
                null));

    assertThat(dto.status()).isEqualTo(ExperimentVideoStatus.READY);
    assertThat(dto.reviewStatus()).isEqualTo(ExperimentVideoReviewStatus.APPROVED);
    assertThat(dto.reviewedBy()).isEqualTo("aprovador@marketinghub.local");
    assertThat(dto.reviewedAt()).isNotNull();
    assertThat(dto.assetUrl()).isEqualTo("https://cdn.test/video.mp4");
    assertThat(dto.hasAudio()).isTrue();
  }

  /** Garante que reprovação humana sempre explique a causa para nova criação. */
  @Test
  void shouldRequireReasonWhenRejectingVideoAsset() {
    Experiment experiment = Experiment.builder().id(39L).build();
    ExperimentVideoAsset videoAsset =
        ExperimentVideoAsset.builder()
            .id(5L)
            .experiment(experiment)
            .slot(ExperimentVideoSlot.AD)
            .objective("Validar criativo")
            .primaryMetric("thumbstop")
            .provider("LUMA")
            .model("ray-3.2")
            .status(ExperimentVideoStatus.READY)
            .reviewStatus(ExperimentVideoReviewStatus.PENDING)
            .requiredForRelease(true)
            .build();
    given(experimentRepository.findById(39L)).willReturn(Optional.of(experiment));
    given(repository.findById(5L)).willReturn(Optional.of(videoAsset));

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () ->
                service.update(
                    39L,
                    5L,
                    new UpdateExperimentVideoAssetRequest(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        ExperimentVideoReviewStatus.REJECTED,
                        " ",
                        "aprovador@marketinghub.local",
                        null,
                        null,
                        null,
                        null,
                        null)));

    assertThat(exception.getStatusCode().value()).isEqualTo(400);
  }

  /** Bloqueia aprovação quando anúncio e hero compartilham a mesma origem visual sem exceção. */
  @Test
  void shouldBlockApprovalWhenAdAndHeroShareVisualSourceWithoutOverride() {
    Experiment experiment = Experiment.builder().id(39L).build();
    ExperimentVideoAsset adVideo =
        ExperimentVideoAsset.builder()
            .id(5L)
            .experiment(experiment)
            .slot(ExperimentVideoSlot.AD)
            .objective("Validar criativo")
            .primaryMetric("ctr")
            .provider("HEYGEN")
            .model("avatar-iv")
            .status(ExperimentVideoStatus.READY)
            .hasAudio(true)
            .visualSourceKey("sofia-musa")
            .reviewStatus(ExperimentVideoReviewStatus.PENDING)
            .requiredForRelease(true)
            .build();
    ExperimentVideoAsset heroVideo =
        ExperimentVideoAsset.builder()
            .id(6L)
            .experiment(experiment)
            .slot(ExperimentVideoSlot.LANDING_HERO)
            .objective("Explicar PDE")
            .primaryMetric("diagnostico_iniciado")
            .provider("HEYGEN")
            .model("avatar-iv")
            .status(ExperimentVideoStatus.READY)
            .hasAudio(true)
            .visualSourceKey("sofia-musa")
            .reviewStatus(ExperimentVideoReviewStatus.APPROVED)
            .requiredForRelease(true)
            .build();
    given(experimentRepository.findById(39L)).willReturn(Optional.of(experiment));
    given(repository.findById(5L)).willReturn(Optional.of(adVideo));
    given(repository.findByExperimentIdAndVisualSourceKey(39L, "sofia-musa"))
        .willReturn(List.of(adVideo, heroVideo));

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () ->
                service.update(
                    39L,
                    5L,
                    new UpdateExperimentVideoAssetRequest(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        ExperimentVideoReviewStatus.APPROVED,
                        null,
                        "aprovador@marketinghub.local",
                        null,
                        null,
                        null,
                        null,
                        null)));

    assertThat(exception.getStatusCode().value()).isEqualTo(400);
    assertThat(exception.getReason()).contains("mesma origem visual");
  }

  /** Garante que a listagem retorna os vídeos registrados para o experimento. */
  @Test
  void shouldListVideoAssetsForExperiment() {
    Experiment experiment = Experiment.builder().id(39L).build();
    ExperimentVideoAsset videoAsset =
        ExperimentVideoAsset.builder()
            .id(5L)
            .experiment(experiment)
            .slot(ExperimentVideoSlot.FORM_EXPLAINER)
            .objective("Reduzir duvida")
            .primaryMetric("form_submit_rate")
            .provider("VEO")
            .model("veo-3.1-generate-preview")
            .status(ExperimentVideoStatus.READY)
            .reviewStatus(ExperimentVideoReviewStatus.APPROVED)
            .requiredForRelease(true)
            .build();
    given(experimentRepository.findById(39L)).willReturn(Optional.of(experiment));
    given(repository.findByExperimentIdOrderByCreatedAtDesc(39L)).willReturn(List.of(videoAsset));

    List<ExperimentVideoAssetDto> result = service.list(39L);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).slot()).isEqualTo(ExperimentVideoSlot.FORM_EXPLAINER);
  }

  /** Garante que a biblioteca operacional lista vídeos de diferentes experimentos. */
  @Test
  void shouldListAllVideoAssetsForOperationalLibrary() {
    Experiment firstExperiment = Experiment.builder().id(39L).build();
    Experiment secondExperiment = Experiment.builder().id(40L).build();
    ExperimentVideoAsset firstVideo =
        ExperimentVideoAsset.builder()
            .id(5L)
            .experiment(firstExperiment)
            .slot(ExperimentVideoSlot.FORM_EXPLAINER)
            .objective("Reduzir duvida")
            .primaryMetric("form_submit_rate")
            .provider("VEO")
            .model("veo-3.1-generate-preview")
            .status(ExperimentVideoStatus.READY)
            .reviewStatus(ExperimentVideoReviewStatus.APPROVED)
            .requiredForRelease(true)
            .build();
    ExperimentVideoAsset secondVideo =
        ExperimentVideoAsset.builder()
            .id(6L)
            .experiment(secondExperiment)
            .slot(ExperimentVideoSlot.AD)
            .objective("Aumentar CTR")
            .primaryMetric("ctr")
            .provider("LUMA")
            .model("ray-3.2")
            .status(ExperimentVideoStatus.PLANNED)
            .reviewStatus(ExperimentVideoReviewStatus.PENDING)
            .requiredForRelease(true)
            .build();
    given(repository.findAllByOrderByCreatedAtDesc()).willReturn(List.of(firstVideo, secondVideo));

    List<ExperimentVideoAssetDto> result = service.listAll();

    assertThat(result).hasSize(2);
    assertThat(result).extracting(ExperimentVideoAssetDto::experimentId).containsExactly(39L, 40L);
  }
}

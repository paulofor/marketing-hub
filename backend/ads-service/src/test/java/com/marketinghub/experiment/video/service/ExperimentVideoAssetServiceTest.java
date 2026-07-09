package com.marketinghub.experiment.video.service;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.LandingPage;
import com.marketinghub.experiment.video.ExperimentVideoAsset;
import com.marketinghub.experiment.video.ExperimentVideoReviewStatus;
import com.marketinghub.experiment.video.ExperimentVideoSlot;
import com.marketinghub.experiment.video.ExperimentVideoStatus;
import com.marketinghub.experiment.video.dto.CreateExperimentVideoAssetRequest;
import com.marketinghub.experiment.video.dto.ExperimentVideoAssetDto;
import com.marketinghub.experiment.video.dto.RequestExperimentVeoVideoRequest;
import com.marketinghub.experiment.video.dto.UpdateExperimentVideoAssetRequest;
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
import com.marketinghub.salesvideo.SalesVideoProfile;
import com.marketinghub.salesvideo.dto.SalesVideoJobDto;
import com.marketinghub.salesvideo.dto.SalesVideoProfileDto;
import com.marketinghub.salesvideo.service.SalesVideoService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * Valida o registro de vídeos como ativos comerciais vinculados ao experimento.
 */
@ExtendWith(MockitoExtension.class)
class ExperimentVideoAssetServiceTest {
    @Mock
    private ExperimentVideoAssetRepository repository;
    @Mock
    private ExperimentRepository experimentRepository;
    @Mock
    private SalesVideoProfileRepository profileRepository;
    @Mock
    private SalesVideoJobRepository jobRepository;
    @Mock
    private AssetRepository assetRepository;
    @Mock
    private LandingVideoSlotRepository landingVideoSlotRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private LandingPageRepository landingPageRepository;
    @Mock
    private SalesVideoService salesVideoService;

    private ExperimentVideoAssetService service;

    /** Inicializa o serviço com repositórios simulados para testes unitários. */
    @BeforeEach
    void setUp() {
        service = new ExperimentVideoAssetService(
                repository,
                experimentRepository,
                profileRepository,
                jobRepository,
                assetRepository,
                landingVideoSlotRepository,
                productRepository,
                landingPageRepository,
                salesVideoService);
    }

    /** Garante que um vídeo novo recebe estados padrão quando criado para o experimento. */
    @Test
    void shouldCreatePlannedVideoAssetForExperiment() {
        Experiment experiment = Experiment.builder().id(39L).build();
        given(experimentRepository.findById(39L)).willReturn(Optional.of(experiment));
        given(repository.save(any(ExperimentVideoAsset.class))).willAnswer(invocation -> {
            ExperimentVideoAsset saved = invocation.getArgument(0);
            saved.setId(7L);
            return saved;
        });
        CreateExperimentVideoAssetRequest request = new CreateExperimentVideoAssetRequest(
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
                15,
                "9:16",
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

    /** Garante que o Hub cria o fluxo VEO completo a partir do experimento. */
    @Test
    void shouldRequestVeoRenderFromExperiment() {
        MarketNiche niche = MarketNiche.builder().id(31L).name("Beleza").build();
        Experiment experiment = Experiment.builder()
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
        RequestExperimentVeoVideoRequest request = new RequestExperimentVeoVideoRequest(
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
                "VEO",
                SalesVideoExecutionMode.TEST,
                "time@marketinghub.io",
                true);
        given(experimentRepository.findById(61L)).willReturn(Optional.of(experiment));
        given(productRepository.findFirstByMarketNiche_IdOrderByCreatedAtDesc(31L)).willReturn(Optional.of(product));
        given(landingPageRepository.findByExperimentId(61L)).willReturn(List.of());
        given(salesVideoService.createProfile(any(), any())).willReturn(profileDto);
        given(salesVideoService.requestRender(any(), any())).willReturn(jobDto);
        given(profileRepository.findById(12L)).willReturn(Optional.of(profile));
        given(jobRepository.findById(10108L)).willReturn(Optional.of(job));
        given(repository.save(any(ExperimentVideoAsset.class))).willAnswer(invocation -> {
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
    }

    /** Garante que a landing de outro experimento não pode contaminar o aprendizado do funil atual. */
    @Test
    void shouldRejectLandingVideoSlotFromAnotherExperiment() {
        Experiment experiment = Experiment.builder().id(39L).build();
        Experiment anotherExperiment = Experiment.builder().id(40L).build();
        LandingPage landingPage = LandingPage.builder().id(3L).experiment(anotherExperiment).build();
        LandingVideoSlot slot = LandingVideoSlot.builder().id(12L).landingPage(landingPage).build();
        given(experimentRepository.findById(39L)).willReturn(Optional.of(experiment));
        given(landingVideoSlotRepository.findById(12L)).willReturn(Optional.of(slot));
        CreateExperimentVideoAssetRequest request = new CreateExperimentVideoAssetRequest(
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
                15,
                "9:16",
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
        ExperimentVideoAsset videoAsset = ExperimentVideoAsset.builder()
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

        ExperimentVideoAssetDto dto = service.update(39L, 5L, new UpdateExperimentVideoAssetRequest(
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
                null,
                null,
                null,
                null,
                ExperimentVideoReviewStatus.APPROVED,
                null,
                null,
                null,
                null,
                null));

        assertThat(dto.status()).isEqualTo(ExperimentVideoStatus.READY);
        assertThat(dto.reviewStatus()).isEqualTo(ExperimentVideoReviewStatus.APPROVED);
        assertThat(dto.assetUrl()).isEqualTo("https://cdn.test/video.mp4");
    }

    /** Garante que a listagem retorna os vídeos registrados para o experimento. */
    @Test
    void shouldListVideoAssetsForExperiment() {
        Experiment experiment = Experiment.builder().id(39L).build();
        ExperimentVideoAsset videoAsset = ExperimentVideoAsset.builder()
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
}

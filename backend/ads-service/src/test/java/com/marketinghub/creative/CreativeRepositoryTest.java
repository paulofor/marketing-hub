package com.marketinghub.creative;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.journey.model.JourneyTemplate;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.hypothesis.HypothesisRepository;
import com.marketinghub.repository.jpa.journey.JourneyTemplateRepository;
import com.marketinghub.repository.jpa.niche.MarketNicheRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

/** Valida consultas de criativos aprovados e publicáveis usadas pela prontidão comercial. */
@DataJpaTest
@TestPropertySource(properties = "spring.liquibase.enabled=false")
class CreativeRepositoryTest {

  @Autowired CreativeRepository repository;
  @Autowired ExperimentRepository experimentRepository;
  @Autowired MarketNicheRepository nicheRepository;
  @Autowired HypothesisRepository hypothesisRepository;
  @Autowired JourneyTemplateRepository journeyTemplateRepository;
  @Autowired EntityManager entityManager;

  @Autowired
  com.marketinghub.repository.jpa.experiment.video.ExperimentVideoAssetRepository videoRepository;

  /** Garante que formatos de posicionamento STORY e LINK contem como imagens publicáveis. */
  @Test
  void shouldCountStoryAndLinkFormatsAsUsableImages() {
    Experiment experiment = createExperiment();
    repository.save(
        Creative.builder()
            .experiment(experiment)
            .status(CreativeStatus.READY)
            .format("STORY")
            .imageUrl("https://cdn.example.com/story.jpg")
            .build());
    repository.save(
        Creative.builder()
            .experiment(experiment)
            .status(CreativeStatus.READY)
            .format("LINK")
            .imageUrl("https://cdn.example.com/feed.jpg")
            .build());
    entityManager.flush();
    entityManager.clear();

    assertThat(
            repository.countByExperimentIdAndStatusAndUsableImage(
                experiment.getId(), CreativeStatus.READY))
        .isEqualTo(2L);
    assertThat(
            repository.existsByExperimentIdAndStatusAndUsableMedia(
                experiment.getId(), CreativeStatus.READY))
        .isTrue();
  }

  /** Prova o filtro e a projeção das duas origens sem hidratar auditoria nem outro produto. */
  @Test
  void videoSummaryUsesScalarProjectionsAndExactProductScope() {
    var firstProduct = com.marketinghub.product.Product.builder().name("Produto A").build();
    var secondProduct = com.marketinghub.product.Product.builder().name("Produto B").build();
    entityManager.persist(firstProduct);
    entityManager.persist(secondProduct);
    Experiment first = createExperiment();
    first.setProduct(firstProduct);
    first.setStatus(com.marketinghub.experiment.ExperimentStatus.PLANNED);
    first.setLandingPageHtml("auditoria-nao-exibida-".repeat(50000));
    Experiment second = createExperiment();
    second.setProduct(secondProduct);
    var ancestor =
        repository.save(
            Creative.builder()
                .experiment(first)
                .format("IMAGE")
                .status(CreativeStatus.DRAFT)
                .build());
    var current =
        repository.save(
            Creative.builder()
                .experiment(first)
                .format("VIDEO")
                .status(CreativeStatus.DRAFT)
                .sourceCreative(ancestor)
                .videoUrl("https://media.test/a.mp4")
                .agentReviewJson("parecer-volumoso-".repeat(10000))
                .build());
    repository.save(
        Creative.builder()
            .experiment(second)
            .format("VIDEO")
            .status(CreativeStatus.READY)
            .videoId("outro-produto")
            .build());
    var asset =
        com.marketinghub.experiment.video.ExperimentVideoAsset.builder()
            .experiment(first)
            .slot(com.marketinghub.experiment.video.ExperimentVideoSlot.AD)
            .objective("Teste interno")
            .primaryMetric("TEST")
            .provider("LOCAL")
            .model("TEST")
            .status(com.marketinghub.experiment.video.ExperimentVideoStatus.READY)
            .reviewStatus(com.marketinghub.experiment.video.ExperimentVideoReviewStatus.PENDING)
            .assetUrl("https://media.test/b.mp4")
            .hasAudio(true)
            .requiredForRelease(true)
            .prompt("auditoria-nao-exibida-".repeat(50000))
            .build();
    entityManager.persist(asset);
    entityManager.flush();
    entityManager.clear();

    var rows = repository.findVideoSummarySnapshots(firstProduct.getId(), first.getId());
    var videoRows = videoRepository.findVideoSummarySnapshots(firstProduct.getId(), first.getId());

    assertThat(rows).hasSize(2);
    assertThat(
            com.marketinghub.creative.service.videoreview.CreativeVideoSummarySnapshot
                .eligibleCreatives(rows))
        .singleElement()
        .satisfies(
            creative -> {
              assertThat(creative.getId()).isEqualTo(current.getId());
              assertThat(creative.getSourceCreative().getId()).isEqualTo(ancestor.getId());
            });
    assertThat(videoRows)
        .singleElement()
        .satisfies(
            row -> {
              assertThat(row.id()).isEqualTo(asset.getId());
              assertThat(row.requiredForRelease()).isTrue();
            });
    assertThat(repository.findVideoSummarySnapshots(secondProduct.getId(), first.getId()))
        .isEmpty();
    assertThat(videoRepository.findVideoSummarySnapshots(secondProduct.getId(), first.getId()))
        .isEmpty();
    assertThat(entityManager.unwrap(org.hibernate.Session.class).getStatistics().getEntityCount())
        .isZero();
    assertThat(
            com.marketinghub.creative.service.videoreview.CreativeVideoSummarySnapshot
                .eligibleCreatives(repository.findVideoSummarySnapshots(null, null)))
        .extracting(Creative::getId)
        .containsExactlyElementsOf(
            repository.findVideoCreativesForReview(null, null, null).stream()
                .map(Creative::getId)
                .toList());
    assertThat(videoRepository.findVideoSummarySnapshots(null, null))
        .extracting(
            com.marketinghub.creative.service.videoreview.ExperimentVideoSummarySnapshot::id)
        .containsExactlyInAnyOrderElementsOf(
            videoRepository
                .findReadyExperimentVideosForReview(
                    com.marketinghub.experiment.video.ExperimentVideoStatus.READY, null, null, null)
                .stream()
                .map(com.marketinghub.experiment.video.ExperimentVideoAsset::getId)
                .toList());
  }

  /** Cria o contexto mínimo persistido exigido por um experimento. */
  private Experiment createExperiment() {
    MarketNiche niche = nicheRepository.save(MarketNiche.builder().name("Nail Design").build());
    Hypothesis hypothesis =
        hypothesisRepository.save(
            Hypothesis.builder().marketNiche(niche).title("Amostra personalizada").build());
    JourneyTemplate template =
        journeyTemplateRepository.save(JourneyTemplate.builder().name("Microamostra").build());
    return experimentRepository.save(
        Experiment.builder()
            .name("Experimento de criativo")
            .niche(niche)
            .hypothesisRef(hypothesis)
            .journeyTemplate(template)
            .build());
  }
}

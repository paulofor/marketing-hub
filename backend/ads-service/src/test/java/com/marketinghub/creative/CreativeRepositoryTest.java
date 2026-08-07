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

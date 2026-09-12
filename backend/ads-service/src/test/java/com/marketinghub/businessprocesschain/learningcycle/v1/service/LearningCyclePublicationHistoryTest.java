package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.ads.FacebookAccount;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.experiment.*;
import com.marketinghub.experiment.run.*;
import com.marketinghub.facebookads.*;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRunRepository;
import com.marketinghub.repository.jpa.facebookads.FacebookAdsCampaignRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: comprovar a origem histórica com consultas JPA reais e gates independentes. */
@DataJpaTest
@TestPropertySource(properties = "spring.liquibase.enabled=false")
@Import(LearningCyclePublicationHistory.class)
class LearningCyclePublicationHistoryTest {
  @Autowired private EntityManager entities;
  @Autowired private ExperimentRunRepository runs;
  @Autowired private FacebookAdsCampaignRepository campaigns;
  @Autowired private LearningCyclePublicationHistory history;

  /** Status e rascunhos locais não comprovam publicação, mesmo em experimento encerrado. */
  @Test
  void ignoresStatusAndDraftCampaignsWithoutExternalReceipt() {
    var experiment = experiment(ExperimentPlatform.FACEBOOK);
    experiment.setStatus(ExperimentStatus.USER_STOPPED);
    assertThat(history.find(experiment)).isEmpty();
    campaign(experiment, null);
    campaign(experiment, "   ");
    entities.flush();
    assertThat(history.find(experiment)).isEmpty();
    assertThat(runs.count()).isZero();
  }

  /** Recibo externo da campanha permite aprender sem inventar run ou preflight antigo. */
  @Test
  void adoptsLegacyReceiptAndPreservesMissingRun() {
    var experiment = experiment(ExperimentPlatform.FACEBOOK);
    var campaign = campaign(experiment, "fixture-meta-receipt");
    var proof = history.find(experiment).orElseThrow();
    assertThat(proof.source()).isEqualTo("LEGACY_META_CAMPAIGN");
    assertThat(proof.experimentId()).isEqualTo(experiment.getId());
    assertThat(proof.reference()).isEqualTo("facebook_ads_campaign:" + campaign.getId());
    assertThat(proof.recordedAt()).isEqualTo(campaign.getCreatedAt());
    assertThat(proof.preflightRecorded()).isFalse();
    assertThat(proof.summary()).contains("sem publicação registrada em run/preflight");
    assertThat(runs.count()).isZero();
    assertThat(campaign.getStatus()).isEqualTo(FacebookAdStatus.PAUSED);
  }

  /** Recibos de outro experimento e canal não se tornam prova para o sucessor. */
  @Test
  void isolatesExperimentAndChannel() {
    var legacy = experiment(ExperimentPlatform.FACEBOOK);
    campaign(legacy, "fixture-meta-receipt");
    assertThat(history.find(experiment(ExperimentPlatform.FACEBOOK))).isEmpty();
    var direct = experiment(ExperimentPlatform.DIRECT_ONE_TO_ONE);
    campaign(direct, "fixture-wrong-channel");
    assertThat(history.find(direct)).isEmpty();
  }

  /** Tentativa posterior sem publicação e run de teste não escondem o histórico produtivo. */
  @Test
  void keepsEarlierProductionRunAfterUnpublishedRetry() {
    var experiment = experiment(ExperimentPlatform.FACEBOOK);
    var published =
        run(experiment, 1, ExperimentRunMode.PRODUCTION, Instant.now().minusSeconds(600));
    published.setPreflightCompletedAt(published.getPublishedAt().minusSeconds(10));
    run(experiment, 2, ExperimentRunMode.PRODUCTION, null);
    run(experiment, 3, ExperimentRunMode.TEST, Instant.now());
    entities.flush();
    var proof = history.find(experiment).orElseThrow();
    assertThat(proof.source()).isEqualTo("PRODUCTION_RUN");
    assertThat(proof.reference()).isEqualTo("experiment_run:" + published.getId());
    assertThat(proof.preflightRecorded()).isTrue();
    assertThat(runs.count()).isEqualTo(3);
  }

  /** Run de teste, ainda que tenha data de publicação, não autoriza histórico comercial. */
  @Test
  void ignoresTestOnlyPublication() {
    var experiment = experiment(ExperimentPlatform.FACEBOOK);
    run(experiment, 1, ExperimentRunMode.TEST, Instant.now());
    assertThat(history.find(experiment)).isEmpty();
  }

  /** Histórico comprovado sem preflight deve manter essa limitação explícita. */
  @Test
  void preservesMissingPreflightOnPublishedRun() {
    var experiment = experiment(ExperimentPlatform.FACEBOOK);
    run(experiment, 1, ExperimentRunMode.PRODUCTION, Instant.now());
    var proof = history.find(experiment).orElseThrow();
    assertThat(proof.preflightRecorded()).isFalse();
    assertThat(proof.summary()).contains("sem preflight registrado");
  }

  /** Recibo antigo nunca substitui run, preflight e autorização da nova publicação. */
  @Test
  void legacyEvidenceNeverBypassesSuccessorPublicationGate() {
    var experiment = experiment(ExperimentPlatform.FACEBOOK);
    experiment.setStatus(ExperimentStatus.RUNNING);
    experiment.setMediaSpendLimit(BigDecimal.valueOf(100));
    campaign(experiment, "fixture-meta-receipt");
    var gate =
        new LearningCycleEvidence(
            mock(BusinessProcessActivityInstanceRepository.class),
            runs,
            new LearningCycleJson(new ObjectMapper()),
            history,
            mock(com.marketinghub.repository.jpa.product.ProductRepository.class));
    var cycle = new LearningSalesCycle();
    cycle.setExperimentId(experiment.getId());
    cycle.setBudgetLimitBrl(BigDecimal.valueOf(100));
    cycle.setWindowStart(Instant.now().minusSeconds(600));
    cycle.setWindowEnd(Instant.now().plusSeconds(600));
    Instant authorized = Instant.now().minusSeconds(120);
    assertThat(gate.operated(experiment)).isTrue();
    assertThatThrownBy(() -> gate.publication(cycle, experiment, authorized))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("preflight");
    var run = run(experiment, 1, ExperimentRunMode.PRODUCTION, Instant.now());
    assertThatThrownBy(() -> gate.publication(cycle, experiment, authorized))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("preflight");
    run.setPreflightCompletedAt(authorized.minusSeconds(10));
    entities.flush();
    assertThatCode(() -> gate.publication(cycle, experiment, authorized))
        .doesNotThrowAnyException();
    assertThatThrownBy(() -> gate.publication(cycle, experiment, Instant.now().plusSeconds(1)))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("depois da autorização");
  }

  /** Persiste o contexto mínimo de um experimento local, sem credenciais ou aquisição real. */
  private Experiment experiment(ExperimentPlatform platform) {
    var niche = MarketNiche.builder().name("Nicho segregado " + UUID.randomUUID()).build();
    entities.persist(niche);
    var hypothesis = Hypothesis.builder().marketNiche(niche).title("Hipótese local").build();
    entities.persist(hypothesis);
    var experiment =
        Experiment.builder()
            .niche(niche)
            .hypothesisRef(hypothesis)
            .name("Experimento local")
            .platform(platform)
            .status(ExperimentStatus.PLANNED)
            .build();
    entities.persist(experiment);
    entities.flush();
    return experiment;
  }

  /** Persiste um recibo ou rascunho Meta controlado, com conta sem token. */
  private FacebookAdsCampaign campaign(Experiment experiment, String externalId) {
    var account = FacebookAccount.builder().name("Conta local sem credencial").build();
    entities.persist(account);
    var campaign = new FacebookAdsCampaign();
    campaign.setId(UUID.randomUUID().toString());
    campaign.setExternalId(externalId);
    campaign.setExperiment(experiment);
    campaign.setFacebookAccount(account);
    campaign.setAdAccountId("fixture-only");
    campaign.setName("Campanha local");
    campaign.setObjective("OUTCOME_TRAFFIC");
    campaign.setBudgetMode(BudgetMode.ADSET);
    campaign.setStatus(FacebookAdStatus.PAUSED);
    entities.persist(campaign);
    entities.flush();
    return campaign;
  }

  /** Persiste uma tentativa controlada para distinguir publicação, teste e repetição. */
  private ExperimentRun run(
      Experiment experiment, int number, ExperimentRunMode mode, Instant publishedAt) {
    var run = new ExperimentRun();
    run.setExperiment(experiment);
    run.setRunNumber(number);
    run.setMode(mode);
    run.setStatus(ExperimentRunStatus.COMPLETED);
    run.setEvidenceValidity(ExperimentEvidenceValidity.NOT_EVALUATED);
    run.setDataQualityStatus(ExperimentRunDataQualityStatus.UNKNOWN);
    run.setStopPolicy(ExperimentRunStopPolicy.MANUAL_ONLY);
    run.setRequestedAt(Instant.now().minusSeconds(1200));
    run.setPublishedAt(publishedAt);
    return runs.saveAndFlush(run);
  }
}

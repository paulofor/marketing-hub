package com.marketinghub.creative.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.FixtureUtils;
import com.marketinghub.ads.AdsServiceApplication;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.history.ExperimentHistoryEventService;
import com.marketinghub.experiment.video.ExperimentVideoAsset;
import com.marketinghub.experiment.video.ExperimentVideoReviewStatus;
import com.marketinghub.experiment.video.ExperimentVideoSlot;
import com.marketinghub.experiment.video.ExperimentVideoStatus;
import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experiment.video.ExperimentVideoAssetRepository;
import java.util.Map;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Homologa seleção, segregação, concorrência, histórico e gates reais de aprovação do vídeo. */
@SpringBootTest(
    classes = AdsServiceApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "spring.datasource.url=jdbc:h2:mem:videoCreative;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
      "spring.datasource.driverClassName=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.liquibase.enabled=false"
    })
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class VideoCreativeControllerTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired FixtureUtils fixtures;
  @Autowired ExperimentRepository experiments;
  @Autowired ExperimentVideoAssetRepository videos;
  @Autowired CreativeRepository creatives;
  @Autowired com.marketinghub.repository.jpa.product.ProductRepository products;

  @org.springframework.boot.test.mock.mockito.MockBean
  com.marketinghub.experiment.funnel.ExperimentFunnelService commercialMetrics;

  @SpyBean ExperimentHistoryEventService history;
  @org.springframework.boot.test.web.server.LocalServerPort int port;
  Experiment experiment;
  ExperimentVideoAsset approved;
  ExperimentVideoAsset rejected;

  /** Cria um experimento e duas versões fictícias em banco exclusivo da homologação. */
  @BeforeEach
  void setup() {
    // Não há compras nem schema externo de pagamentos na homologação de publicação.
    org.mockito.Mockito.when(commercialMetrics.approvedRevenue(any()))
        .thenReturn(java.math.BigDecimal.ZERO);
    org.mockito.Mockito.when(commercialMetrics.summarizeLandingAnalytics(any()))
        .thenReturn(
            new com.marketinghub.experiment.funnel.service.analytics.ExperimentLandingAnalyticsDto(
                0,
                0,
                0,
                0,
                0,
                0,
                null,
                java.util.List.of(),
                java.util.List.of(),
                java.util.List.of(),
                null,
                null,
                java.util.List.of()));
    experiment = fixtures.createAndSaveExperiment(fixtures.createAndSaveNiche());
    experiment.setStatus(ExperimentStatus.PLANNED);
    experiment.setFollowUpActionUrl("https://landing.test/qa-internal");
    experiment = experiments.save(experiment);
    rejected = saveVideo(ExperimentVideoReviewStatus.REJECTED);
    approved = saveVideo(ExperimentVideoReviewStatus.APPROVED);
  }

  /** Remove apenas a simulação de falha do serviço de histórico. */
  @AfterEach
  void clearFailure() {
    reset(history);
  }

  /** Monta mídia fictícia com áudio e proveniência local, sem acessar provedores reais. */
  private ExperimentVideoAsset saveVideo(ExperimentVideoReviewStatus review) {
    return videos.save(
        ExperimentVideoAsset.builder()
            .experiment(experiment)
            .slot(ExperimentVideoSlot.AD)
            .objective("Homologação local")
            .primaryMetric("CHECKOUT_CLICK")
            .provider("LOCAL_QA")
            .model("test-double")
            .status(ExperimentVideoStatus.READY)
            .reviewStatus(review)
            .hasAudio(true)
            .requiredForRelease(true)
            .assetUrl("https://media.test/" + experiment.getId() + "/" + review + ".mp4")
            .thumbnailUrl("https://media.test/qa.png")
            .rejectionReason(
                review == ExperimentVideoReviewStatus.REJECTED ? "Tremor comprovado" : null)
            .build());
  }

  /** Envia a mesma seleção comercial pela rota HTTP real da interface. */
  private ResultActions select(String tenant, long id) throws Exception {
    return mvc.perform(
        post("/api/experiments/" + experiment.getId() + "/video-assets/" + id + "/creative")
            .header("X-Tenant-ID", tenant)
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                json.writeValueAsString(
                    Map.of(
                        "headline",
                        "Primeiro ajuste",
                        "primaryText",
                        "Use o que já tem.",
                        "description",
                        "Dia 1 gratuito",
                        "replacesVideoAssetId",
                        rejected.getId()))));
  }

  /** Confirma criação idempotente, histórico preservado e aprovação técnica separada da humana. */
  @Test
  void selectsApprovedVideoAndPreservesAllReviewGates() throws Exception {
    String body =
        select("default", approved.getId())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.agentReviewStatus").value("PENDING"))
            .andExpect(jsonPath("$.videoUrl").value(approved.getAssetUrl()))
            .andExpect(jsonPath("$.destinationUrl").value(experiment.getFollowUpActionUrl()))
            .andReturn()
            .getResponse()
            .getContentAsString();
    long creativeId = json.readTree(body).get("id").asLong();
    select("default", approved.getId())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(creativeId));
    assertThat(creatives.findByExperimentId(experiment.getId())).hasSize(1);
    assertThat(history.list(experiment.getId())).hasSize(1);
    var old = videos.findById(rejected.getId()).orElseThrow();
    assertThat(old.isRequiredForRelease()).isFalse();
    assertThat(old.getReviewStatus()).isEqualTo(ExperimentVideoReviewStatus.REJECTED);
    assertThat(old.getRejectionReason()).isEqualTo("Tremor comprovado");
    mvc.perform(
            patch("/api/creatives/" + creativeId + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"READY\"}"))
        .andExpect(status().is4xxClientError());
    mvc.perform(
            post("/api/internal/creatives/" + creativeId + "/agent-review/result")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
          {"decision":"APPROVED","attentionScore":90,"clarityScore":90,"desireScore":90,
           "credibilityScore":90,"actionScore":90,"summary":"QA_INTERNAL: parecer simulado",
           "copyAssessment":"QA_INTERNAL: mensagem clara.","commercialAestheticAssessment":"QA_INTERNAL: mídia aprovada.",
           "destinationIntegrationAssessment":"QA_INTERNAL: destino coerente.",
           "model":"LOCAL_TEST_DOUBLE","requestJson":"{}","responseJson":"{}","costUsd":0}
          """))
        .andExpect(status().isOk());
    assertThat(creatives.findById(creativeId).orElseThrow().getStatus().name()).isEqualTo("DRAFT");
    mvc.perform(
            patch("/api/creatives/" + creativeId + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"READY\"}"))
        .andExpect(status().isOk());
    assertThat(experiments.findById(experiment.getId()).orElseThrow().getStatus())
        .isEqualTo(ExperimentStatus.PLANNED);
    select("default", approved.getId())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.agentReviewStatus").value("APPROVED"));
  }

  /** Impede corrida de dois cliques criando apenas um anúncio e uma decisão auditável. */
  @Test
  void concurrentRequestsCreateOneCreative() throws Exception {
    try (var threads = Executors.newFixedThreadPool(2)) {
      var first =
          threads.submit(
              () -> select("default", approved.getId()).andReturn().getResponse().getStatus());
      var second =
          threads.submit(
              () -> select("default", approved.getId()).andReturn().getResponse().getStatus());
      assertThat(first.get()).isEqualTo(200);
      assertThat(second.get()).isEqualTo(200);
    }
    assertThat(creatives.findByExperimentId(experiment.getId())).hasSize(1);
    assertThat(history.list(experiment.getId())).hasSize(1);
  }

  /** Rejeita tenant diferente sem alterar mídia, histórico ou anúncio. */
  @Test
  void rejectsAnotherTenant() throws Exception {
    select("other-tenant", approved.getId()).andExpect(status().isNotFound());
    assertThat(creatives.findByExperimentId(experiment.getId())).isEmpty();
  }

  /** Rejeita mídia de outro experimento, mesmo com identificador válido. */
  @Test
  void rejectsAnotherExperiment() throws Exception {
    var other = fixtures.createAndSaveExperiment(fixtures.createAndSaveNiche());
    approved.setExperiment(other);
    videos.save(approved);
    select("default", approved.getId()).andExpect(status().isNotFound());
  }

  /** Bloqueia a reutilização da própria versão reprovada. */
  @Test
  void rejectsUnapprovedVideo() throws Exception {
    select("default", rejected.getId()).andExpect(status().isConflict());
  }

  /** Exige áudio confirmado antes de cadastrar o anúncio premium. */
  @Test
  void rejectsSilentVideo() throws Exception {
    approved.setHasAudio(false);
    videos.save(approved);
    select("default", approved.getId()).andExpect(status().isConflict());
  }

  /** Mantém bloqueios de hero independentes da troca do anúncio. */
  @Test
  void rejectsReplacementOfAnotherSlot() throws Exception {
    rejected.setSlot(ExperimentVideoSlot.LANDING_HERO);
    videos.save(rejected);
    select("default", approved.getId()).andExpect(status().isConflict());
    assertThat(videos.findById(rejected.getId()).orElseThrow().isRequiredForRelease()).isTrue();
  }

  /** Impede alterar os ativos depois da liberação de campanha. */
  @Test
  void rejectsReleasedExperiment() throws Exception {
    experiment.setFacebookReleaseRequestedAt(java.time.Instant.now());
    experiments.save(experiment);
    select("default", approved.getId()).andExpect(status().isConflict());
  }

  /** Reverte a seleção e o anúncio se o registro auditável falhar. */
  @Test
  void rollsBackIfHistoryFails() throws Exception {
    doThrow(new IllegalStateException("QA_INTERNAL: falha simulada"))
        .when(history)
        .create(eq(experiment.getId()), any());
    select("default", approved.getId()).andExpect(status().is5xxServerError());
    assertThat(creatives.findByExperimentId(experiment.getId())).isEmpty();
    assertThat(videos.findById(rejected.getId()).orElseThrow().isRequiredForRelease()).isTrue();
  }

  /** Valida campos obrigatórios na fronteira HTTP antes de qualquer persistência. */
  @Test
  void rejectsBlankCopy() throws Exception {
    mvc.perform(
            post("/api/experiments/"
                    + experiment.getId()
                    + "/video-assets/"
                    + approved.getId()
                    + "/creative")
                .header("X-Tenant-ID", "default")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"headline\":\" \",\"primaryText\":\"\"}"))
        .andExpect(status().isBadRequest());
    assertThat(creatives.findByExperimentId(experiment.getId())).isEmpty();
  }

  /** Permite completar uma substituição explícita posterior sem criar outro anúncio. */
  @Test
  void completesReplacementForExistingCreative() throws Exception {
    mvc.perform(
            post("/api/experiments/"
                    + experiment.getId()
                    + "/video-assets/"
                    + approved.getId()
                    + "/creative")
                .header("X-Tenant-ID", "default")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"headline\":\"Primeiro ajuste\",\"primaryText\":\"Use o que já tem.\",\"description\":\"Dia 1 gratuito\"}"))
        .andExpect(status().isOk());
    select("default", approved.getId()).andExpect(status().isOk());
    select("default", approved.getId()).andExpect(status().isOk());
    assertThat(creatives.findByExperimentId(experiment.getId())).hasSize(1);
    assertThat(history.list(experiment.getId())).hasSize(2);
    assertThat(videos.findById(rejected.getId()).orElseThrow().isRequiredForRelease()).isFalse();
  }

  /** Recusa mídia sem arquivo publicável e preserva o bloqueio anterior. */
  @Test
  void rejectsMissingVideoUrl() throws Exception {
    approved.setAssetUrl(null);
    videos.save(approved);
    select("default", approved.getId()).andExpect(status().isBadRequest());
    assertThat(creatives.findByExperimentId(experiment.getId())).isEmpty();
  }

  /** Exercita desktop e celulares no frontend compilado contra o backend e banco reais locais. */
  @Test
  @org.junit.jupiter.api.condition.EnabledIfSystemProperty(
      named = "videoCreative.browser",
      matches = "true")
  void browserJourneyUsesRealLocalBackend() throws Exception {
    var args =
        json.writeValueAsString(
            Map.of(
                "backend",
                "http://127.0.0.1:" + port,
                "experimentId",
                experiment.getId(),
                "videoId",
                approved.getId(),
                "rejectedId",
                rejected.getId()));
    var browserLog = new java.io.File("target/video-creative-browser.log");
    var process =
        new ProcessBuilder("node", "frontend/scripts/validate-video-creative-ui.cjs", args)
            .directory(new java.io.File("../.."))
            .redirectErrorStream(true)
            .redirectOutput(browserLog)
            .start();
    try {
      assertThat(process.waitFor(90, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
      assertThat(process.exitValue())
          .withFailMessage(java.nio.file.Files.readString(browserLog.toPath()))
          .isZero();
    } finally {
      if (process.isAlive()) process.destroyForcibly();
    }
    assertThat(creatives.findByExperimentId(experiment.getId())).hasSize(1);
    assertThat(history.list(experiment.getId())).hasSize(1);
    assertThat(videos.findById(rejected.getId()).orElseThrow().isRequiredForRelease()).isFalse();
  }

  /** Homologa a recuperação da falha pela interface contra persistência e gates reais locais. */
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"desktop", "iphone", "pixel"})
  @org.junit.jupiter.api.condition.EnabledIfSystemProperty(
      named = "publicationRecovery.browser",
      matches = "true")
  void browserRecoversFailedPublicationWithIndependentCopyReview(String device) throws Exception {
    var product =
        products.save(
            com.marketinghub.product.Product.builder()
                .name("QA recuperação")
                .slug("qa-recovery-" + experiment.getId())
                .marketNiche(experiment.getNiche())
                .build());
    experiment.setProduct(product);
    experiment.setStatus(ExperimentStatus.FAILED);
    experiment = experiments.save(experiment);
    var original =
        creatives.save(
            com.marketinghub.creative.Creative.builder()
                .experiment(experiment)
                .headline("QA copy longa")
                .primaryText("x".repeat(202))
                .description("Primeiro ajuste")
                .cta("LEARN_MORE")
                .format("VIDEO")
                .status(com.marketinghub.creative.CreativeStatus.READY)
                .agentReviewStatus(com.marketinghub.creative.CreativeAgentReviewStatus.APPROVED)
                .videoUrl(approved.getAssetUrl())
                .imageUrl(approved.getThumbnailUrl())
                .destinationUrl(experiment.getFollowUpActionUrl())
                .instagramUserId("qa-instagram")
                .build());
    var args =
        json.writeValueAsString(
            Map.of(
                "backend",
                "http://127.0.0.1:" + port,
                "experimentId",
                experiment.getId(),
                "name",
                experiment.getName(),
                "creativeId",
                original.getId(),
                "device",
                device));
    var output = new java.io.File("target/publication-recovery-" + device + ".log");
    var process =
        new ProcessBuilder("node", "frontend/scripts/validate-publication-recovery.cjs", args)
            .directory(new java.io.File("../.."))
            .redirectErrorStream(true)
            .redirectOutput(output)
            .start();
    try {
      assertThat(process.waitFor(150, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
      assertThat(process.exitValue())
          .withFailMessage(java.nio.file.Files.readString(output.toPath()))
          .isZero();
    } finally {
      if (process.isAlive()) process.destroyForcibly();
    }
    var saved = creatives.findByExperimentId(experiment.getId());
    assertThat(saved).hasSize(2);
    assertThat(saved.stream().filter(c -> c.getSourceCreative() != null).findFirst().orElseThrow())
        .satisfies(
            c -> {
              assertThat(c.getStatus()).isEqualTo(com.marketinghub.creative.CreativeStatus.READY);
              assertThat(c.getVideoUrl()).isEqualTo(approved.getAssetUrl());
              assertThat(c.getInstagramUserId()).isEqualTo("qa-instagram");
            });
    assertThat(experiments.findById(experiment.getId()).orElseThrow().getStatus())
        .isEqualTo(ExperimentStatus.FAILED);
  }
}

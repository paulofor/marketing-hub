package com.marketinghub.creative.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.FixtureUtils;
import com.marketinghub.ads.AdsServiceApplication;
import com.marketinghub.creative.*;
import com.marketinghub.experiment.*;
import com.marketinghub.experiment.video.*;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experiment.video.ExperimentVideoAssetRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/** Homologa o build da tela contra HTTP e persistência reais locais, sem rede ou gasto externos. */
@SpringBootTest(
    classes = AdsServiceApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfEnvironmentVariable(named = "VIDEO_REVIEW_BROWSER", matches = "true")
class VideoReviewBrowserTest {
  @LocalServerPort int port;
  @Autowired FixtureUtils fixtures;
  @Autowired ExperimentRepository experiments;
  @Autowired CreativeRepository creatives;
  @Autowired ExperimentVideoAssetRepository videos;
  @Autowired ProductRepository products;

  /**
   * Exercita leitura, filtros, decisões e isolamento de produto em três viewports com dados
   * sintéticos.
   */
  @Test
  void validatesLocalFrontendAndBackendTogether() throws Exception {
    Product currentProduct =
        products.save(Product.builder().name("Produto local com vídeo").build());
    Product unrelatedProduct =
        products.save(Product.builder().name("Produto local sem vídeo").build());
    Experiment current = fixtures.createAndSaveExperiment(fixtures.createAndSaveNiche());
    current.setProduct(currentProduct);
    experiments.saveAndFlush(current);
    Experiment closed = fixtures.createAndSaveExperiment(fixtures.createAndSaveNiche());
    closed.setStatus(ExperimentStatus.INVALIDATED);
    experiments.saveAndFlush(closed);
    var pending =
        videos.saveAndFlush(
            ExperimentVideoAsset.builder()
                .experiment(current)
                .slot(ExperimentVideoSlot.AD)
                .objective("Nova peça local")
                .primaryMetric("checkout")
                .script("Demonstração de homologação")
                .provider("LOCAL_FIXTURE")
                .model("fixture")
                .status(ExperimentVideoStatus.READY)
                .assetUrl("https://fixture.invalid/demo.mp4")
                .hasAudio(true)
                .reviewStatus(ExperimentVideoReviewStatus.PENDING)
                .requiredForRelease(true)
                .build());
    videos.saveAndFlush(
        ExperimentVideoAsset.builder()
            .experiment(current)
            .slot(ExperimentVideoSlot.AD)
            .objective("Candidata opcional local")
            .primaryMetric("checkout")
            .script("Demonstração opcional de homologação")
            .provider("LOCAL_FIXTURE")
            .model("fixture")
            .status(ExperimentVideoStatus.READY)
            .assetUrl("https://fixture.invalid/optional.mp4")
            .hasAudio(true)
            .reviewStatus(ExperimentVideoReviewStatus.PENDING)
            .requiredForRelease(false)
            .build());
    creatives.saveAndFlush(
        Creative.builder()
            .experiment(current)
            .format("VIDEO")
            .headline("Anúncio em ajuste")
            .primaryText("Copy de teste")
            .videoUrl("https://fixture.invalid/demo.mp4")
            .status(CreativeStatus.DRAFT)
            .agentReviewStatus(CreativeAgentReviewStatus.ADJUST)
            .build());
    creatives.saveAndFlush(
        Creative.builder()
            .experiment(closed)
            .format("VIDEO")
            .headline("Tentativa histórica")
            .primaryText("Copy de teste")
            .videoUrl("https://fixture.invalid/demo.mp4")
            .status(CreativeStatus.DRAFT)
            .agentReviewStatus(CreativeAgentReviewStatus.FAILED)
            .build());
    Path repository = Path.of(System.getProperty("user.dir")).resolve("../..").normalize();
    Path output = repository.resolve(".codex/video-review-validation");
    Files.createDirectories(output);
    ProcessBuilder builder =
        new ProcessBuilder(
            "node", repository.resolve("frontend/scripts/validate-video-review.cjs").toString());
    builder.environment().put("VIDEO_REVIEW_BACKEND", "http://127.0.0.1:" + port);
    builder.environment().put("VIDEO_REVIEW_PRODUCT", currentProduct.getId().toString());
    builder
        .environment()
        .put("VIDEO_REVIEW_UNRELATED_PRODUCT", unrelatedProduct.getId().toString());
    builder.environment().put("VIDEO_REVIEW_EXPERIMENT", current.getId().toString());
    builder.environment().put("VIDEO_REVIEW_ASSET", pending.getId().toString());
    builder.redirectErrorStream(true).redirectOutput(output.resolve("browser.log").toFile());
    Process process = builder.start();
    boolean finished = process.waitFor(Duration.ofMinutes(2).toMillis(), TimeUnit.MILLISECONDS);
    if (!finished) process.destroyForcibly();
    assertThat(finished).as("Navegador encerra sem espera indefinida").isTrue();
    assertThat(process.exitValue()).as(Files.readString(output.resolve("browser.log"))).isZero();
    assertThat(videos.findById(pending.getId()).orElseThrow().getReviewStatus())
        .isEqualTo(ExperimentVideoReviewStatus.APPROVED);
  }
}

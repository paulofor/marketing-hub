package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.pde.vega.privateprototype.v1.controller.VegaPrivateController;
import com.marketinghub.pde.vega.privateprototype.v1.service.VegaPrivateService;
import com.marketinghub.repository.jpa.vega.*;
import jakarta.persistence.EntityManagerFactory;
import java.time.Instant;
import java.util.Map;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.web.bind.annotation.*;

/**
 * Responsabilidade: unir ciclo e experiência reais na sandbox com os fornecedores de mídia
 * simulados.
 */
@TestConfiguration
@Profile("learning-cycles-fixture")
@Import({
  LearningCycleVideoBinding.class,
  LearningCyclePrototypeContext.class,
  VegaPrivateService.class,
  VegaPrivateController.class,
  LearningCycleVideoContinuationFixtures.Controller.class
})
public class LearningCycleVideoContinuationFixtures {
  static boolean vegaEnabled;

  /** Publica sessões reais e segregadas na mesma transação do backend local. */
  @Bean
  VegaPrivateSessionRepository vegaSessions(EntityManagerFactory factory) {
    return new JpaRepositoryFactory(SharedEntityManagerCreator.createSharedEntityManager(factory))
        .getRepository(VegaPrivateSessionRepository.class);
  }

  /** Publica a fila consumida pelo worker real com o provedor OpenAI simulado. */
  @Bean
  VegaAdjustmentExecutionRepository vegaExecutions(EntityManagerFactory factory) {
    return new JpaRepositoryFactory(SharedEntityManagerCreator.createSharedEntityManager(factory))
        .getRepository(VegaAdjustmentExecutionRepository.class);
  }

  /**
   * Responsabilidade: controlar dados de teste e chamar a integração canônica sem expor endpoints
   * produtivos.
   */
  @RestController
  @Profile("learning-cycles-fixture")
  static class Controller {
    private final LearningCycleService service;

    /** Recebe o serviço real que aplica locks, eventos e transições no MySQL local. */
    Controller(LearningCycleService service) {
      this.service = service;
    }

    /** Acrescenta aos test doubles os metadados dos bytes sintéticos gerados pelo ffmpeg. */
    @PostMapping("/fixture/video-continuation/media")
    Map<String, Object> media(@RequestBody JsonNode body) {
      vegaEnabled = true;
      for (String key : java.util.List.of("campaignVideo", "heroVideo")) {
        var input = body.path(key);
        var video = LearningCycleVideoFixtures.VIDEOS.get(input.path("assetId").asLong());
        if (video == null) throw new IllegalArgumentException("Ativo precisa pertencer à fixture.");
        video.getExperiment().setProduct(LearningCycleLocalApplication.product(91001L));
        video.getExperiment().setFollowUpActionUrl(null);
        video.setAssetUrl(input.path("assetUrl").asText());
        video.setHlsPlaybackUrl(input.path("hlsPlaybackUrl").asText());
        video.setDurationSeconds(input.path("durationSeconds").asInt());
        video.setReviewedAt(Instant.now());
        video.setReviewedBy("Homologação sintética local");
        video.setResponseJson(input.path("metadata").toString());
      }
      LearningCycleVideoFixtures.CREATIVES.clear();
      LearningCycleVideoFixtures.SLOTS.clear();
      return Map.of("simulatedProvider", true, "commercialDestination", false);
    }

    /**
     * Invoca o mesmo comando interno usado pelo coordenador, inclusive em requisições concorrentes.
     */
    @PostMapping("/fixture/video-continuation/{productId}/{cycleId}/integrate")
    Map<String, Boolean> integrate(@PathVariable Long productId, @PathVariable Long cycleId) {
      service.integrateApprovedVideos(productId, cycleId);
      return Map.of("accepted", true);
    }

    /** Restaura somente o status da mídia sintética após o teste de revogação. */
    @PostMapping("/fixture/video-continuation/restore-approval")
    Map<String, Boolean> restore(@RequestBody JsonNode body) {
      LearningCycleVideoFixtures.VIDEOS
          .get(body.path("assetId").asLong())
          .setReviewStatus(com.marketinghub.experiment.video.ExperimentVideoReviewStatus.APPROVED);
      return Map.of("restoredSyntheticApproval", true);
    }
  }
}

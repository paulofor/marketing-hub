package com.marketinghub.creative.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.creative.Creative;
import com.marketinghub.creative.CreativeAgentReviewStatus;
import com.marketinghub.creative.CreativeStatus;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.product.Product;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar a prova de adoção comercial usada pelo revisor de anúncios. */
class CreativeCommercialLineageEvidenceServiceTest {
  private final CreativeCommercialLineageEvidenceService service =
      new CreativeCommercialLineageEvidenceService();

  /** Reconhece landing, checkout e mídia herdados de um criativo aprovado do antecessor. */
  @Test
  void verifiesDirectSuccessorAndReusedCreative() {
    Product product = Product.builder().id(7L).build();
    Hypothesis hypothesis = Hypothesis.builder().id(UUID.randomUUID()).build();
    Experiment sourceExperiment =
        Experiment.builder()
            .id(88L)
            .product(product)
            .hypothesisRef(hypothesis)
            .followUpActionUrl("https://shop.test/flows/exp-88")
            .commercialCheckoutUrl("https://checkout.test/capella")
            .build();
    Experiment targetExperiment =
        Experiment.builder()
            .id(94L)
            .product(product)
            .hypothesisRef(hypothesis)
            .sourceExperiment(sourceExperiment)
            .followUpActionUrl("https://shop.test/flows/exp-88")
            .commercialCheckoutUrl("https://checkout.test/capella")
            .build();
    Creative source =
        Creative.builder()
            .id(523L)
            .experiment(sourceExperiment)
            .format("IMAGE")
            .imageUrl("https://cdn.test/exp-88-story.png")
            .destinationUrl("https://shop.test/flows/exp-88")
            .status(CreativeStatus.READY)
            .agentReviewStatus(CreativeAgentReviewStatus.APPROVED)
            .reviewedAt(Instant.parse("2026-09-24T12:00:00Z"))
            .build();
    Creative reused =
        Creative.builder()
            .id(531L)
            .experiment(targetExperiment)
            .sourceCreative(source)
            .format("IMAGE")
            .imageUrl("https://cdn.test/exp-88-story.png")
            .destinationUrl("https://shop.test/flows/exp-88")
            .build();

    var evidence = service.resolve(reused, reused.getDestinationUrl());

    assertThat(evidence.verificationStatus()).isEqualTo("VERIFIED");
    assertThat(evidence.adoptedSourceExperimentId()).isEqualTo(88L);
    assertThat(evidence.sourceCreativeId()).isEqualTo(523L);
    assertThat(evidence.reusedCreative()).isTrue();
    assertThat(evidence.destinationUrlMatched()).isTrue();
    assertThat(evidence.checkoutUrlMatched()).isTrue();
    assertThat(evidence.mediaUrlMatched()).isTrue();
  }

  /** Bloqueia a alegação de herança quando o checkout do sucessor diverge da origem. */
  @Test
  void keepsLineageIncompleteWhenCheckoutDiverges() {
    Product product = Product.builder().id(7L).build();
    Hypothesis hypothesis = Hypothesis.builder().id(UUID.randomUUID()).build();
    Experiment sourceExperiment =
        Experiment.builder()
            .id(88L)
            .product(product)
            .hypothesisRef(hypothesis)
            .followUpActionUrl("https://shop.test/flows/exp-88")
            .commercialCheckoutUrl("https://checkout.test/capella")
            .build();
    Experiment targetExperiment =
        Experiment.builder()
            .id(94L)
            .product(product)
            .hypothesisRef(hypothesis)
            .sourceExperiment(sourceExperiment)
            .commercialCheckoutUrl("https://checkout.test/outro")
            .build();
    Creative video = Creative.builder().experiment(targetExperiment).format("VIDEO").build();

    var evidence = service.resolve(video, "https://shop.test/flows/exp-88");

    assertThat(evidence.verificationStatus()).isEqualTo("INCOMPLETE");
    assertThat(evidence.checkoutUrlMatched()).isFalse();
  }
}

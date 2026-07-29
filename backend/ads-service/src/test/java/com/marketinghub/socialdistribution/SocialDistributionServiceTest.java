package com.marketinghub.socialdistribution;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.product.ProductRepository;
import com.marketinghub.socialdistribution.dto.SocialDistributionDtos.CreateSocialVideoPublicationRequest;
import com.marketinghub.socialdistribution.dto.SocialDistributionDtos.RecordSocialPublicationMetricRequest;
import com.marketinghub.socialdistribution.dto.SocialDistributionDtos.SaveSocialAccountRequest;
import com.marketinghub.socialdistribution.dto.SocialDistributionDtos.SocialAccountResponse;
import com.marketinghub.socialdistribution.dto.SocialDistributionDtos.SocialPublicationMetricResponse;
import com.marketinghub.socialdistribution.dto.SocialDistributionDtos.SocialVideoPublicationResponse;
import com.marketinghub.socialdistribution.service.SocialDistributionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

/** Responsabilidade: validar regras comerciais da fila de distribuição orgânica. */
@DataJpaTest
@Import(SocialDistributionService.class)
@TestPropertySource(properties = "spring.liquibase.enabled=false")
class SocialDistributionServiceTest {
  @Autowired private SocialDistributionService service;
  @Autowired private ProductRepository productRepository;

  /** Bloqueia publicação quando a conta YouTube ainda não está conectada via OAuth. */
  @Test
  void blocksPublicationWhenYoutubeAccountRequiresSetup() {
    Product product = productRepository.save(Product.builder().slug("musa").name("MUSA").build());
    SocialAccountResponse account =
        service.createAccount(
            new SaveSocialAccountRequest(
                SocialPlatform.YOUTUBE,
                "Canal MUSA",
                "@musa",
                null,
                SocialConnectionMode.OAUTH,
                SocialAccountStatus.SETUP_REQUIRED,
                null));

    SocialVideoPublicationResponse publication =
        service.createPublication(
            new CreateSocialVideoPublicationRequest(
                product.getId(),
                null,
                account.id(),
                SocialPlatform.YOUTUBE,
                SocialVideoFormat.YOUTUBE_SHORT,
                "Short MUSA",
                "Legenda",
                "#Shorts",
                "https://cdn.example.test/video.mp4",
                null));

    SocialVideoPublicationResponse queued = service.queuePublication(publication.id());

    assertThat(queued.status()).isEqualTo(SocialVideoPublicationStatus.BLOCKED);
    assertThat(queued.failureReason()).contains("OAuth");
  }

  /** Enfileira publicação YouTube quando há conta conectada e vídeo público. */
  @Test
  void queuesYoutubePublicationWhenAccountIsConnected() {
    Product product = productRepository.save(Product.builder().slug("musa").name("MUSA").build());
    SocialAccountResponse account =
        service.createAccount(
            new SaveSocialAccountRequest(
                SocialPlatform.YOUTUBE,
                "Canal MUSA",
                "@musa",
                "channel-123",
                SocialConnectionMode.OAUTH,
                SocialAccountStatus.CONNECTED,
                null));

    SocialVideoPublicationResponse publication =
        service.createPublication(
            new CreateSocialVideoPublicationRequest(
                product.getId(),
                null,
                account.id(),
                SocialPlatform.YOUTUBE,
                SocialVideoFormat.YOUTUBE_SHORT,
                "Short MUSA",
                "Legenda",
                "#Shorts",
                "https://cdn.example.test/video.mp4",
                null));

    SocialVideoPublicationResponse queued = service.queuePublication(publication.id());

    assertThat(queued.status()).isEqualTo(SocialVideoPublicationStatus.QUEUED);
    assertThat(queued.publishPayloadJson()).contains("YOUTUBE", "YOUTUBE_SHORT");
  }

  /** Registra métricas sem aceitar valores negativos na leitura comercial. */
  @Test
  void normalizesNegativeMetricValues() {
    Product product = productRepository.save(Product.builder().slug("musa").name("MUSA").build());
    SocialVideoPublicationResponse publication =
        service.createPublication(
            new CreateSocialVideoPublicationRequest(
                product.getId(),
                null,
                null,
                SocialPlatform.YOUTUBE,
                SocialVideoFormat.YOUTUBE_SHORT,
                "Short MUSA",
                "Legenda",
                "#Shorts",
                "https://cdn.example.test/video.mp4",
                null));

    SocialPublicationMetricResponse metric =
        service.recordMetric(
            publication.id(),
            new RecordSocialPublicationMetricRequest(-10L, 3L, 1L, 0L, -5L, "{}", null));

    assertThat(metric.views()).isZero();
    assertThat(metric.clicks()).isZero();
    assertThat(metric.likes()).isEqualTo(3L);
  }
}

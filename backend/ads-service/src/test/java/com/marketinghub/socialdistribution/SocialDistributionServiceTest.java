package com.marketinghub.socialdistribution;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.product.ProductRepository;
import com.marketinghub.socialdistribution.dto.SocialDistributionDtos.CreateSocialGrowthContentRequest;
import com.marketinghub.socialdistribution.dto.SocialDistributionDtos.CreateSocialGrowthPlanRequest;
import com.marketinghub.socialdistribution.dto.SocialDistributionDtos.CreateSocialVideoPublicationRequest;
import com.marketinghub.socialdistribution.dto.SocialDistributionDtos.MarkSocialVideoFailedRequest;
import com.marketinghub.socialdistribution.dto.SocialDistributionDtos.MarkSocialVideoPublishedRequest;
import com.marketinghub.socialdistribution.dto.SocialDistributionDtos.RecordSocialPublicationMetricRequest;
import com.marketinghub.socialdistribution.dto.SocialDistributionDtos.SaveSocialAccountRequest;
import com.marketinghub.socialdistribution.dto.SocialDistributionDtos.SocialAccountResponse;
import com.marketinghub.socialdistribution.dto.SocialDistributionDtos.SocialGrowthContentResponse;
import com.marketinghub.socialdistribution.dto.SocialDistributionDtos.SocialGrowthPlanResponse;
import com.marketinghub.socialdistribution.dto.SocialDistributionDtos.SocialPublicationMetricResponse;
import com.marketinghub.socialdistribution.dto.SocialDistributionDtos.SocialVideoPublicationResponse;
import com.marketinghub.socialdistribution.service.SocialDistributionService;
import java.math.BigDecimal;
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
    assertThat(queued.socialAccountExternalAccountId()).isEqualTo("channel-123");
  }

  /** Registra sucesso e falha do worker para fechar o ciclo operacional da fila. */
  @Test
  void recordsWorkerPublicationResult() {
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

    SocialVideoPublicationResponse publishing = service.markPublishing(publication.id());
    SocialVideoPublicationResponse failed =
        service.markFailed(
            publication.id(), new MarkSocialVideoFailedRequest("YOUTUBE_ERROR", "quota"));
    SocialVideoPublicationResponse published =
        service.markPublished(
            publication.id(),
            new MarkSocialVideoPublishedRequest("https://youtube.com/watch?v=abc", "abc", null));

    assertThat(publishing.status()).isEqualTo(SocialVideoPublicationStatus.PUBLISHING);
    assertThat(failed.status()).isEqualTo(SocialVideoPublicationStatus.FAILED);
    assertThat(failed.failureReason()).contains("YOUTUBE_ERROR", "quota");
    assertThat(published.status()).isEqualTo(SocialVideoPublicationStatus.PUBLISHED);
    assertThat(published.publishedUrl()).contains("youtube.com");
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

  /** Gera atribuição no backend e exige aprovação antes de vincular a pauta à publicação. */
  @Test
  void createsTrackedPlanAndRequiresHumanApproval() {
    Product product =
        productRepository.save(Product.builder().slug("agenda-cheia").name("Agenda Cheia").build());
    SocialGrowthPlanResponse plan =
        service.createGrowthPlan(
            new CreateSocialGrowthPlanRequest(
                product.getId(),
                "Piloto YouTube",
                "Manicures com horários vazios",
                "Conteúdo aplicável gera visitas qualificadas",
                "Gerar leads atribuídos",
                "Baixar amostra",
                "https://agenda.example.test/amostra",
                "Agenda Cheia YouTube",
                null,
                null));
    SocialGrowthContentResponse content =
        service.createGrowthContent(
            plan.id(),
            new CreateSocialGrowthContentRequest(
                SocialGrowthContentType.SHORT,
                "Recuperação de clientes",
                "Mensagem para preencher horário vazio",
                "DESCOBERTA",
                null,
                null));

    assertThat(content.status()).isEqualTo(SocialGrowthContentStatus.DRAFT);
    assertThat(content.trackingUrl())
        .contains("utm_source=youtube", "utm_campaign=agenda-cheia-youtube");
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                service.createPublication(
                    new CreateSocialVideoPublicationRequest(
                        product.getId(),
                        content.id(),
                        null,
                        null,
                        SocialPlatform.YOUTUBE,
                        SocialVideoFormat.YOUTUBE_SHORT,
                        null,
                        null,
                        "#Shorts",
                        "https://cdn.example.test/video.mp4",
                        null)))
        .hasMessageContaining("aprovação humana");

    service.approveGrowthContent(plan.id(), content.id());
    SocialVideoPublicationResponse publication =
        service.createPublication(
            new CreateSocialVideoPublicationRequest(
                product.getId(),
                content.id(),
                null,
                null,
                SocialPlatform.YOUTUBE,
                SocialVideoFormat.YOUTUBE_SHORT,
                null,
                null,
                "#Shorts",
                "https://cdn.example.test/video.mp4",
                null));

    assertThat(publication.growthContentId()).isEqualTo(content.id());
    assertThat(publication.title()).isEqualTo(content.topic());
    assertThat(publication.caption()).contains(content.trackingUrl());
  }

  /** Recomenda continuar somente quando a métrica atribuída contém sinal comercial real. */
  @Test
  void recommendsContinueFromAttributedLead() {
    Product product = productRepository.save(Product.builder().slug("musa").name("MUSA").build());
    SocialGrowthPlanResponse plan =
        service.createGrowthPlan(
            new CreateSocialGrowthPlanRequest(
                product.getId(),
                "Piloto MUSA",
                "Mulheres buscando clareza visual",
                "Demonstração prática aquece a intenção",
                "Gerar leads",
                "Iniciar diagnóstico",
                "https://musa.example.test",
                "musa-youtube",
                null,
                null));
    SocialGrowthContentResponse content =
        service.createGrowthContent(
            plan.id(),
            new CreateSocialGrowthContentRequest(
                SocialGrowthContentType.LONG_VIDEO,
                "Microajustes",
                "O que sua imagem comunica",
                "AQUECIMENTO",
                null,
                null));
    service.approveGrowthContent(plan.id(), content.id());
    SocialVideoPublicationResponse publication =
        service.createPublication(
            new CreateSocialVideoPublicationRequest(
                product.getId(),
                content.id(),
                null,
                null,
                SocialPlatform.YOUTUBE,
                SocialVideoFormat.YOUTUBE_SHORT,
                null,
                null,
                null,
                "https://cdn.example.test/musa.mp4",
                null));
    service.recordMetric(
        publication.id(),
        new RecordSocialPublicationMetricRequest(
            500L,
            200L,
            new BigDecimal("22.5"),
            20L,
            3L,
            30L,
            4L,
            2L,
            15L,
            12L,
            2L,
            1L,
            0L,
            BigDecimal.ZERO,
            "{}",
            null));

    SocialGrowthPlanResponse result = service.listGrowthPlans(product.getId()).getFirst();
    assertThat(result.performance().decision()).isEqualTo("CONTINUAR");
    assertThat(result.performance().leads()).isEqualTo(2L);
    assertThat(result.performance().salesApproved()).isZero();
  }
}

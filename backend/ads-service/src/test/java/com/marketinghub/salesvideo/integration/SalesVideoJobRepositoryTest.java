package com.marketinghub.salesvideo.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.product.ProductRepository;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoJobRepository;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoProfileRepository;
import com.marketinghub.salesvideo.SalesVideoExecutionMode;
import com.marketinghub.salesvideo.SalesVideoJob;
import com.marketinghub.salesvideo.SalesVideoJobType;
import com.marketinghub.salesvideo.SalesVideoKind;
import com.marketinghub.salesvideo.SalesVideoProfile;
import com.marketinghub.salesvideo.SalesVideoProviderFamily;
import com.marketinghub.salesvideo.SalesVideoStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

/** Valida a exclusividade e a retomada controlada da lease dos jobs de vídeo. */
@DataJpaTest
@TestPropertySource(properties = "spring.liquibase.enabled=false")
class SalesVideoJobRepositoryTest {

  @Autowired private SalesVideoJobRepository jobRepository;
  @Autowired private SalesVideoProfileRepository profileRepository;
  @Autowired private ProductRepository productRepository;

  /** Filtra contratos incompatíveis no banco antes do limite sem ocultar trabalho normal. */
  @Test
  void excludesRecoveryFromLegacyWorkerBeforePagination() {
    var product =
        productRepository.save(
            Product.builder().slug("fixture-recovery-query").name("Fixture").build());
    var profile =
        profileRepository.save(
            SalesVideoProfile.builder()
                .product(product)
                .videoKind(SalesVideoKind.HERO)
                .title("Recuperação isolada")
                .status(SalesVideoStatus.VIDEO_REQUESTED)
                .build());
    for (int i = 0; i < 3; i++) {
      jobRepository.saveAndFlush(
          SalesVideoJob.builder()
              .profile(profile)
              .providerFamily(SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE)
              .jobType(SalesVideoJobType.POST_PRODUCTION)
              .status(SalesVideoStatus.VIDEO_REQUESTED)
              .requestedAt(Instant.parse("2026-10-10T00:00:00Z").plusSeconds(i))
              .metadataJson(
                  i == 0
                      ? "{\"preservedNarration\":{}}"
                      : i == 1 ? "{\"captionText\":\"Texto normal\"}" : null)
              .build());
    }
    var page =
        org.springframework.data.domain.PageRequest.of(
            0, 1, org.springframework.data.domain.Sort.by("requestedAt"));
    var legacy =
        jobRepository.findAll(
            com.marketinghub.salesvideo.service.VideoNarrationRecovery.consumableBy(null), page);
    assertThat(legacy.getTotalElements()).isEqualTo(2);
    assertThat(legacy.getContent())
        .singleElement()
        .satisfies(job -> assertThat(job.getMetadataJson()).contains("Texto normal"));
    var capable =
        jobRepository.findAll(
            com.marketinghub.salesvideo.service.VideoNarrationRecovery.consumableBy(
                com.marketinghub.salesvideo.service.VideoNarrationRecovery.CONTRACT),
            page);
    assertThat(capable.getTotalElements()).isEqualTo(3);
    assertThat(capable.getContent())
        .singleElement()
        .satisfies(job -> assertThat(job.getMetadataJson()).contains("preservedNarration"));
  }

  /** Reserva uma vez, bloqueia concorrência, retoma lease vencida e preserva estado terminal. */
  @Test
  void claimsOnlyAvailableOrStaleJobs() {
    Product product =
        productRepository.save(Product.builder().slug("vega-claim-test").name("Vega").build());
    SalesVideoProfile profile =
        profileRepository.save(
            SalesVideoProfile.builder()
                .product(product)
                .videoKind(SalesVideoKind.HERO)
                .title("Product UGC premium")
                .status(SalesVideoStatus.VIDEO_REQUESTED)
                .build());
    SalesVideoJob job =
        jobRepository.saveAndFlush(
            SalesVideoJob.builder()
                .profile(profile)
                .providerFamily(SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE)
                .executionMode(SalesVideoExecutionMode.PRODUCTION)
                .jobType(SalesVideoJobType.POST_PRODUCTION)
                .status(SalesVideoStatus.VIDEO_REQUESTED)
                .requestedAt(Instant.parse("2026-09-05T06:00:00Z"))
                .build());
    Instant firstClaim = Instant.parse("2026-09-05T06:10:00Z");

    int claimed =
        jobRepository.claimIfAvailable(
            job.getId(),
            SalesVideoStatus.VIDEO_REQUESTED,
            SalesVideoStatus.VIDEO_PROCESSING,
            firstClaim,
            firstClaim.minusSeconds(600));
    int duplicate =
        jobRepository.claimIfAvailable(
            job.getId(),
            SalesVideoStatus.VIDEO_REQUESTED,
            SalesVideoStatus.VIDEO_PROCESSING,
            firstClaim.plusSeconds(60),
            firstClaim.minusSeconds(540));
    int reclaimed =
        jobRepository.claimIfAvailable(
            job.getId(),
            SalesVideoStatus.VIDEO_REQUESTED,
            SalesVideoStatus.VIDEO_PROCESSING,
            firstClaim.plusSeconds(660),
            firstClaim.plusSeconds(60));
    SalesVideoJob ready = jobRepository.findById(job.getId()).orElseThrow();
    ready.setStatus(SalesVideoStatus.VIDEO_READY);
    jobRepository.saveAndFlush(ready);
    int terminalClaim =
        jobRepository.claimIfAvailable(
            job.getId(),
            SalesVideoStatus.VIDEO_REQUESTED,
            SalesVideoStatus.VIDEO_PROCESSING,
            firstClaim.plusSeconds(1320),
            firstClaim.plusSeconds(720));

    assertThat(claimed).isOne();
    assertThat(duplicate).isZero();
    assertThat(reclaimed).isOne();
    assertThat(terminalClaim).isZero();
    assertThat(jobRepository.findById(job.getId()))
        .get()
        .extracting(SalesVideoJob::getStatus)
        .isEqualTo(SalesVideoStatus.VIDEO_READY);
  }
}

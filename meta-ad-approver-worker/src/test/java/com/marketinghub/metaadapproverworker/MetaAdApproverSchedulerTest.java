package com.marketinghub.metaadapproverworker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar concorrência e isolamento entre as filas da entrada única de Têmis. */
class MetaAdApproverSchedulerTest {
  /** Confirma que três revisões reservadas começam juntas e recebem callback independente. */
  @Test
  void processesClaimedBatchConcurrently() throws Exception {
    MetaAdApproverBackendClient backend = mock(MetaAdApproverBackendClient.class);
    MetaAdApproverCodexRunner runner = mock(MetaAdApproverCodexRunner.class);
    MetaAdApproverProperties properties = new MetaAdApproverProperties();
    CountDownLatch started = new CountDownLatch(3);
    CountDownLatch release = new CountDownLatch(1);
    AtomicInteger active = new AtomicInteger();
    AtomicInteger maximumActive = new AtomicInteger();
    List<MetaAdReviewJob> jobs = List.of(job(278L), job(279L), job(280L));

    when(backend.claimPending(3)).thenReturn(jobs);
    when(runner.run(any()))
        .thenAnswer(
            invocation -> {
              int current = active.incrementAndGet();
              maximumActive.accumulateAndGet(current, Math::max);
              started.countDown();
              assertThat(started.await(2, TimeUnit.SECONDS)).isTrue();
              release.countDown();
              release.await(2, TimeUnit.SECONDS);
              active.decrementAndGet();
              return Map.of("decision", "APPROVED");
            });

    scheduler(backend, runner, properties).processPending();

    assertThat(maximumActive).hasValue(3);
    verify(backend).report(278L, Map.of("decision", "APPROVED"));
    verify(backend).report(279L, Map.of("decision", "APPROVED"));
    verify(backend).report(280L, Map.of("decision", "APPROVED"));
  }

  /**
   * Confirma que uma falha de callback não impede o tratamento independente do restante do lote.
   */
  @Test
  void isolatesCallbackFailureWithinClaimedBatch() throws Exception {
    MetaAdApproverBackendClient backend = mock(MetaAdApproverBackendClient.class);
    MetaAdApproverCodexRunner runner = mock(MetaAdApproverCodexRunner.class);
    MetaAdApproverProperties properties = new MetaAdApproverProperties();
    List<MetaAdReviewJob> jobs = List.of(job(339L), job(342L));
    Map<String, Object> approved = Map.of("decision", "APPROVED");

    when(backend.claimPending(3)).thenReturn(jobs);
    when(runner.run(any())).thenReturn(approved);
    org.mockito.Mockito.doThrow(new IllegalStateException("backend indisponível"))
        .when(backend)
        .report(339L, approved);

    scheduler(backend, runner, properties).processPending();

    verify(backend).fail(any(Long.class), any(IllegalStateException.class));
    verify(backend).report(342L, approved);
  }

  /** Confirma que indisponibilidade da fila não escapa do ciclo nem afeta o health do revisor. */
  @Test
  void containsFailureWhileClaimingReviewQueue() {
    MetaAdApproverBackendClient backend = mock(MetaAdApproverBackendClient.class);
    MetaAdApproverCodexRunner runner = mock(MetaAdApproverCodexRunner.class);
    MetaAdApproverProperties properties = new MetaAdApproverProperties();
    when(backend.claimPending(3)).thenThrow(new IllegalStateException("backend indisponível"));

    assertThatCode(() -> scheduler(backend, runner, properties).processPending())
        .doesNotThrowAnyException();
  }

  /** Confirma que a revisão da Biblioteca continua paralela ao gate dos criativos. */
  @Test
  void processesLibraryReviewWithoutWaitingForCreativeReviews() throws Exception {
    MetaAdApproverBackendClient backend = mock(MetaAdApproverBackendClient.class);
    MetaAdApproverCodexRunner runner = mock(MetaAdApproverCodexRunner.class);
    MetaAdApproverProperties properties = new MetaAdApproverProperties();
    TemisLibraryImageReviewProcessor libraryReview = mock(TemisLibraryImageReviewProcessor.class);
    CountDownLatch libraryProcessed = new CountDownLatch(1);

    when(backend.claimPending(3)).thenReturn(List.of(job(506L)));
    org.mockito.Mockito.doAnswer(
            invocation -> {
              libraryProcessed.countDown();
              return null;
            })
        .when(libraryReview)
        .processPending();
    when(runner.run(any()))
        .thenAnswer(
            invocation -> {
              assertThat(libraryProcessed.await(2, TimeUnit.SECONDS)).isTrue();
              return Map.of("decision", "APPROVED");
            });

    new MetaAdApproverScheduler(backend, runner, properties, libraryReview).processPending();

    verify(libraryReview).processPending();
    verify(backend).report(506L, Map.of("decision", "APPROVED"));
  }

  /** Cria um job mínimo segregado pelo experimento homologado. */
  private MetaAdReviewJob job(Long creativeId) {
    return new MetaAdReviewJob(creativeId, 88L, Map.of());
  }

  /** Monta o scheduler pelo mesmo construtor canônico usado pelo Spring em produção. */
  private MetaAdApproverScheduler scheduler(
      MetaAdApproverBackendClient backend,
      MetaAdApproverCodexRunner runner,
      MetaAdApproverProperties properties) {
    return new MetaAdApproverScheduler(
        backend, runner, properties, mock(TemisLibraryImageReviewProcessor.class));
  }
}

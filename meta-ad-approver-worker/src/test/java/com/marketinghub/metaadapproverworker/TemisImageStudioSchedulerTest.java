package com.marketinghub.metaadapproverworker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/** Responsabilidade: homologar o isolamento das filas produtivas do Estúdio Visual de Têmis. */
class TemisImageStudioSchedulerTest {

  /** Confirma que criação e retrabalho começam sem esperar uma pela outra. */
  @Test
  void processesProductionQueuesConcurrently() throws Exception {
    TemisImageStudioProcessor imageStudio = mock(TemisImageStudioProcessor.class);
    TemisCreativeImprovementProcessor creativeImprovement =
        mock(TemisCreativeImprovementProcessor.class);
    CountDownLatch started = new CountDownLatch(2);

    doAnswer(
            invocation -> {
              started.countDown();
              assertThat(started.await(2, TimeUnit.SECONDS)).isTrue();
              return null;
            })
        .when(imageStudio)
        .processPending();
    doAnswer(
            invocation -> {
              started.countDown();
              assertThat(started.await(2, TimeUnit.SECONDS)).isTrue();
              return null;
            })
        .when(creativeImprovement)
        .processPending();

    new TemisImageStudioScheduler(imageStudio, creativeImprovement).processPending();

    verify(imageStudio).processPending();
    verify(creativeImprovement).processPending();
  }

  /** Confirma que falha na criação não impede o retrabalho de criativos. */
  @Test
  void isolatesFailureBetweenProductionQueues() {
    TemisImageStudioProcessor imageStudio = mock(TemisImageStudioProcessor.class);
    TemisCreativeImprovementProcessor creativeImprovement =
        mock(TemisCreativeImprovementProcessor.class);
    doThrow(new IllegalStateException("GPT indisponível")).when(imageStudio).processPending();

    new TemisImageStudioScheduler(imageStudio, creativeImprovement).processPending();

    verify(creativeImprovement).processPending();
  }
}

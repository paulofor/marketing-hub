package com.marketinghub.metaadapproverworker;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

/** Responsabilidade: homologar o isolamento das filas produtivas do Estúdio Visual de Têmis. */
class TemisImageStudioSchedulerTest {

  /** Confirma que criação termina antes do retrabalho para serializar uploads grandes. */
  @Test
  void processesProductionQueuesSequentially() {
    TemisImageStudioProcessor imageStudio = mock(TemisImageStudioProcessor.class);
    TemisCreativeImprovementProcessor creativeImprovement =
        mock(TemisCreativeImprovementProcessor.class);

    new TemisImageStudioScheduler(imageStudio, creativeImprovement).processPending();

    var ordered = inOrder(imageStudio, creativeImprovement);
    ordered.verify(imageStudio).processPending();
    ordered.verify(creativeImprovement).processPending();
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

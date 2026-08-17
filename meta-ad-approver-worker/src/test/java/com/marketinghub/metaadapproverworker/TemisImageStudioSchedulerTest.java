package com.marketinghub.metaadapproverworker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
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

  /** Preserva o canvas 9:16 do Story ao materializar a correção aprendida. */
  @Test
  void keepsStoryFormatInCreativeImprovement() throws Exception {
    TemisCreativeImprovementProcessor processor =
        new TemisCreativeImprovementProcessor(
            new MetaAdApproverProperties(), mock(TemisImageStudioOpenAiClient.class));
    Method toImageJob =
        TemisCreativeImprovementProcessor.class.getDeclaredMethod(
            "toImageJob", Map.class, Long.class);
    toImageJob.setAccessible(true);
    Map<String, Object> correction =
        Map.of(
            "experimentId",
            88,
            "format",
            "STORY",
            "revisedImagePrompt",
            "Preservar o produto",
            "referenceImageUrls",
            List.of());

    TemisImageStudioJob job = (TemisImageStudioJob) toImageJob.invoke(processor, correction, 522L);

    assertThat(job.size()).isEqualTo("1152x2048");
    assertThat(job.label()).contains("STORY");
  }
}

package com.marketinghub.agenttask;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Evita revisão histórica reutilizada para uma nova edição e repetição paga sem mudança. */
class CreativeVideoReviewReadinessTest {
  /** A mesma atividade reabre para hash novo e permanece satisfeita após seu parecer próprio. */
  @Test
  void reopensOnlyWhenTheFinalMediaNeedsItsOwnReview() {
    var evidence = mock(CreativeFinalVideoEvidence.class);
    var targets = mock(AgentTaskTargetContextProvider.class);
    var service = new CreativeVideoReviewReadiness(evidence, targets);
    var process = new BusinessProcessDefinition(); process.setProcessCode("creative-production-approval");
    var activity = new BusinessProcessActivityDefinition(); activity.setActivityId("customer");
    var target = new AgentTaskTargetResponse("experiment:91102",91102L,91010L,"fixture","Produto","Produto","v3",null,null,null,null,null);
    when(targets.resolve("experiment:91102",process.getProcessCode())).thenReturn(Optional.of(target));
    assertThat(service.supports(process,activity)).isTrue();
    when(evidence.needsReview(target)).thenReturn(true,false);
    assertThat(service.requiresFreshExecution(process,activity,null,"experiment:91102")).isTrue();
    assertThat(service.requiresFreshExecution(process,activity,null,"experiment:91102")).isFalse();
    activity.setActivityId("nonAudiovisual");
    assertThat(service.supports(process,activity)).isFalse();
  }
}

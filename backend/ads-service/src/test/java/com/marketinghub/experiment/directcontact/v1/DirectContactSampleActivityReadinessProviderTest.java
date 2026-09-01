package com.marketinghub.experiment.directcontact.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Responsabilidade: provar que Hermes só custa novamente quando a amostra direta está pronta. */
class DirectContactSampleActivityReadinessProviderTest {

  /** Bloqueia nova tarefa enquanto faltarem contatos reais. */
  @Test
  void shouldBlockHermesUntilTargetIsReached() {
    ExperimentDirectContactService service = mock(ExperimentDirectContactService.class);
    when(service.isDirectOneToOne(89L)).thenReturn(true);
    when(service.getSample(89L)).thenReturn(sample(14, 15));
    DirectContactSampleActivityReadinessProvider provider =
        new DirectContactSampleActivityReadinessProvider(service);

    var readiness = provider.readiness(process(), activity(), null, "experiment:89");

    assertThat(readiness.ready()).isFalse();
    assertThat(readiness.reason()).contains("14 de 15").contains("faltam 1");
  }

  /** Libera uma única revisão quando a evidência já alcançou a meta. */
  @Test
  void shouldAllowHermesAtTarget() {
    ExperimentDirectContactService service = mock(ExperimentDirectContactService.class);
    when(service.isDirectOneToOne(89L)).thenReturn(true);
    when(service.getSample(89L)).thenReturn(sample(15, 15));
    DirectContactSampleActivityReadinessProvider provider =
        new DirectContactSampleActivityReadinessProvider(service);

    var readiness = provider.readiness(process(), activity(), null, "experiment:89");

    assertThat(readiness.ready()).isTrue();
    assertThat(readiness.reason()).contains("15 de 15");
  }

  /** Preserva a operação existente dos experimentos que não usam canal individual. */
  @Test
  void shouldNotApplyDirectSampleToPaidChannel() {
    ExperimentDirectContactService service = mock(ExperimentDirectContactService.class);
    when(service.isDirectOneToOne(90L)).thenReturn(false);
    DirectContactSampleActivityReadinessProvider provider =
        new DirectContactSampleActivityReadinessProvider(service);

    var readiness = provider.readiness(process(), activity(), null, "experiment:90");

    assertThat(readiness.ready()).isTrue();
    verify(service, never()).getSample(90L);
  }

  /** Reconhece somente a atividade de acumulação do processo correto. */
  @Test
  void shouldSupportOnlyDirectSampleActivity() {
    DirectContactSampleActivityReadinessProvider provider =
        new DirectContactSampleActivityReadinessProvider(
            mock(ExperimentDirectContactService.class));

    assertThat(provider.supports(process(), activity())).isTrue();
    BusinessProcessActivityDefinition other = activity();
    other.setActivityId("task-1");
    assertThat(provider.supports(process(), other)).isFalse();
  }

  /** Cria a resposta mínima de amostra usada pelo gate. */
  private ExperimentDirectContactSampleResponse sample(long recorded, int target) {
    return new ExperimentDirectContactSampleResponse(
        89L,
        "DIRECT_ONE_TO_ONE",
        "RUNNING",
        target,
        recorded,
        Math.max(0, target - recorded),
        recorded >= target,
        recorded >= target ? "READY_FOR_HERMES_REVIEW" : "ACCUMULATING_CONSENTED_SAMPLE",
        List.of());
  }

  /** Cria o processo operacional mínimo. */
  private BusinessProcessDefinition process() {
    BusinessProcessDefinition process = new BusinessProcessDefinition();
    process.setProcessCode("operacao-otimizacao-experimento");
    return process;
  }

  /** Cria a atividade de acumulação mínima. */
  private BusinessProcessActivityDefinition activity() {
    BusinessProcessActivityDefinition activity = new BusinessProcessActivityDefinition();
    activity.setActivityId("task-2");
    return activity;
  }
}

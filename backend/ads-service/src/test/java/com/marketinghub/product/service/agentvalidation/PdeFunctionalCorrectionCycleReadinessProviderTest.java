package com.marketinghub.product.service.agentvalidation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Responsabilidade: prevenir diagnóstico pago no ciclo rejeitado sem quebrar correção técnica. */
class PdeFunctionalCorrectionCycleReadinessProviderTest {
  private final AgentTaskRepository tasks = mock(AgentTaskRepository.class);
  private final PdeFunctionalCorrectionCycleReadinessProvider provider =
      new PdeFunctionalCorrectionCycleReadinessProvider(tasks, new ObjectMapper());

  /** Exige sucessor no caso original e em outra identidade, sem depender de nome de produto. */
  @ParameterizedTest
  @ValueSource(longs = {7L, 18L})
  void blocksFunctionalCorrectionBeforeAnotherModelCall(long productId) {
    String source = "experiment:" + (9000 + productId);
    when(tasks.findPdeValidationTaskSnapshots(source, "pde-construction-approval"))
        .thenReturn(
            List.of(
                proof(676, "psiqueAdherent", "FUNCTIONAL_ADJUSTMENT"),
                proof(677, "prototypeCorrection", "FUNCTIONAL_ADJUSTMENT")));

    var readiness =
        provider.readiness(
            process(),
            activity("prototypeCorrection"),
            Product.builder().id(productId).build(),
            source);

    assertThat(readiness.ready()).isFalse();
    assertThat(readiness.reason())
        .contains("#676", "sucessores vinculados", "nova candidata", "referência atual");
  }

  /** Preserva falha técnica mais recente e não confunde diagnóstico de correção com parecer. */
  @Test
  void preservesCurrentTechnicalFailure() {
    when(tasks.findPdeValidationTaskSnapshots("experiment:9123", "pde-construction-approval"))
        .thenReturn(
            List.of(
                proof(676, "psiqueAdherent", "FUNCTIONAL_ADJUSTMENT"),
                proof(678, "technicalHomologation", "TECHNICAL_FAILURE")));
    assertThat(
            provider
                .readiness(
                    process(),
                    activity("prototypeCorrection"),
                    Product.builder().id(7L).build(),
                    "experiment:9123")
                .ready())
        .isTrue();
  }

  /** Não bloqueia uma nova candidata que ainda não recebeu rejeição própria. */
  @Test
  void permitsSuccessorWithoutItsOwnRejection() {
    when(tasks.findPdeValidationTaskSnapshots("experiment:9124", "pde-construction-approval"))
        .thenReturn(List.of());
    assertThat(
            provider
                .readiness(
                    process(),
                    activity("prototypeCorrection"),
                    Product.builder().id(7L).build(),
                    "experiment:9124")
                .ready())
        .isTrue();
  }

  /** Mantém a correção legada e restringe o gate ao domínio e atividade correspondentes. */
  @Test
  void preservesLegacyAndOtherActivities() {
    var process = process();
    var product = Product.builder().id(7L).build();
    assertThat(
            provider
                .readiness(
                    process,
                    activity("prototypeCorrection"),
                    product,
                    "product:7@agent-validation-v1")
                .ready())
        .isTrue();
    assertThat(provider.supports(process, activity("psiqueAdherent"))).isFalse();
    process.setProcessCode("another-domain");
    assertThat(provider.supports(process, activity("prototypeCorrection"))).isFalse();
    verifyNoInteractions(tasks);
  }

  /** Monta a definição genérica do domínio sem alterar contratos ou processos publicados. */
  private BusinessProcessDefinition process() {
    var process = new BusinessProcessDefinition();
    process.setId(117L);
    process.setProcessCode("pde-construction-approval");
    process.setVersionNumber(8);
    return process;
  }

  /** Confere o gate na revisão publicada do processo com o contrato canônico completo. */
  @Test
  void supportsCurrentCanonicalRevision() throws Exception {
    var process = process();
    process.setVersionNumber(42);
    for (var source :
        new ObjectMapper()
            .readTree(
                java.nio.file.Path.of("../../infra/testing/pde-commercial-principles/sources.json")
                    .toFile())) {
      if (process.getProcessCode().equals(source.path("processCode").asText())) {
        process.setDiagramJson(source.path("diagram").toString());
      }
    }
    assertThat(provider.supports(process, activity("prototypeCorrection"))).isTrue();
  }

  /** Monta a atividade consultada pelo mesmo ponto de entrada do motor BPM. */
  private BusinessProcessActivityDefinition activity(String code) {
    var activity = new BusinessProcessActivityDefinition();
    activity.setActivityId(code);
    return activity;
  }

  /** Projeta a rejeição auditável sem transportar prompts nem conteúdo de outro produto. */
  private PdeValidationTaskSnapshot proof(long id, String activity, String category) {
    return new PdeValidationTaskSnapshot(id, 117L, activity, "BLOCKED", category, null, null, null);
  }
}

package com.marketinghub.product.service.agentvalidation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.predecessor.ProductProcessActivityPredecessorReadiness;
import com.marketinghub.businessprocess.execution.service.predecessor.ProductProcessActivityPredecessorService;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Responsabilidade: comprovar a rota condicional de correção e revalidação do PDE v8. */
class PdeAgentValidationReworkReadinessProviderTest {
  private static final String SOURCE = "product:10@agent-validation-v1";
  private final ProductProcessActivityPredecessorService predecessors =
      mock(ProductProcessActivityPredecessorService.class);
  private final AgentTaskRepository tasks = mock(AgentTaskRepository.class);
  private final PdeAgentValidationReworkReadinessProvider provider =
      new PdeAgentValidationReworkReadinessProvider(predecessors, tasks, new ObjectMapper());
  private final List<AgentTask> history = new ArrayList<>();
  private BusinessProcessDefinition process;
  private Product product;

  /** Monta a versão publicada e a identidade aceita da nova versão de Mira. */
  @BeforeEach
  void setUp() {
    process = process(80L, 8);
    product =
        Product.builder()
            .id(10L)
            .validationDefinitionJson(
                "{\"privatePrototypeAcceptance\":{\"prototypeVersion\":\"mira-private-v2\"}}")
            .build();
    when(tasks.findBySourceReferenceOrderByCreatedAtAscIdAsc(SOURCE)).thenReturn(history);
  }

  /** Encaminha o parecer rejeitado para Dédalo com causa, ação e retorno explícitos. */
  @Test
  void exposesCorrectionAfterFunctionalRejectionFromPreviousProcessVersion() {
    AgentTask rejection = rejection(350L);
    history.add(rejection);

    var readiness = provider.readiness(process, activity("prototypeCorrection"), product, SOURCE);

    assertThat(readiness.ready()).isTrue();
    assertThat(readiness.reason())
        .contains("#350")
        .contains("Psique · cenário aderente")
        .contains("rotina útil desaparece")
        .containsIgnoringCase("manter a rotina visível")
        .contains("homologação técnica");
  }

  /** Impede reexecutar o harness antes de existir correção válida e versionada. */
  @Test
  void blocksTechnicalHomologationUntilCorrectionIsReady() {
    history.add(rejection(350L));

    var readiness = provider.readiness(process, activity("technicalHomologation"), product, SOURCE);

    assertThat(readiness.ready()).isFalse();
    assertThat(readiness.reason()).contains("publique uma nova versão");
  }

  /** Libera nova homologação quando Dédalo corrige a causa em versão distinta da rejeitada. */
  @Test
  void releasesTechnicalHomologationAfterVersionedCorrection() {
    history.add(rejection(350L));
    history.add(correction(351L));
    BusinessProcessActivityDefinition technical = activity("technicalHomologation");
    when(predecessors.readiness(process, technical, SOURCE))
        .thenReturn(
            new ProductProcessActivityPredecessorReadiness(
                true, "A atividade predecessora possui conclusão comprovada."));

    var readiness = provider.readiness(process, technical, product, SOURCE);

    assertThat(readiness.ready()).isTrue();
    assertThat(readiness.reason()).contains("nova versão");
  }

  /** Não aceita a homologação da versão aposentada como autorização para Psique na v8. */
  @Test
  void requiresCurrentProcessTechnicalApprovalForPsique() {
    AgentTask oldTechnical = task(349L, process(69L, 7), "technicalHomologation", "COMPLETED");
    oldTechnical.setResultJson(
        "{\"decision\":\"APPROVED\",\"prototypeVersion\":\"mira-private-v1\"}");
    history.add(oldTechnical);
    BusinessProcessActivityDefinition psique = activity("psiqueAdherent");
    when(predecessors.readiness(process, psique, SOURCE))
        .thenReturn(
            new ProductProcessActivityPredecessorReadiness(
                true, "A atividade predecessora possui conclusão comprovada."));

    var readiness = provider.readiness(process, psique, product, SOURCE);

    assertThat(readiness.ready()).isFalse();
    assertThat(readiness.reason()).contains("mira-private-v2");
  }

  /** Uma segunda rejeição exige nova correção mesmo quando a versão já teve homologação técnica. */
  @Test
  void blocksReviewerAfterNewFunctionalRejection() {
    history.add(correction(353L));
    AgentTask approved = task(354L, process, "technicalHomologation", "COMPLETED");
    approved.setResultJson("{\"decision\":\"APPROVED\",\"prototypeVersion\":\"mira-private-v2\"}");
    history.add(approved);
    AgentTask newRejection = rejection(355L);
    newRejection.setProcessDefinition(process);
    history.add(newRejection);

    var readiness = provider.readiness(process, activity("psiqueAdherent"), product, SOURCE);

    assertThat(readiness.ready()).isFalse();
    assertThat(readiness.reason()).contains("#355", "publique uma nova versão");
    assertThat(
            provider.requiresFreshExecution(
                process, activity("prototypeCorrection"), product, SOURCE))
        .isTrue();
  }

  /** Mantém bloqueios atuais visíveis e retira do estado corrente somente os já corrigidos. */
  @ParameterizedTest
  @CsvSource({
    "technicalHomologation,352,true",
    "technicalHomologation,355,false",
    "psiqueAdherent,352,true",
    "psiqueAdherent,355,false",
    "prototypeCorrection,356,false"
  })
  void keepsCurrentBlockAndRetiresCorrectedBlock(
      String activityId, long taskId, boolean expectedFresh) {
    history.add(rejection(350L));
    history.add(correction(353L));
    if ("prototypeCorrection".equals(activityId)) {
      AgentTask newRejection = rejection(355L);
      newRejection.setProcessDefinition(process);
      history.add(newRejection);
    }
    history.add(task(taskId, process, activityId, "BLOCKED"));
    assertThat(provider.requiresFreshExecution(process, activity(activityId), product, SOURCE))
        .isEqualTo(expectedFresh);
  }

  /** Uma falha técnica posterior impede reutilizar a aprovação anterior como prova vigente. */
  @Test
  void newerTechnicalFailureInvalidatesPreviousApproval() {
    history.add(correction(353L));
    AgentTask approved = task(354L, process, "technicalHomologation", "COMPLETED");
    approved.setResultJson("{\"decision\":\"APPROVED\",\"prototypeVersion\":\"mira-private-v2\"}");
    history.add(approved);
    history.add(task(356L, process, "technicalHomologation", "BLOCKED"));
    var psique = activity("psiqueAdherent");
    when(predecessors.readiness(process, psique, SOURCE))
        .thenReturn(new ProductProcessActivityPredecessorReadiness(true, "Histórico concluído."));
    assertThat(provider.readiness(process, psique, product, SOURCE).ready()).isFalse();
  }

  /** Limita a regra à versão que declara a rota de retrabalho. */
  @Test
  void supportsOnlyVersionEightActivities() {
    assertThat(provider.supports(process, activity("prototypeCorrection"))).isTrue();
    assertThat(provider.supports(process, activity("psiqueSafety"))).isTrue();

    process.setVersionNumber(7);

    assertThat(provider.supports(process, activity("prototypeCorrection"))).isFalse();
  }

  /** Cria uma rejeição funcional histórica com orientação operacional preservada. */
  private AgentTask rejection(long id) {
    AgentTask task = task(id, process(69L, 7), "psiqueAdherent", "BLOCKED");
    task.setBlockerCategory("FUNCTIONAL_ADJUSTMENT");
    task.setBlockerAction("Manter a rotina visível depois da conclusão.");
    task.setExecutionError("Psique rejeitou a continuidade da experiência.");
    task.setResultJson(
        "{\"rootCause\":\"A rotina útil desaparece quando o cenário é concluído.\"}");
    return task;
  }

  /** Cria a conclusão de Dédalo que liga a rejeição à versão corrigida. */
  private AgentTask correction(long id) {
    AgentTask task = task(id, process, "prototypeCorrection", "COMPLETED");
    task.setResultJson(
        """
        {
          "decision":"READY",
          "correctionPlan":{
            "previousPrototypeVersion":"mira-private-v1",
            "correctedPrototypeVersion":"mira-private-v2",
            "nextActivityId":"technicalHomologation",
            "verification":{
              "technicalRevalidationRequired":true,
              "noExternalSideEffects":true
            }
          }
        }
        """);
    return task;
  }

  /** Monta uma tarefa ordenável do mesmo produto e processo funcional. */
  private AgentTask task(
      long id, BusinessProcessDefinition taskProcess, String activityId, String status) {
    AgentTask task = new AgentTask();
    task.setId(id);
    task.setProcessDefinition(taskProcess);
    task.setSourceReference(SOURCE);
    task.setProcessActivityId(activityId);
    task.setStatus(status);
    task.setCreatedAt(Instant.parse("2026-09-07T10:00:00Z").plusSeconds(id));
    task.setUpdatedAt(task.getCreatedAt());
    return task;
  }

  /** Cria a identidade mínima de uma versão do macroprocesso. */
  private BusinessProcessDefinition process(long id, int version) {
    BusinessProcessDefinition definition = new BusinessProcessDefinition();
    definition.setId(id);
    definition.setProcessCode("pde-construction-approval");
    definition.setVersionNumber(version);
    definition.setStatus(version == 8 ? "PUBLISHED" : "RETIRED");
    return definition;
  }

  /** Cria a definição relacional da atividade consultada. */
  private BusinessProcessActivityDefinition activity(String activityId) {
    BusinessProcessActivityDefinition activity = new BusinessProcessActivityDefinition();
    activity.setId((long) activityId.hashCode());
    activity.setProcessDefinition(process);
    activity.setActivityId(activityId);
    return activity;
  }
}

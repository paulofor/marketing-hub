package com.marketinghub.repository.jpa.leadportal;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.EntityGraph;

/** Responsabilidade: proteger a leitura de fluxos contra duplicação cartesiana de perguntas. */
class LeadPortalFlowRepositoryContractTest {

  /** Confirma que perguntas e suas opções não são buscadas no mesmo grafo JPA. */
  @Test
  void entityGraphsDoNotJoinQuestionsAndOptionsTogether() {
    for (Method method : LeadPortalFlowRepository.class.getDeclaredMethods()) {
      EntityGraph graph = method.getAnnotation(EntityGraph.class);
      if (graph == null) {
        continue;
      }
      assertThat(Arrays.asList(graph.attributePaths()))
          .as("método %s", method.getName())
          .doesNotContain("questions.options");
    }
  }
}

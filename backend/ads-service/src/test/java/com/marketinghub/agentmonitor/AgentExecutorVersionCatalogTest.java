package com.marketinghub.agentmonitor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/**
 * Responsabilidade: proteger a identidade técnica publicada contra cadastro ausente ou inválido.
 */
class AgentExecutorVersionCatalogTest {
  /** Confirma o recurso empacotado e impede fallback para um executor não catalogado. */
  @Test
  void loadsPackagedVersionsWithoutUnknownFallback() {
    var catalog = AgentExecutorVersionCatalog.load();
    assertThat(catalog.expectedVersion("customer-agent")).isEqualTo(6);
    assertThat(catalog.expectedVersion("unknown")).isNull();
  }

  /** Rejeita manifesto vazio, versão inválida e agentes duplicados antes de liberar o serviço. */
  @Test
  void rejectsInvalidCatalogs() throws Exception {
    var json = new ObjectMapper();
    for (String source :
        new String[] {
          "{}",
          "{\"contractVersion\":\"codex-health-v1\",\"agents\":[]}",
          "{\"contractVersion\":\"codex-health-v1\",\"agents\":[{\"key\":\"customer-agent\",\"expectedVersion\":0}]}",
          "{\"contractVersion\":\"codex-health-v1\",\"agents\":[{\"key\":\"customer-agent\",\"expectedVersion\":6},{\"key\":\"customer-agent\",\"expectedVersion\":7}]}"
        }) {
      var document = json.readTree(source);
      assertThatThrownBy(() -> new AgentExecutorVersionCatalog(document))
          .isInstanceOf(IllegalStateException.class);
    }
  }
}

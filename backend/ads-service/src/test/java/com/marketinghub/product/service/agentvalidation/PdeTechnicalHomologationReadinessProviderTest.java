package com.marketinghub.product.service.agentvalidation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.agenttask.AgentTaskTargetContextProvider;
import com.marketinghub.agenttask.AgentTaskTargetResponse;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.product.Product;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Comprova que especificações e alvos divergentes não liberam a homologação técnica. */
class PdeTechnicalHomologationReadinessProviderTest {
  private final AgentTaskTargetContextProvider targets = mock(AgentTaskTargetContextProvider.class);
  private final PdeTechnicalHomologationReadinessProvider provider =
      new PdeTechnicalHomologationReadinessProvider(targets);
  private final ObjectMapper json = new ObjectMapper();

  /** Reproduz a entrada do segundo ciclo: existe plano, mas a URL executável está ausente. */
  @Test
  void blocksTask377PlannedCycleBeforeCreatingWork() {
    var target =
        target(
            "experiment:92", 4L, null, "vega-v8", json.createObjectNode().put("status", "PLANNED"));
    when(targets.resolve("experiment:92", "pde-construction-approval"))
        .thenReturn(Optional.of(target));
    var result = provider.readiness(process(), activity(), product(4L), "experiment:92");
    assertThat(result.ready()).isFalse();
    assertThat(result.reason()).contains("URL executável", "Dédalo", "implementação", "Repetir");
  }

  /** Mantém o caminho histórico válido quando URL e versão correspondem à aceitação privada. */
  @Test
  void allowsAcceptedPrivatePrototype() {
    var result = readiness("valid");
    assertThat(result.ready()).isTrue();
  }

  /** Rejeita identidades, versões, URLs ou aceitação incompatíveis sem executar integração. */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "missing",
        "product",
        "slug",
        "reference",
        "cycle",
        "version",
        "url",
        "acceptance",
        "planned",
        "blank-version",
        "http",
        "credentials",
        "query",
        "fragment",
        "malformed",
        "resolution-error"
      })
  void rejectsIncompleteOrMismatchedTargets(String scenario) {
    assertThat(readiness(scenario).ready()).isFalse();
  }

  /** Não aplica a regra da homologação a tarefas de especificação ou versões antigas. */
  @Test
  void scopesReadinessToTechnicalHomologation() {
    var process = process();
    var activity = activity();
    assertThat(provider.supports(process, activity)).isTrue();
    activity.setActivityId("deliverables");
    assertThat(provider.supports(process, activity)).isFalse();
    activity.setActivityId("technicalHomologation");
    process.setVersionNumber(6);
    assertThat(provider.supports(process, activity)).isFalse();
  }

  /** Monta mutações independentes de um contrato válido para exercitar fronteiras reais. */
  private com.marketinghub.businessprocess.execution.service.agentactivity
          .AgentProductProcessActivityReadiness
      readiness(String scenario) {
    String source = scenario.equals("cycle") ? "experiment:92" : "product:10@agent-validation-v1";
    String url =
        switch (scenario) {
          case "http" -> "http://prototype.example/mira-private";
          case "credentials" -> "https://test:secret@prototype.example/mira-private";
          case "query" -> "https://prototype.example/mira-private?token=secret";
          case "fragment" -> "https://prototype.example/mira-private#secret";
          case "malformed" -> "https://[invalid";
          default -> "https://prototype.example/mira-private";
        };
    var context = json.createObjectNode();
    context
        .putObject("privatePrototypeAcceptance")
        .put("status", scenario.equals("planned") ? "PLANNED" : "READY")
        .put("prototypeVersion", scenario.equals("version") ? "mira-private-v1" : "mira-private-v3")
        .put(
            "privateAccessUrl",
            scenario.equals("url") ? "https://other.example/mira-private" : url);
    var target =
        target(
            scenario.equals("reference") ? "product:4@agent-validation-v1" : source,
            scenario.equals("product") ? 4L : 10L,
            url,
            scenario.equals("blank-version") ? "" : "mira-private-v3",
            scenario.equals("acceptance") ? null : context);
    if (scenario.equals("slug")) {
      target =
          new AgentTaskTargetResponse(
              source,
              null,
              10L,
              "another-product",
              "Mira",
              "Mira",
              "mira-private-v3",
              url,
              null,
              null,
              null,
              null,
              context);
    }
    when(targets.resolve(source, "pde-construction-approval"))
        .thenReturn(scenario.equals("missing") ? Optional.empty() : Optional.of(target));
    if (scenario.equals("resolution-error")) {
      when(targets.resolve(source, "pde-construction-approval"))
          .thenThrow(new IllegalStateException("Contrato indisponível"));
    }
    return provider.readiness(process(), activity(), product(10L), source);
  }

  /** Cria alvo sintético sem URL comercial ou credenciais operacionais. */
  private AgentTaskTargetResponse target(
      String reference, Long id, String url, String version, ObjectNode context) {
    return new AgentTaskTargetResponse(
        reference,
        null,
        id,
        "orientacao-digital-rotina-pele-madura",
        "Mira",
        "Mira",
        version,
        url,
        null,
        null,
        null,
        null,
        context);
  }

  /** Identifica o produto independente das strings de apresentação da atividade. */
  private Product product(Long id) {
    return Product.builder().id(id).slug("orientacao-digital-rotina-pele-madura").build();
  }

  /** Define o processo operacional multiagente atual. */
  private BusinessProcessDefinition process() {
    var result = new BusinessProcessDefinition();
    result.setId(70L);
    result.setProcessCode("pde-construction-approval");
    result.setVersionNumber(8);
    return result;
  }

  /** Seleciona somente a etapa técnica determinística. */
  private BusinessProcessActivityDefinition activity() {
    var result = new BusinessProcessActivityDefinition();
    result.setActivityId("technicalHomologation");
    return result;
  }
}

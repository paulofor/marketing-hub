package com.marketinghub.landinggeneratoragent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/** Responsabilidade: proteger os contratos BPM de construção do PDE executados por Dédalo. */
class PdeConstructionBpmTaskConsumerTest {
  private final ObjectMapper json = new ObjectMapper();

  /** Resolve prompts e schemas específicos sem misturar contratos entre atividades. */
  @Test
  void resolvesVersionedResourcesByActivity() {
    assertThat(
            PdeConstructionBpmTaskConsumer.promptResourceFor(
                "pde-construction-approval", "journey"))
        .isEqualTo("prompts/pde-construction/v1/journey.md");
    assertThat(
            PdeConstructionBpmTaskConsumer.schemaResourceFor("pde-construction-approval", "access"))
        .isEqualTo("prompts/pde-construction/v1/access-schema.json");
    assertThat(
            PdeConstructionBpmTaskConsumer.promptResourceFor(
                "venda-entrega-satisfacao-cliente", "materialization"))
        .isEqualTo("prompts/pde-delivery/v1/personalization.md");
    assertThat(
            PdeConstructionBpmTaskConsumer.promptResourceFor(
                "pde-tasting-proof-of-value", "materialization"))
        .isEqualTo("prompts/pde-tasting/v1/materialization.md");
  }

  /** Cobre todas as atividades vigentes de Dédalo e recusa comunicação pertencente a Íris. */
  @Test
  void supportsOnlyCurrentProductContracts() {
    assertThat(
            PdeConstructionBpmTaskConsumer.supportsContract(
                "pde-commercial-plan-offer", "productArchitecture"))
        .isTrue();
    assertThat(
            PdeConstructionBpmTaskConsumer.supportsContract("pde-construction-approval", "journey"))
        .isTrue();
    assertThat(
            PdeConstructionBpmTaskConsumer.supportsContract(
                "pde-construction-approval", "deliverables"))
        .isTrue();
    assertThat(
            PdeConstructionBpmTaskConsumer.supportsContract("pde-construction-approval", "access"))
        .isTrue();
    assertThat(
            PdeConstructionBpmTaskConsumer.supportsContract(
                "pde-tasting-proof-of-value", "materialization"))
        .isTrue();
    assertThat(
            PdeConstructionBpmTaskConsumer.supportsContract(
                "venda-entrega-satisfacao-cliente", "materialization"))
        .isTrue();
    assertThat(
            PdeConstructionBpmTaskConsumer.supportsContract(
                "creative-production-approval", "nonAudiovisual"))
        .isFalse();
    assertThat(PdeConstructionBpmTaskConsumer.supportsContract("landing-page-generation", "html"))
        .isFalse();
  }

  /** Prioriza a personalização de venda reconciliada antes de construção e degustação. */
  @Test
  void prioritizesPaidDeliveryInPollingOrder() {
    assertThat(PdeConstructionBpmTaskConsumer.contractKeysInPollingOrder())
        .startsWith("venda-entrega-satisfacao-cliente/materialization")
        .containsExactlyInAnyOrder(
            "venda-entrega-satisfacao-cliente/materialization",
            "pde-construction-approval/journey",
            "pde-construction-approval/deliverables",
            "pde-construction-approval/access",
            "pde-commercial-plan-offer/productArchitecture",
            "pde-tasting-proof-of-value/materialization");
  }

  /** Garante que todos os schemas executáveis existem e são estritos em cada objeto. */
  @Test
  void keepsEveryCurrentProductSchemaStrict() throws Exception {
    List<String> schemas =
        List.of(
            PdeConstructionBpmTaskConsumer.schemaResourceFor(
                "pde-commercial-plan-offer", "productArchitecture"),
            PdeConstructionBpmTaskConsumer.schemaResourceFor(
                "pde-construction-approval", "journey"),
            PdeConstructionBpmTaskConsumer.schemaResourceFor(
                "pde-construction-approval", "deliverables"),
            PdeConstructionBpmTaskConsumer.schemaResourceFor("pde-construction-approval", "access"),
            PdeConstructionBpmTaskConsumer.schemaResourceFor(
                "pde-tasting-proof-of-value", "materialization"),
            PdeConstructionBpmTaskConsumer.schemaResourceFor(
                "venda-entrega-satisfacao-cliente", "materialization"));

    for (String schema : schemas) {
      JsonNode root =
          json.readTree(
              new ClassPathResource(schema)
                  .getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
      assertStrictObjects(root, schema);
    }
  }

  /** Impede que o prompt genérico de jornada recupere produto fixo ou comunicação de Íris. */
  @Test
  void keepsJourneyGenericAndRestrictedToPostPurchaseProduct() throws Exception {
    String prompt = read("prompts/pde-construction/v1/journey.md");

    assertThat(prompt).contains("jornada pós-compra", "pertencem a Íris", "TASK_CONTEXT");
    assertThat(prompt).doesNotContain("Kit Manual de Atendimento", "landing e artefatos");
  }

  /** Aceita somente jornada com decisão comparada, cinco etapas e critérios verificáveis. */
  @Test
  void validatesCompleteJourney() throws Exception {
    var result =
        json.readTree(
            """
            {"decision":"READY","rationale":"Contrato completo e coerente.",
             "selectedApproach":"Formulário guiado com entrega assistida completa.",
             "alternatives":[{},{},{}],"acceptanceCriteria":["a"],
             "experienceContract":{"stages":[{},{},{},{},{}]}}
            """);

    PdeConstructionBpmTaskConsumer.validate(result, "pde-construction-approval", "journey");
  }

  /** Bloqueia pacote que não contém todos os grupos mínimos de entregáveis. */
  @Test
  void rejectsIncompleteDeliveryPackage() throws Exception {
    var result =
        json.readTree(
            """
            {"decision":"READY","rationale":"Pacote parcial.",
             "selectedApproach":"Materiais editáveis com orientação guiada completa.",
             "alternatives":[{},{},{}],"acceptanceCriteria":["a"],
             "deliveryPackage":{"assets":[{},{},{}]}}
            """);

    assertThatThrownBy(
            () ->
                PdeConstructionBpmTaskConsumer.validate(
                    result, "pde-construction-approval", "deliverables"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Pacote");
  }

  /** Impede decisão de abordagem truncada pelo contrato estruturado. */
  @Test
  void rejectsTruncatedSelectedApproach() throws Exception {
    var result =
        json.readTree(
            """
            {"decision":"READY","rationale":"A jornada atende aos gates.",
             "selectedApproach":"sem","alternatives":[{},{},{}],
             "acceptanceCriteria":["critério"],
             "experienceContract":{"stages":[{},{},{},{},{}]}}
            """);

    assertThatThrownBy(
            () ->
                PdeConstructionBpmTaskConsumer.validate(
                    result, "pde-construction-approval", "journey"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("decisão comparada");
  }

  /** Aceita arquitetura de produto aprovada sem exigir campos exclusivos da construção v1. */
  @Test
  void validatesProductArchitecture() throws Exception {
    var result =
        json.readTree(
            """
            {"decision":"APPROVE","rationale":"Arquitetura coerente com os contratos.",
             "selectedApproach":"Experiência assistida com primeiro valor verificável.",
             "alternatives":[{},{},{}],"productArchitecture":{"format":"PDE"}}
            """);

    PdeConstructionBpmTaskConsumer.validate(
        result, "pde-commercial-plan-offer", "productArchitecture");
  }

  /** Exige artefato funcional, eventos e isolamento antes de liberar uma degustação. */
  @Test
  void rejectsTastingWithoutFunctionalArtifact() throws Exception {
    var result =
        json.readTree(
            """
            {"decision":"READY","rationale":"A descrição ainda não materializa valor.",
             "selectedApproach":"Microexperiência limitada ao primeiro resultado real.",
             "alternatives":[{},{},{}],"acceptanceCriteria":["valor real"],
             "tastingExperience":{"steps":[{},{},{}]},
             "functionalArtifact":{"content":""},
             "instrumentationEvents":["TASTING_STARTED"],"testIsolation":"qa=true"}
            """);

    assertThatThrownBy(
            () ->
                PdeConstructionBpmTaskConsumer.validate(
                    result, "pde-tasting-proof-of-value", "materialization"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Microexperiência");
  }

  /** Exige referência da venda e entregáveis concretos na personalização pós-compra. */
  @Test
  void validatesPaidPersonalizationPackage() throws Exception {
    var result =
        json.readTree(
            """
            {"decision":"READY","rationale":"Venda e versão contratada foram preservadas.",
             "selectedApproach":"Personalização mínima com revisão humana e acesso rastreável.",
             "alternatives":[{},{},{}],"acceptanceCriteria":["entrega íntegra"],
             "personalizationPackage":{"contractReference":"sale:42","deliverables":[{}]},
             "qualityChecks":[{}],"accessHandoff":{}}
            """);

    PdeConstructionBpmTaskConsumer.validate(
        result, "venda-entrega-satisfacao-cliente", "materialization");
  }

  /** Encerra o processo Codex filho antes do lançador quando a atividade ultrapassa o timeout. */
  @Test
  void terminatesWholeProcessTree() {
    Process process = mock(Process.class);
    ProcessHandle child = mock(ProcessHandle.class);
    when(process.descendants()).thenReturn(Stream.of(child));

    PdeConstructionBpmTaskConsumer.terminateTree(process);

    verify(child).destroyForcibly();
    verify(process).destroyForcibly();
  }

  /** Verifica recursivamente o subconjunto estrito exigido pelos contratos de saída. */
  private void assertStrictObjects(JsonNode node, String resource) {
    if (node.isObject() && "object".equals(node.path("type").asText())) {
      assertThat(node.path("additionalProperties").asBoolean(true))
          .as("additionalProperties em %s", resource)
          .isFalse();
      Set<String> propertyNames =
          StreamSupport.stream(
                  java.util.Spliterators.spliteratorUnknownSize(
                      node.path("properties").fieldNames(), java.util.Spliterator.ORDERED),
                  false)
              .collect(Collectors.toSet());
      Set<String> required =
          StreamSupport.stream(node.path("required").spliterator(), false)
              .map(JsonNode::asText)
              .collect(Collectors.toSet());
      assertThat(required).as("required em %s", resource).isEqualTo(propertyNames);
    }
    node.forEach(child -> assertStrictObjects(child, resource));
  }

  /** Lê um prompt do classpath com a mesma codificação usada em produção. */
  private String read(String resource) throws IOException {
    return new ClassPathResource(resource)
        .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
  }
}

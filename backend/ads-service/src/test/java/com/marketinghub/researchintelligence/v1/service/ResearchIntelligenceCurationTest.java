package com.marketinghub.researchintelligence.v1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agent.Agent;
import com.marketinghub.agent.AgentInput;
import com.marketinghub.repository.jpa.agent.AgentRepository;
import com.marketinghub.researchintelligence.v1.service.catalog.ResearchIntelligenceAgentPolicyResponse;
import com.marketinghub.salesvideo.VideoProject;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Comprova que referências do cadastro chegam às tarefas e preservam evidência, limites e
 * isolamento.
 */
class ResearchIntelligenceCurationTest {
  private final Clock clock = Clock.fixed(Instant.parse("2026-09-10T12:00:00Z"), ZoneOffset.UTC);
  private AgentRepository repository;
  private ResearchIntelligenceService service;
  private Map<String, Agent> agents;

  /** Simula cadastros independentes com as mesmas referências escolhidas na curadoria comercial. */
  @BeforeEach
  void setUp() throws Exception {
    repository = mock(AgentRepository.class);
    agents = new LinkedHashMap<>();
    try (var input =
        getClass().getResourceAsStream("/fixtures/harness-curadoria-agentes-v1.json")) {
      JsonNode fixture = new ObjectMapper().readTree(input);
      for (JsonNode row : fixture) {
        Agent agent =
            Agent.builder()
                .id(row.path("agentId").asLong())
                .agentKey(row.path("agentKey").asText())
                .nickname(row.path("agentName").asText())
                .currentVersion(99)
                .build();
        for (JsonNode reference : row.path("inputs")) {
          agent
              .getInputs()
              .add(
                  AgentInput.builder()
                      .name(reference.path("name").asText())
                      .type(reference.path("type").asText())
                      .description(reference.path("description").asText())
                      .build());
        }
        agents.put(agent.getAgentKey(), agent);
        when(repository.findByAgentKey(agent.getAgentKey())).thenReturn(Optional.of(agent));
      }
    }
    service = new ResearchIntelligenceService(clock, null, repository);
  }

  /**
   * Entrega as 25 referências de 14 fontes às nove identidades sem copiar a biblioteca integral.
   */
  @Test
  void deliversCuratedCardsToAllNineAgentsWithoutAudiovisualKeywords() throws Exception {
    assertThat(agents).hasSize(9);
    assertThat(agents.values().stream().flatMap(agent -> agent.getInputs().stream())).hasSize(25);
    for (Agent agent : agents.values()) {
      var selection =
          service.selectForAgentTask(agent.getAgentKey(), "Atividade da Cadeia de Valor");
      assertThat(selection.routes()).hasSize(1);
      var route = selection.routes().getFirst();
      assertThat(route.agentKey()).isEqualTo(agent.getAgentKey());
      assertThat(route.cards())
          .extracting("cardId")
          .containsExactlyInAnyOrderElementsOf(
              agent.getInputs().stream().map(AgentInput::getName).toList());
      assertThat(route.selectionReason())
          .contains("Orientação editorial", "v99", "não é achado científico");
      for (var card : route.cards()) {
        assertThat(card.sourceSha256()).matches("[0-9a-f]{64}");
        assertThat(route.selectionReason()).contains(card.cardId());
      }
      assertThat(agent.getInputs())
          .allSatisfy(
              input ->
                  assertThat(input.getDescription().getBytes(StandardCharsets.UTF_8))
                      .hasSizeLessThanOrEqualTo(255));
    }
    String output = System.getProperty("harness.curation.fixture");
    if (output != null) {
      Map<String, Object> selections = new LinkedHashMap<>();
      for (Agent agent : agents.values())
        selections.put(
            agent.getAgentKey(),
            service.selectForAgentTask(agent.getAgentKey(), "Atividade da Cadeia de Valor"));
      new ObjectMapper()
          .findAndRegisterModules()
          .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
          .writerWithDefaultPrettyPrinter()
          .writeValue(
              java.nio.file.Path.of(output).toFile(),
              Map.of("catalog", service.getCatalog(), "selections", selections));
    }
  }

  /**
   * Mostra no catálogo a associação real, inclusive agentes sem coleções audiovisuais automáticas.
   */
  @Test
  void exposesSavedAssignmentsAndTheirAvailability() {
    var catalog = service.getCatalog();
    assertThat(catalog.agentPolicies()).hasSize(9);
    assertThat(catalog.agentPolicies())
        .allSatisfy(
            policy -> {
              assertThat(policy.agentId()).isNotNull();
              assertThat(policy.assignments())
                  .isNotEmpty()
                  .allSatisfy(assignment -> assertThat(assignment.available()).isTrue());
            });
    assertThat(catalog.agentPolicies())
        .filteredOn(policy -> policy.agentKey().equals("landing-generator"))
        .extracting(ResearchIntelligenceAgentPolicyResponse::agentName)
        .containsExactly("Dédalo");
  }

  /** Uma menção a vídeo no histórico não remove a curadoria do protótipo de Psique. */
  @Test
  void doesNotLetOldAudiovisualContextOverrideExplicitTaskCuration() {
    var route =
        service
            .selectForAgentTask(
                "customer-agent", "Revisar protótipo; histórico inclui vídeo Instagram")
            .routes()
            .getFirst();
    assertThat(route.cards()).extracting("cardId").contains("RI1-AAB0EC98AB06");
  }

  /**
   * Mudanças de orientação ou versão alteram o fingerprint auditável sem mudar o artigo científico.
   */
  @Test
  void fingerprintsEditorialGuidanceAndAgentVersion() {
    var before = service.selectForAgentTask("landing-generator", "Jornada");
    agents
        .get("landing-generator")
        .getInputs()
        .getFirst()
        .setDescription("Nova hipótese de conclusão da jornada.");
    agents.get("landing-generator").setCurrentVersion(100);
    var after = service.selectForAgentTask("landing-generator", "Jornada");
    assertThat(after.contextFingerprint()).isNotEqualTo(before.contextFingerprint());
    assertThat(after.routes().getFirst().cards())
        .containsExactlyInAnyOrderElementsOf(before.routes().getFirst().cards());
  }

  /**
   * Referências duplicadas não aumentam o contexto e entradas de outros tipos não são
   * interpretadas.
   */
  @Test
  void ignoresOrdinaryInputsAndDeduplicatesReferences() {
    Agent agent = agents.get("landing-generator");
    agent.getInputs().add(agent.getInputs().getFirst());
    agent
        .getInputs()
        .add(
            AgentInput.builder()
                .name("RI1-646DE1AF483E")
                .type("CONTEXT")
                .description("Outra entrada")
                .build());
    assertThat(
            service.selectForAgentTask(agent.getAgentKey(), "Jornada").routes().getFirst().cards())
        .hasSize(3)
        .extracting("cardId")
        .doesNotContain("RI1-646DE1AF483E");
  }

  /**
   * Ausência, vencimento e publicação futura aparecem como indisponibilidade, sem inventar fonte
   * substituta.
   */
  @Test
  void excludesUnavailableExpiredAndFutureCards() {
    Agent agent = agents.get("landing-generator");
    agent
        .getInputs()
        .add(
            AgentInput.builder()
                .name("RI1-INEXISTENTE")
                .type(ResearchIntelligenceService.CURATED_INPUT_TYPE)
                .build());
    var current = service.selectForAgentTask(agent.getAgentKey(), "Jornada");
    assertThat(current.routes().getFirst().selectionReason())
        .contains("RI1-INEXISTENTE", "Referências indisponíveis");
    for (String instant : List.of("2025-01-01T00:00:00Z", "2030-01-01T00:00:00Z")) {
      var other =
          new ResearchIntelligenceService(
              Clock.fixed(Instant.parse(instant), ZoneOffset.UTC), null, repository);
      assertThat(
              other.selectForAgentTask(agent.getAgentKey(), "Jornada").routes().getFirst().cards())
          .isEmpty();
      assertThat(other.getCatalog().agentPolicies())
          .flatExtracting("assignments")
          .allSatisfy(
              assignment ->
                  assertThat(
                          ((com.marketinghub.researchintelligence.v1.service.catalog
                                      .ResearchIntelligenceAssignmentResponse)
                                  assignment)
                              .available())
                      .isFalse());
    }
  }

  /** Mantém no máximo quatro referências mesmo quando a curadoria possui muitas entradas. */
  @Test
  void boundsCurationToFourCards() {
    var agent = agents.get("landing-generator");
    for (var card : service.getCatalog().cards().subList(0, 10)) {
      agent
          .getInputs()
          .add(
              AgentInput.builder()
                  .name(card.cardId())
                  .type(ResearchIntelligenceService.CURATED_INPUT_TYPE)
                  .build());
    }
    assertThat(
            service.selectForAgentTask(agent.getAgentKey(), "Jornada").routes().getFirst().cards())
        .hasSize(4);
  }

  /** Projetos de vídeo preservam as quatro rotas e a cobertura de coleções previamente exigida. */
  @Test
  void preservesVideoProductionCollectionCoverage() {
    var selection =
        service.selectForVideoProject(
            VideoProject.builder().title("Vídeo do resultado útil").build());
    Map<String, List<String>> expected =
        Map.of(
            "communication-director", List.of("neuromarketing", "momentos-de-compra-b2c"),
            "videomaker", List.of("video", "prazer-audio-visual"),
            "customer-agent", List.of("neuromarketing", "prazer-audio-visual"),
            "meta-ad-approver",
                List.of(
                    "video", "prazer-audio-visual", "neuromarketing", "momentos-de-compra-b2c"));
    assertThat(selection.routes())
        .hasSize(4)
        .allSatisfy(
            route -> {
              assertThat(route.cards()).hasSizeLessThanOrEqualTo(4);
              assertThat(route.cards())
                  .extracting("collection")
                  .containsAll(expected.get(route.agentKey()));
            });
  }

  /**
   * Lê evidência e aplicação das fontes revisadas sem confundi-las com data, origem ou próximo
   * cabeçalho.
   */
  @Test
  void extractsEvidenceHypothesisApplicationAndLimitsFromReviewedSources() {
    var card =
        service.getCatalog().cards().stream()
            .filter(item -> item.cardId().equals("RI1-AAB0EC98AB06"))
            .findFirst()
            .orElseThrow();
    assertThat(card.finding()).contains("420 profissionais").doesNotContain("##");
    assertThat(card.mechanism()).contains("hipótese de design");
    assertThat(card.commercialApplication())
        .contains("Marketing Hub")
        .doesNotContain("Resultado real");
    assertThat(card.risks())
        .contains("não de um experimento randomizado")
        .doesNotContain("Fonte original");
    var audio =
        service.getCatalog().cards().stream()
            .filter(item -> item.cardId().equals("RI1-AC85EF9A7C43"))
            .findFirst()
            .orElseThrow();
    assertThat(audio.finding()).contains("fMRI").doesNotStartWith("Derivado");
    assertThat(audio.commercialApplication()).contains("fontes concorrentes");
  }

  /** Falha de consulta não é convertida em biblioteca vazia aparentemente válida. */
  @Test
  void propagatesRepositoryFailureInsteadOfFabricatingEvidence() {
    when(repository.findByAgentKey("landing-generator"))
        .thenThrow(new IllegalStateException("Fonte indisponível"));
    assertThatThrownBy(() -> service.selectForAgentTask("landing-generator", "Jornada"))
        .hasMessage("Fonte indisponível");
  }

  /** Ausência de vínculos mantém tarefas gerais sem biblioteca e vídeos com a política anterior. */
  @Test
  void preservesLegacyBehaviorWithoutAssignments() {
    agents.values().forEach(agent -> agent.setInputs(new ArrayList<>()));
    assertThat(service.selectForAgentTask("landing-generator", "Jornada")).isNull();
    assertThat(service.selectForAgentTask("customer-agent", "Contrato financeiro")).isNull();
    assertThat(service.selectForAgentTask("customer-agent", "Vídeo Instagram").routes()).hasSize(1);
    assertThat(service.getCatalog().agentPolicies()).hasSize(4);
  }
}

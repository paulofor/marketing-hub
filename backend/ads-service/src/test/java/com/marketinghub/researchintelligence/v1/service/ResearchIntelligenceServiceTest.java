package com.marketinghub.researchintelligence.v1.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.researchintelligence.v1.service.select.ResearchIntelligenceRouteResponse;
import com.marketinghub.salesvideo.VideoProject;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Comprova a compilação e o roteamento determinístico dos artigos para o harness. */
class ResearchIntelligenceServiceTest {
  private ResearchIntelligenceService service;

  /** Carrega o mesmo catálogo empacotado usado pelo backend em produção. */
  @BeforeEach
  void setUp() {
    service = new ResearchIntelligenceService();
  }

  /** Entrega a qualquer projeto rotas pequenas, rastreáveis e específicas por responsabilidade. */
  @Test
  void shouldSelectAuditableCardsForAnyVideoProject() {
    var selection = service.selectForVideoProject(vega91());

    assertThat(selection.contractVersion()).isEqualTo(ResearchIntelligenceService.CONTRACT_VERSION);
    assertThat(selection.totalAvailableCards()).isGreaterThanOrEqualTo(56);
    assertThat(selection.contextFingerprint()).matches("[0-9a-f]{64}");
    assertThat(selection.routes())
        .extracting(ResearchIntelligenceRouteResponse::agentKey)
        .containsExactly(
            "communication-director", "videomaker", "customer-agent", "meta-ad-approver");

    ResearchIntelligenceRouteResponse apolo = route(selection.routes(), "videomaker");
    ResearchIntelligenceRouteResponse iris = route(selection.routes(), "communication-director");
    assertThat(collections(apolo)).contains("video", "prazer-audio-visual");
    assertThat(collections(iris)).contains("neuromarketing", "momentos-de-compra-b2c");
    assertThat(selection.routes())
        .allSatisfy(route -> assertThat(route.cards()).hasSizeBetween(2, 4));
    assertThat(selection.routes())
        .flatExtracting(ResearchIntelligenceRouteResponse::cards)
        .allSatisfy(
            card -> {
              assertThat(card.cardId()).matches("RI1-[0-9A-F]{12}");
              assertThat(card.sourcePath()).startsWith("pesquisas/").endsWith(".md");
              assertThat(card.sourceSha256()).matches("[0-9a-f]{64}");
              assertThat(card.finding()).hasSizeLessThanOrEqualTo(700);
              assertThat(card.evidenceKind()).isEqualTo("EXTERNAL_RESEARCH");
            });
    assertThat(selection.limitations()).anyMatch(limit -> limit.contains("não comprovam demanda"));
  }

  /** Expõe o catálogo global uma única vez, separado das seleções de cada projeto. */
  @Test
  void shouldExposeGlobalCatalogForCurrentAndFutureProjects() {
    var catalog = service.getCatalog();

    assertThat(catalog.contractVersion()).isEqualTo(ResearchIntelligenceService.CONTRACT_VERSION);
    assertThat(catalog.totalCompiledCards()).isGreaterThanOrEqualTo(56);
    assertThat(catalog.activeCards())
        .isPositive()
        .isLessThanOrEqualTo(catalog.totalCompiledCards());
    assertThat(catalog.cards()).hasSize(catalog.totalCompiledCards());
    assertThat(catalog.agentPolicies())
        .extracting("agentKey")
        .containsExactly(
            "communication-director", "videomaker", "customer-agent", "meta-ad-approver");
    assertThat(catalog.agentPolicies())
        .allSatisfy(
            policy -> {
              assertThat(policy.collections()).isNotEmpty();
              assertThat(policy.maxCardsPerContext())
                  .isEqualTo(ResearchIntelligenceService.MAX_CARDS_PER_ROUTE);
            });
  }

  /** Mantém seleção e impressão digital estáveis enquanto briefing e fontes não mudarem. */
  @Test
  void shouldKeepSelectionStableAndFingerprintTheContext() {
    var first = service.selectForVideoProject(vega91());
    var second = service.selectForVideoProject(vega91());
    VideoProject other = vega91();
    other.setTitle("Outro vídeo sobre uma rotina financeira");

    assertThat(second).isEqualTo(first);
    assertThat(service.selectForVideoProject(other).contextFingerprint())
        .isNotEqualTo(first.contextFingerprint());
  }

  /**
   * Seleciona a biblioteca para um projeto futuro sem depender do produto ou experimento piloto.
   */
  @Test
  void shouldRouteResearchForFutureProjectWithoutVegaIdentifiers() {
    VideoProject futureProject =
        VideoProject.builder()
            .id(847L)
            .productId(10L)
            .experimentId(203L)
            .title("Clareza financeira em uma rotina de cinco minutos")
            .objective("Converter atenção em diagnóstico financeiro")
            .storyText("Um homem organiza decisões financeiras pelo celular")
            .targetChannel("YOUTUBE_SHORTS")
            .funnelStage("AWARENESS")
            .commercialHypothesis("Visualizar a próxima ação reduz esforço percebido")
            .scriptText("Dor, mecanismo, demonstração e CTA")
            .scenePlan("Rotina; diagnóstico; resultado; CTA")
            .build();

    var selection = service.selectForVideoProject(futureProject);

    assertThat(selection.routes()).hasSize(4);
    assertThat(selection.routes())
        .allSatisfy(route -> assertThat(route.cards()).hasSizeBetween(2, 4));
    assertThat(selection.contextFingerprint())
        .isNotEqualTo(service.selectForVideoProject(vega91()).contextFingerprint());
  }

  /** Não injeta pesquisa audiovisual em tarefas fora do domínio e limita a rota do agente. */
  @Test
  void shouldScopeAgentTaskSelectionToAudiovisualContext() {
    assertThat(service.selectForAgentTask("customer-agent", "Revisar contrato financeiro"))
        .isNull();

    var selection =
        service.selectForAgentTask(
            "customer-agent", "Revisar criativo de vídeo vertical para Instagram");

    assertThat(selection.routes())
        .singleElement()
        .extracting("agentKey")
        .isEqualTo("customer-agent");
    assertThat(collections(selection.routes().getFirst()))
        .contains("neuromarketing", "prazer-audio-visual");
  }

  /** Exclui cartões vencidos em vez de apenas exibir uma validade sem aplicá-la. */
  @Test
  void shouldNeverDeliverExpiredResearchCards() {
    LocalDate currentDate = LocalDate.of(2026, 10, 20);
    service =
        new ResearchIntelligenceService(
            Clock.fixed(Instant.parse("2026-10-20T00:00:00Z"), ZoneOffset.UTC));

    var selectedCards =
        service.selectForVideoProject(vega91()).routes().stream()
            .flatMap(route -> route.cards().stream())
            .toList();

    assertThat(selectedCards)
        .extracting("collection")
        .doesNotContain("video", "momentos-de-compra-b2c");
    assertThat(selectedCards)
        .allSatisfy(
            card -> {
              if (card.validUntil() != null) {
                assertThat(card.validUntil()).isAfterOrEqualTo(currentDate);
              }
            });
  }

  /** Localiza uma rota obrigatória da seleção de teste. */
  private ResearchIntelligenceRouteResponse route(
      java.util.List<ResearchIntelligenceRouteResponse> routes, String agentKey) {
    return routes.stream()
        .filter(route -> agentKey.equals(route.agentKey()))
        .findFirst()
        .orElseThrow();
  }

  /** Resume as coleções cobertas por uma rota. */
  private Set<String> collections(ResearchIntelligenceRouteResponse route) {
    return route.cards().stream().map(card -> card.collection()).collect(Collectors.toSet());
  }

  /** Monta o briefing persistido do piloto sem criar exceção específica para seu ID. */
  private VideoProject vega91() {
    return VideoProject.builder()
        .id(91L)
        .productId(4L)
        .experimentId(91L)
        .title("Vega #91 - O espelho antes de sair")
        .objective("Converter reconhecimento íntimo em clique no diagnóstico MUSA")
        .storyText("Mulher diante do espelho aplica um microajuste antes de sair")
        .targetChannel("INSTAGRAM_REELS_STORIES")
        .funnelStage("AWARENESS_TO_DIAGNOSTIC")
        .commercialHypothesis("Dor reconhecida, recompensa sensorial e continuidade para o PDE")
        .scientificBasis("Cartões de vídeo, prazer audiovisual e neuromarketing")
        .hookText("Você se arruma, mas sente que ainda falta presença?")
        .scriptText("Espelho, microajuste, resultado e CTA")
        .scenePlan("Dor; resultado; mecanismo; prova; CTA")
        .voiceoverPlan("Voz íntima em português brasileiro")
        .soundtrackPlan("Trilha editorial discreta")
        .build();
  }
}

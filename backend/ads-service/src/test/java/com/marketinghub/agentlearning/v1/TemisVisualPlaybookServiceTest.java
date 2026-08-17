package com.marketinghub.agentlearning.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.CommercialPlanVisualAsset;
import com.marketinghub.planning.CommercialPlanVisualAssetStatus;
import com.marketinghub.repository.jpa.planning.CommercialPlanVisualAssetRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Responsabilidade: provar segregação, promoção governada e exemplos positivos do playbook visual.
 */
class TemisVisualPlaybookServiceTest {
  private GovernedAgentLearningService learningService;
  private CommercialPlanVisualAssetRepository assetRepository;
  private TemisVisualPlaybookService service;

  /** Prepara a governança e a Biblioteca Audiovisual sem dependências externas. */
  @BeforeEach
  void setUp() {
    learningService = mock(GovernedAgentLearningService.class);
    assetRepository = mock(CommercialPlanVisualAssetRepository.class);
    service = new TemisVisualPlaybookService(learningService, assetRepository, new ObjectMapper());
  }

  /** Usa baseline canônica e no máximo dois exemplos aprovados e compatíveis. */
  @Test
  void resolvesBaselineWithApprovedFeedAndStoryExamples() {
    CommercialPlan plan = plan();
    when(assetRepository.findByCommercialPlanIdAndStatusOrderByCreatedAtAsc(
            2L, CommercialPlanVisualAssetStatus.APPROVED))
        .thenReturn(
            List.of(asset(11L, "Post 01", "/post.png"), asset(12L, "Story 01", "/story.png")));

    var result = service.resolve(plan, "Anúncio Feed", List.of("ADS"), "2048x2048");

    assertThat(result.status()).isEqualTo("CANONICAL_BASELINE");
    assertThat(result.version()).isEqualTo("temis-visual-playbook-v1");
    assertThat(result.approvedExamples()).extracting("assetId").containsExactly(11L, 12L);
    assertThat(result.promotedRules()).anyMatch(rule -> rule.contains("produto real"));
  }

  /** Acrescenta somente regras de uma estratégia já promovida no mesmo contexto. */
  @Test
  void mergesPromotedStrategyWithoutReplacingCanonicalSafety() {
    CommercialPlan plan = plan();
    String context = service.contextKey(plan, "Story", List.of("ADS"), "1152x2048");
    when(learningService.promoted("meta-ad-approver", "VISUAL_CONTEXT", context))
        .thenReturn(
            List.of(
                new LearningExperimentResponse(
                    7L,
                    "meta-ad-approver",
                    "VISUAL_CONTEXT",
                    context,
                    "temis-visual-1-15",
                    "temis-visual-playbook-v1",
                    "PROMOTED",
                    8L,
                    "{}",
                    "{\"playbook\":{\"rules\":[\"Mostrar o nome Agenda Cheia na primeira dobra\"],\"avoid\":[\"Mockup genérico\"]}}",
                    "holdoutGain=8",
                    BigDecimal.valueOf(5),
                    BigDecimal.ZERO,
                    true,
                    true,
                    Instant.now(),
                    Instant.now(),
                    Instant.now())));
    when(assetRepository.findByCommercialPlanIdAndStatusOrderByCreatedAtAsc(
            2L, CommercialPlanVisualAssetStatus.APPROVED))
        .thenReturn(List.of());

    var result = service.resolve(plan, "Story", List.of("ADS"), "1152x2048");

    assertThat(result.status()).isEqualTo("PROMOTED");
    assertThat(result.version()).isEqualTo("temis-visual-1-15");
    assertThat(result.promotedRules())
        .anyMatch(rule -> rule.contains("produto real"))
        .contains("Mostrar o nome Agenda Cheia na primeira dobra");
    assertThat(result.avoid()).contains("Mockup genérico");
  }

  /** Cria o plano mínimo do mesmo contexto comercial. */
  private CommercialPlan plan() {
    CommercialPlan plan = new CommercialPlan();
    plan.setId(2L);
    plan.setName("Agenda Cheia Nail Design");
    return plan;
  }

  /** Cria um exemplo premium elegível para todos os usos comerciais. */
  private CommercialPlanVisualAsset asset(Long id, String label, String url) {
    CommercialPlanVisualAsset asset = new CommercialPlanVisualAsset();
    asset.setId(id);
    asset.setLabel(label);
    asset.setAssetUrl(url);
    asset.setPurposesJson("[\"DELIVERY\",\"LANDING\",\"ADS\",\"SOCIAL\"]");
    asset.setStatus(CommercialPlanVisualAssetStatus.APPROVED);
    return asset;
  }
}

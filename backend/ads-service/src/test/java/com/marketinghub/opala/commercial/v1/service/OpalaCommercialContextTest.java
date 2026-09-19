package com.marketinghub.opala.commercial.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.creative.Creative;
import com.marketinghub.creative.CreativeAgentReviewStatus;
import com.marketinghub.creative.CreativeStatus;
import com.marketinghub.creative.dto.CreativeMediaGovernanceEvidenceDto;
import com.marketinghub.creative.service.CreativeMediaGovernanceEvidenceService;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.service.ExperimentTargetingSelectionService;
import com.marketinghub.pde.PdeProductionSlot;
import com.marketinghub.pde.PdeProductionSlotStatus;
import com.marketinghub.product.Product;
import com.marketinghub.producttype.ProductTypeDefinition;
import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experiment.video.ExperimentVideoAssetRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.pde.PdeProductionSlotRepository;
import com.marketinghub.repository.jpa.targeting.TargetingElementRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Responsabilidade: impedir contaminação de produto, experimento e versão sem bloquear leitura
 * histórica.
 */
class OpalaCommercialContextTest {
  private final LearningSalesCycleRepository cycles = mock(LearningSalesCycleRepository.class);
  private final ExperimentRepository experiments = mock(ExperimentRepository.class);
  private final PdeProductionSlotRepository slots = mock(PdeProductionSlotRepository.class);
  private final CreativeRepository creatives = mock(CreativeRepository.class);
  private final CreativeMediaGovernanceEvidenceService mediaGovernanceEvidence =
      mock(CreativeMediaGovernanceEvidenceService.class);
  private final OpalaCommercialContext context =
      new OpalaCommercialContext(
          cycles,
          experiments,
          slots,
          mock(ExperimentVideoAssetRepository.class),
          mock(TargetingElementRepository.class),
          new ObjectMapper(),
          creatives,
          mock(ExperimentTargetingSelectionService.class),
          mock(OpalaCommercialFinancialPlan.class),
          mediaGovernanceEvidence);
  private final Product product =
      Product.builder()
          .id(4L)
          .slug("fixture-opala")
          .pdeExperienceJson("{\"experienceVersion\":\"fixture-v7\"}")
          .productTypeDefinition(ProductTypeDefinition.builder().code("PDE").build())
          .build();
  private final Experiment experiment =
      Experiment.builder().id(92L).product(product).status(ExperimentStatus.PLANNED).build();
  private final LearningSalesCycle cycle = new LearningSalesCycle();

  /** Prepara identidade e três slots com versões ou experimentos distintos. */
  @BeforeEach
  void setup() {
    cycle.setId(2L);
    cycle.setProductId(4L);
    cycle.setProductVersion("fixture-v12");
    cycle.setStatus("OPEN");
    cycle.setStage("PUBLICATION");
    when(cycles.findByExperimentId(92L)).thenReturn(Optional.of(cycle));
    when(experiments.findById(92L)).thenReturn(Optional.of(experiment));
    when(slots.findByProductSlugOrderBySlotCodeAsc("fixture-opala"))
        .thenReturn(
            List.of(
                slot(1L, 92L, "fixture-v12"),
                slot(2L, 93L, "fixture-v12"),
                slot(3L, 92L, "fixture-v11")));
  }

  /** O contexto dos agentes contém somente a entrada exata da ocorrência. */
  @Test
  void selectsOnlySameExperimentAndVersion() {
    assertThat(context.scope("experiment:92").experiment()).isSameAs(experiment);
    var snapshot = context.snapshot("experiment:92");
    var selected = snapshot.path("slots");
    assertThat(selected.size()).isEqualTo(1);
    assertThat(selected.get(0).path("id").asLong()).isEqualTo(1L);
    assertThat(snapshot.path("destinationUrl").asText()).isEqualTo("https://v12.sandbox.local");
    assertThat(snapshot.path("destinationSource").asText()).isEqualTo("VERSION_SLOT");
    assertThat(snapshot.path("productContract").path("experienceVersion").asText())
        .isEqualTo("fixture-v12");
  }

  /** Destino explícito divergente permanece visível para o gate, sem correção silenciosa. */
  @Test
  void preservesExplicitDestinationForMismatchDetection() {
    experiment.setFollowUpActionUrl("https://outro.sandbox.local");
    var snapshot = context.snapshot("experiment:92");
    assertThat(snapshot.path("destinationUrl").asText()).isEqualTo("https://outro.sandbox.local");
    assertThat(snapshot.path("destinationSource").asText()).isEqualTo("EXPERIMENT");
  }

  /** Duas candidatas da mesma identidade são ambíguas e não fornecem destino inferido. */
  @Test
  void refusesToInferDestinationFromAmbiguousCandidates() {
    when(slots.findByProductSlugOrderBySlotCodeAsc("fixture-opala"))
        .thenReturn(List.of(slot(1L, 92L, "fixture-v12"), slot(4L, 92L, "fixture-v12")));
    var snapshot = context.snapshot("experiment:92");
    assertThat(snapshot.path("destinationUrl").isNull()).isTrue();
    assertThat(snapshot.path("destinationSource").asText()).isEqualTo("AMBIGUOUS");
  }

  /** Slot inativo permanece auditável, mas não pode se tornar destino de uma nova preparação. */
  @ParameterizedTest
  @EnumSource(
      value = PdeProductionSlotStatus.class,
      names = {"PAUSED", "RETIRED"})
  void refusesInactiveCandidateAsDestination(PdeProductionSlotStatus status) {
    var retired = slot(1L, 92L, "fixture-v12");
    retired.setStatus(status);
    when(slots.findByProductSlugOrderBySlotCodeAsc("fixture-opala")).thenReturn(List.of(retired));

    var snapshot = context.snapshot("experiment:92");

    assertThat(snapshot.path("destinationUrl").isNull()).isTrue();
    assertThat(snapshot.path("destinationSource").asText()).isEqualTo("INELIGIBLE_SLOT");
    assertThat(snapshot.path("slots").size()).isEqualTo(1);
  }

  /** A seleção usa a identidade recebida e funciona em nova execução sem exceção por ID. */
  @Test
  void resolvesAnotherExecutionWithDifferentIdentifiers() {
    var anotherExperiment =
        Experiment.builder().id(192L).product(product).status(ExperimentStatus.PLANNED).build();
    cycle.setId(302L);
    cycle.setExperimentId(192L);
    cycle.setProductVersion("fixture-v13");
    when(cycles.findByExperimentId(192L)).thenReturn(Optional.of(cycle));
    when(experiments.findById(192L)).thenReturn(Optional.of(anotherExperiment));
    when(slots.findByProductSlugOrderBySlotCodeAsc("fixture-opala"))
        .thenReturn(List.of(slot(18L, 192L, "fixture-v13")));

    var snapshot = context.snapshot("experiment:192");

    assertThat(snapshot.path("cycleId").asLong()).isEqualTo(302L);
    assertThat(snapshot.path("experimentId").asLong()).isEqualTo(192L);
    assertThat(snapshot.path("productVersion").asText()).isEqualTo("fixture-v13");
    assertThat(snapshot.path("destinationSource").asText()).isEqualTo("VERSION_SLOT");
  }

  /** Nome comercial ou nome mineral não substituem o tipo oficial e a identidade do produto. */
  @Test
  void rejectsWrongTypeAndProduct() {
    product.getProductTypeDefinition().setCode("OTHER");
    assertThatThrownBy(() -> context.scope("experiment:92"))
        .hasMessageContaining("tipo cadastrado");
    product.getProductTypeDefinition().setCode("PDE");
    cycle.setProductId(9L);
    assertThatThrownBy(() -> context.scope("experiment:92")).hasMessageContaining("outro produto");
    assertThatThrownBy(() -> context.scope("product:4")).hasMessageContaining("experimento exato");
  }

  /** Depois da publicação, o histórico continua legível enquanto novas mutações são recusadas. */
  @Test
  void readsAfterReleaseWithoutAuthorizingMoreWork() {
    experiment.setFacebookReleaseRequestedAt(java.time.Instant.now());
    assertThat(context.snapshot("experiment:92").path("productVersion").asText())
        .isEqualTo("fixture-v12");
    assertThatThrownBy(() -> context.scope("experiment:92"))
        .hasMessageContaining("ainda não liberado");
    cycle.setStatus("CLOSED");
    assertThat(context.snapshot("experiment:92").path("cycleId").asLong()).isEqualTo(2L);
    assertThatThrownBy(() -> context.scope("experiment:92"))
        .hasMessageContaining("preparação comercial");
  }

  /** Entrega à revisão somente a linhagem final com o parecer e os direitos da mídia exata. */
  @Test
  void exposesFinalCreativeLineageAndVerifiedMediaGovernance() {
    var original =
        Creative.builder()
            .id(528L)
            .experiment(experiment)
            .versionNumber(1)
            .status(CreativeStatus.DRAFT)
            .format("VIDEO")
            .headline("Rascunho original")
            .build();
    var approved =
        Creative.builder()
            .id(529L)
            .sourceCreative(original)
            .experiment(experiment)
            .versionNumber(2)
            .status(CreativeStatus.READY)
            .format("VIDEO")
            .headline("Seu ajuste começa com o que você já tem")
            .agentReviewStatus(CreativeAgentReviewStatus.APPROVED)
            .build();
    var finalApproved =
        Creative.builder()
            .id(530L)
            .sourceCreative(approved)
            .experiment(experiment)
            .versionNumber(3)
            .status(CreativeStatus.READY)
            .format("VIDEO")
            .headline("Seu ajuste grátis com o que já tem")
            .primaryText(
                "Receba grátis seu 1º ajuste. Continue 7 dias por R$ 67, pagamento único, acesso por 90 dias, sem assinatura ou renovação.")
            .description("1º ajuste grátis")
            .agentReviewStatus(CreativeAgentReviewStatus.APPROVED)
            .agentReviewJson(
                "{\"decision\":\"APPROVED\",\"summary\":\"Mídia e oferta coerentes.\"}")
            .build();
    var pendingLeaf =
        Creative.builder()
            .id(531L)
            .sourceCreative(original)
            .experiment(experiment)
            .versionNumber(4)
            .status(CreativeStatus.READY)
            .format("VIDEO")
            .headline("Versão ainda sem parecer")
            .agentReviewStatus(CreativeAgentReviewStatus.PENDING)
            .build();
    var governance = mock(CreativeMediaGovernanceEvidenceDto.class);
    when(creatives.findByExperimentId(92L))
        .thenReturn(List.of(original, approved, finalApproved, pendingLeaf));
    when(mediaGovernanceEvidence.resolve(finalApproved)).thenReturn(governance);

    var snapshot = context.snapshot("experiment:92");

    assertThat(snapshot.path("creatives").get(0).path("finalCandidate").asBoolean()).isFalse();
    assertThat(snapshot.path("creatives").get(1).path("finalCandidate").asBoolean()).isFalse();
    var finalCandidate = snapshot.path("creatives").get(2);
    assertThat(finalCandidate.path("id").asLong()).isEqualTo(530L);
    assertThat(finalCandidate.path("sourceCreativeId").asLong()).isEqualTo(529L);
    assertThat(finalCandidate.path("finalCandidate").asBoolean()).isTrue();
    assertThat(finalCandidate.path("agentReview").path("decision").asText()).isEqualTo("APPROVED");
    assertThat(finalCandidate.has("mediaGovernanceEvidence")).isTrue();
    assertThat(snapshot.path("creatives").get(3).path("finalCandidate").asBoolean()).isFalse();
    verify(mediaGovernanceEvidence).resolve(finalApproved);
    verify(mediaGovernanceEvidence, never()).resolve(original);
    verify(mediaGovernanceEvidence, never()).resolve(approved);
    verify(mediaGovernanceEvidence, never()).resolve(pendingLeaf);
  }

  /** Cria um slot sintético com contrato candidato segregado, sem implantação real. */
  private PdeProductionSlot slot(Long id, Long experimentId, String version) {
    var slot = new PdeProductionSlot();
    slot.setId(id);
    slot.setSourceExperimentId(experimentId);
    slot.setExperienceVersion(version);
    slot.setStatus(PdeProductionSlotStatus.PLANNED);
    slot.setPublicUrl("https://v12.sandbox.local");
    slot.setDraftExperienceJson("{\"experienceVersion\":\"" + version + "\"}");
    return slot;
  }
}

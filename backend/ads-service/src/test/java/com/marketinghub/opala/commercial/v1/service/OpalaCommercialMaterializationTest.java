package com.marketinghub.opala.commercial.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.creative.service.video.VideoCreativeService;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.monitoring.dto.PostDeployPdeProductionSlotRequestDto;
import com.marketinghub.experiment.service.ExperimentTargetingSelectionService;
import com.marketinghub.pde.PdeProductionSlot;
import com.marketinghub.pde.PdeProductionSlotStatus;
import com.marketinghub.pde.service.*;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experiment.video.ExperimentVideoAssetRepository;
import com.marketinghub.repository.jpa.pde.PdeProductionSlotRepository;
import com.marketinghub.repository.jpa.targeting.TargetingElementRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;

/**
 * Responsabilidade: comprovar efeitos internos e impedir publicação ou troca silenciosa de oferta.
 */
class OpalaCommercialMaterializationTest {
  private final ObjectMapper json = new ObjectMapper();
  private final OpalaCommercialContext context = mock(OpalaCommercialContext.class);
  private final PdeProductionSlotRepository slots = mock(PdeProductionSlotRepository.class);
  private final PdeProductionSlotService slotService = mock(PdeProductionSlotService.class);
  private final VideoCreativeService creatives = mock(VideoCreativeService.class);
  private final PdeCommercialCheckoutContractResolver checkout =
      mock(PdeCommercialCheckoutContractResolver.class);
  private final ExperimentRepository experiments = mock(ExperimentRepository.class);
  private final ExperimentTargetingSelectionService selections =
      mock(ExperimentTargetingSelectionService.class);
  private final ExperimentVideoAssetRepository videos = mock(ExperimentVideoAssetRepository.class);
  private final OpalaCommercialMaterialization materialization =
      new OpalaCommercialMaterialization(
          context,
          slots,
          slotService,
          creatives,
          videos,
          checkout,
          experiments,
          selections,
          mock(TargetingElementRepository.class));
  private final Product product = Product.builder().id(4L).slug("fixture-opala").build();
  private final Experiment experiment =
      Experiment.builder().id(92L).product(product).unitPrice(new BigDecimal("67")).build();
  private final LearningSalesCycle cycle = new LearningSalesCycle();
  private OpalaCommercialContext.Scope scope;

  /** Prepara contrato comercial sintético, sem domínio ou fornecedor produtivo. */
  @BeforeEach
  void setup() throws Exception {
    cycle.setId(2L);
    cycle.setProductVersion("fixture-v12");
    product.setPdeExperienceJson("{\"experienceVersion\":\"fixture-v12\"}");
    experiment.setFollowUpActionUrl("https://opala.sandbox.local/entry");
    when(context.read(anyString())).thenAnswer(i -> json.readTree((String) i.getArgument(0)));
    scope = new OpalaCommercialContext.Scope(cycle, experiment);
    when(context.candidate(scope))
        .thenReturn(
            new OpalaCommercialContext.Candidate(
                List.of(),
                "https://opala.sandbox.local/entry",
                "EXPERIMENT",
                json.readTree(product.getPdeExperienceJson())));
  }

  /** Preparar a entrada cria somente rascunho próprio, nunca publica contrato ou container. */
  @Test
  void createsUnpublishedSlotForExactOccurrence() {
    materialization.apply("entry", scope, json.createObjectNode());
    var request = ArgumentCaptor.forClass(PostDeployPdeProductionSlotRequestDto.class);
    verify(slotService).saveProductionSlot(eq("fixture-opala"), eq(92L), request.capture());
    assertThat(request.getValue().status())
        .isEqualTo(com.marketinghub.pde.PdeProductionSlotStatus.PLANNED);
    assertThat(request.getValue().experienceVersion()).isEqualTo("fixture-v12");
    verifyNoMoreInteractions(slotService);
  }

  /** A candidata única fornece contrato e destino sem substituir o contrato publicado anterior. */
  @Test
  void bindsUniqueCandidateDestinationToExperiment() throws Exception {
    experiment.setFollowUpActionUrl(null);
    product.setPdeExperienceJson("{\"experienceVersion\":\"fixture-v7\"}");
    var candidate =
        PdeProductionSlot.builder()
            .id(8L)
            .slotCode("v8")
            .productSlug("fixture-opala")
            .publicUrl("https://v12.sandbox.local")
            .experienceVersion("fixture-v12")
            .status(PdeProductionSlotStatus.CANDIDATE)
            .sourceExperimentId(92L)
            .draftExperienceJson("{\"experienceVersion\":\"fixture-v12\"}")
            .build();
    when(context.candidate(scope))
        .thenReturn(
            new OpalaCommercialContext.Candidate(
                List.of(candidate),
                candidate.getPublicUrl(),
                "VERSION_SLOT",
                json.readTree(candidate.getDraftExperienceJson())));

    materialization.apply("entry", scope, json.createObjectNode());
    materialization.apply("entry", scope, json.createObjectNode());

    assertThat(experiment.getFollowUpActionUrl()).isEqualTo("https://v12.sandbox.local");
    verify(experiments, times(1)).save(experiment);
    verifyNoInteractions(slotService);
  }

  /** Uma URL explícita divergente não é substituída pela candidata encontrada. */
  @Test
  void rejectsExplicitDestinationMismatch() throws Exception {
    var candidate =
        PdeProductionSlot.builder()
            .id(8L)
            .publicUrl("https://v12.sandbox.local")
            .experienceVersion("fixture-v12")
            .status(PdeProductionSlotStatus.CANDIDATE)
            .sourceExperimentId(92L)
            .build();
    when(context.candidate(scope))
        .thenReturn(
            new OpalaCommercialContext.Candidate(
                List.of(candidate),
                "https://outro.sandbox.local",
                "EXPERIMENT",
                json.readTree("{\"experienceVersion\":\"fixture-v12\"}")));

    assertThatThrownBy(() -> materialization.apply("entry", scope, json.createObjectNode()))
        .hasMessageContaining("diverge");
    verifyNoInteractions(experiments, slotService);
  }

  /** URL explícita não reativa slot pausado ou retirado nem cria evidência de preparação. */
  @ParameterizedTest
  @EnumSource(
      value = PdeProductionSlotStatus.class,
      names = {"PAUSED", "RETIRED"})
  void rejectsInactiveSlotEvenWithExplicitDestination(PdeProductionSlotStatus status)
      throws Exception {
    var inactive =
        PdeProductionSlot.builder()
            .id(8L)
            .publicUrl("https://opala.sandbox.local/entry")
            .experienceVersion("fixture-v12")
            .status(status)
            .sourceExperimentId(92L)
            .build();
    when(context.candidate(scope))
        .thenReturn(
            new OpalaCommercialContext.Candidate(
                List.of(inactive),
                inactive.getPublicUrl(),
                "EXPERIMENT",
                json.readTree("{\"experienceVersion\":\"fixture-v12\"}")));

    assertThatThrownBy(() -> materialization.apply("entry", scope, json.createObjectNode()))
        .hasMessageContaining("pausado ou retirado");
    verifyNoInteractions(experiments, slotService);
  }

  /** Checkout existente e aprovado é vinculado sem criar uma preferência de pagamento. */
  @Test
  void bindsCanonicalCheckout() {
    when(checkout.resolve(product))
        .thenReturn(
            Optional.of(
                new PdeCommercialCheckoutContractResolver.CanonicalCheckout(
                    "TEST",
                    "https://checkout.sandbox.local/offer",
                    "fixture-offer",
                    new BigDecimal("67"),
                    "BRL",
                    "ONE_TIME")));
    materialization.apply("checkout", scope, json.createObjectNode());
    assertThat(experiment.getCommercialCheckoutUrl())
        .isEqualTo("https://checkout.sandbox.local/offer");
    verify(experiments).save(experiment);
  }

  /** Não corrige divergência de preço alterando silenciosamente a oferta aprovada. */
  @Test
  void rejectsPriceMismatch() {
    when(checkout.resolve(product))
        .thenReturn(
            Optional.of(
                new PdeCommercialCheckoutContractResolver.CanonicalCheckout(
                    "TEST",
                    "https://checkout.sandbox.local/offer",
                    "fixture-offer",
                    new BigDecimal("99"),
                    "BRL",
                    "ONE_TIME")));
    assertThatThrownBy(() -> materialization.apply("checkout", scope, json.createObjectNode()))
        .hasMessageContaining("preço");
    verifyNoInteractions(experiments);
  }

  /** O vídeo aprovado usa o serviço oficial com tenant derivado do ativo e restaura o contexto. */
  @Test
  void bindsApprovedVideoInItsTenantWithoutPublishing() throws Exception {
    var video = new com.marketinghub.experiment.video.ExperimentVideoAsset();
    video.setId(17L);
    video.setExperiment(experiment);
    when(videos.findById(17L)).thenReturn(Optional.of(video));
    when(context.snapshot("experiment:92"))
        .thenReturn(
            (com.fasterxml.jackson.databind.node.ObjectNode)
                json.readTree("{\"approvedVideos\":[{\"id\":17}]}"));
    var original = com.marketinghub.salesvideo.tenant.TenantContextHolder.getContext();
    when(creatives.create(eq(92L), eq(17L), any()))
        .thenAnswer(
            i -> {
              assertThat(com.marketinghub.salesvideo.tenant.TenantContextHolder.requireTenant())
                  .isEqualTo("default");
              return null;
            });
    materialization.apply(
        "creative",
        scope,
        json.readTree(
            "{\"videoAssetId\":17,\"headline\":\"Experiência de teste\",\"primaryText\":\"Valor útil no dia a dia\",\"description\":\"\"}"));
    verify(creatives).create(eq(92L), eq(17L), any());
    assertThat(com.marketinghub.salesvideo.tenant.TenantContextHolder.getContext())
        .isEqualTo(original);
    com.marketinghub.salesvideo.tenant.TenantContextHolder.clear();
  }

  /** Revogar um elemento antes aprovado reabre a preparação em vez de perpetuar o vínculo. */
  @Test
  void revokedAudienceRequiresFreshPreparation() throws Exception {
    var snapshot =
        (com.fasterxml.jackson.databind.node.ObjectNode)
            json.readTree(
                "{\"savedAudience\":[{\"targetingElementId\":7}],\"approvedAudienceElements\":[{\"id\":7}]}");
    when(context.snapshot("experiment:92")).thenReturn(snapshot);
    var instruction = json.readTree("{\"targetingElementIds\":[7]}");
    assertThat(materialization.current("targeting", scope, instruction)).isTrue();
    snapshot.putArray("approvedAudienceElements");
    assertThat(materialization.current("targeting", scope, instruction)).isFalse();
  }

  /** Uma instrução de público fora do catálogo aprovado não modifica o experimento. */
  @Test
  void rejectsUnapprovedAudience() throws Exception {
    when(context.snapshot("experiment:92")).thenReturn(json.createObjectNode());
    assertThatThrownBy(
            () ->
                materialization.apply(
                    "targeting", scope, json.readTree("{\"targetingElementIds\":[999]}")))
        .hasMessageContaining("fora da estratégia");
    verify(selections, never()).save(anyLong(), any());
  }
}

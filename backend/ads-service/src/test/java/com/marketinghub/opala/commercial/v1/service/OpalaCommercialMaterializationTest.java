package com.marketinghub.opala.commercial.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
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
import org.junit.jupiter.params.provider.ValueSource;
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
  private final OpalaCommercialVersionContract versionContract =
      mock(OpalaCommercialVersionContract.class);
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
          versionContract,
          experiments,
          selections,
          mock(TargetingElementRepository.class),
          mock(OpalaPrivateCreativeMaterialization.class));
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
    var canonical =
        new PdeCommercialCheckoutContractResolver.CanonicalCheckout(
            "TEST",
            "https://checkout.sandbox.local/offer",
            "fixture-offer",
            new BigDecimal("67"),
            "BRL",
            "ONE_TIME");
    when(versionContract.resolve(eq(scope), any()))
        .thenReturn(
            new OpalaCommercialVersionContract.Resolved(json.createObjectNode(), canonical, 90));
    materialization.apply("checkout", scope, json.createObjectNode());
    assertThat(experiment.getCommercialCheckoutUrl())
        .isEqualTo("https://checkout.sandbox.local/offer");
    verify(experiments).save(experiment);
  }

  /** Não corrige divergência de preço alterando silenciosamente a oferta aprovada. */
  @Test
  void rejectsPriceMismatch() {
    when(versionContract.resolve(eq(scope), any()))
        .thenThrow(new IllegalStateException("O preço do checkout candidato diverge"));
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

  /** Peça estática exata evita vídeo obrigatório e preserva o parecer independente pendente. */
  @ParameterizedTest
  @ValueSource(longs = {106L, 91006L})
  void reusesStaticDraftWithoutPublishingOrApproving(long experimentId) throws Exception {
    experiment.setId(experimentId);
    var snapshot = staticSnapshot();
    when(context.snapshot("experiment:" + experimentId)).thenReturn(snapshot);
    var instruction = staticInstruction();

    materialization.apply("creative", scope, instruction);
    materialization.apply("creative", scope, instruction);

    assertThat(materialization.current("creative", scope, instruction)).isTrue();
    assertThat(snapshot.path("creatives").get(0).path("agentReviewStatus").asText())
        .isEqualTo("PENDING");
    assertThat(snapshot.path("creatives").get(0).path("status").asText()).isEqualTo("DRAFT");
    verifyNoInteractions(creatives, videos, experiments, slotService);
  }

  /** Aceita a peça ainda sem solicitar revisão para não antecipar o modelo ao checkout. */
  @Test
  void selectsDraftBeforeIndependentReviewIsRequested() throws Exception {
    var snapshot = staticSnapshot();
    ((com.fasterxml.jackson.databind.node.ObjectNode) snapshot.path("creatives").get(0))
        .put("agentReviewStatus", "");
    when(context.snapshot("experiment:92")).thenReturn(snapshot);
    materialization.apply("creative", scope, staticInstruction());
    assertThat(materialization.current("creative", scope, staticInstruction())).isTrue();
    verifyNoInteractions(creatives, videos, experiments, slotService);
  }

  /** Dados ausentes, alterados ou reprovados não se transformam em conclusão estática. */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "missing",
        "ambiguous",
        "replaced",
        "copy",
        "destination",
        "http",
        "credentials",
        "malformed",
        "missingImage",
        "video",
        "rejected",
        "reviewRejected",
        "missingVideoChoice"
      })
  void rejectsUnusableStaticCreative(String defect) throws Exception {
    var snapshot = staticSnapshot();
    var item = (com.fasterxml.jackson.databind.node.ObjectNode) snapshot.path("creatives").get(0);
    var instruction = (com.fasterxml.jackson.databind.node.ObjectNode) staticInstruction();
    switch (defect) {
      case "missing" -> snapshot.putArray("creatives");
      case "ambiguous" -> snapshot.withArray("creatives").add(item.deepCopy().put("id", 1201));
      case "replaced" ->
          snapshot.withArray("creatives").addObject().put("id", 1201).put("sourceCreativeId", 1200);
      case "copy" -> item.put("headline", "Outra promessa");
      case "destination" -> item.put("destinationUrl", "https://other.sandbox.local");
      case "http" -> item.put("imageUrl", "http://media.sandbox.local/image.png");
      case "credentials" -> item.put("imageUrl", "https://user:fixture@media.sandbox.local/a.png");
      case "malformed" -> item.put("imageUrl", "not an image URL");
      case "missingImage" -> item.remove("imageUrl");
      case "video" -> item.put("videoUrl", "https://media.sandbox.local/other.mp4");
      case "rejected" -> item.put("status", "REJECTED");
      case "reviewRejected" -> item.put("agentReviewStatus", "ADJUST");
      case "missingVideoChoice" -> instruction.remove("videoAssetId");
      default -> throw new IllegalArgumentException("Cenário de regressão desconhecido");
    }
    when(context.snapshot("experiment:92")).thenReturn(snapshot);

    assertThat(materialization.current("creative", scope, instruction)).isFalse();
    assertThatThrownBy(() -> materialization.apply("creative", scope, instruction))
        .isInstanceOf(IllegalStateException.class);
    verifyNoInteractions(creatives, videos, experiments, slotService);
  }

  /** Monta somente um anúncio sintético do experimento consultado, ainda sem revisão comercial. */
  private com.fasterxml.jackson.databind.node.ObjectNode staticSnapshot() throws Exception {
    return (com.fasterxml.jackson.databind.node.ObjectNode)
        json.readTree(
            """
        {"approvedVideos":[],"creatives":[{"id":1200,"format":"IMAGE",
         "imageUrl":"https://media.sandbox.local/static.png","status":"DRAFT",
         "agentReviewStatus":"PENDING","headline":"Experiência de teste",
         "primaryText":"Valor útil no dia a dia","description":"",
         "destinationUrl":"https://opala.sandbox.local/entry"}]}
        """);
  }

  /** Usa o contrato já versionado: vídeo explicitamente nulo e copy integral da peça escolhida. */
  private JsonNode staticInstruction() throws Exception {
    return json.readTree(
        """
        {"videoAssetId":null,"headline":"Experiência de teste",
         "primaryText":"Valor útil no dia a dia","description":"","targetingElementIds":[]}
        """);
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

package com.marketinghub.product.modeling.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.product.Product;
import com.marketinghub.product.modeling.v1.ProductModeling;
import com.marketinghub.product.modeling.v1.service.save.SaveModelingRequest;
import com.marketinghub.productdiscovery.v1.*;
import com.marketinghub.productdiscovery.v1.service.*;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import com.marketinghub.repository.jpa.productdiscovery.ProductDiscoveryCycleRepository;
import com.marketinghub.repository.jpa.productmodeling.ProductModelingRepository;
import java.util.*;
import org.junit.jupiter.api.*;

/** Homologa preparo, fila, replay e identidade sem consumir modelos ou alterar produção. */
class ModelingServiceTest {
  final ProductModelingRepository models = mock(ProductModelingRepository.class);
  final ProductRepository products = mock(ProductRepository.class);
  final ProductDiscoveryCycleRepository cycles = mock(ProductDiscoveryCycleRepository.class);
  final ProductDiscoveryService discovery = mock(ProductDiscoveryService.class);
  final AgentTaskRepository tasks = mock(AgentTaskRepository.class);
  final ObjectMapper json = new ObjectMapper();
  final ModelingHandoff handoff =
      new ModelingHandoff(
          models,
          json,
          mock(
              com.marketinghub.repository.jpa.opportunitydossier.OpportunityDossierRepository
                  .class),
          mock(
              com.marketinghub.repository.jpa.opportunitydossier.OpportunityEvidenceRepository
                  .class),
          tasks);
  final ModelingService service =
      new ModelingService(
          models, products, cycles, discovery, json, handoff, mock(ModelingAdoption.class), tasks);
  final ProductDiscoveryCycle cycle = new ProductDiscoveryCycle();

  /** Monta um ciclo sintético; nenhuma dependência externa é chamada. */
  @BeforeEach
  void setup() {
    cycle.setId(9001L);
    cycle.setStatus(ProductDiscoveryCycleStatus.DRAFT);
    when(models.saveAndFlush(any()))
        .thenAnswer(
            i -> {
              ProductModeling m = i.getArgument(0);
              m.setId(91L);
              return m;
            });
    when(models.save(any())).thenAnswer(i -> i.getArgument(0));
    when(cycles.findById(9001L)).thenReturn(Optional.of(cycle));
    when(cycles.findByIdForUpdate(9001L)).thenReturn(Optional.of(cycle));
    var research = mock(ProductDiscoveryCycleResponse.class);
    when(research.id()).thenReturn(9001L);
    when(discovery.prepareCycle(any())).thenReturn(research);
  }

  /** Preparar produto existente não o duplica, não inicia Argos e preserva as fontes. */
  @Test
  void preparesExistingProductWithoutExecution() {
    var product = new Product();
    product.setId(42L);
    product.setInternalName("Outra estrela");
    when(products.findLockedById(42L)).thenReturn(Optional.of(product));
    var response =
        service.prepare(
            new SaveModelingRequest(
                UUID.randomUUID(),
                42L,
                null,
                "Encontrar combinações úteis",
                List.of("https://reference.example/")));
    assertThat(response.productId()).isEqualTo(42L);
    assertThat(response.status()).isEqualTo("DRAFT");
    assertThat(response.researchScope()).isEqualTo("INTERNATIONAL_TO_BR_V1");
    assertThat(response.targetMarket()).isEqualTo("Brasil");
    assertThat(response.referenceUrls()).containsExactly("https://reference.example/");
    assertThat(response.researchUrl()).isEqualTo("/product-discovery/cycles/9001");
    verify(products, never()).save(any());
    verify(discovery)
        .prepareCycle(
            argThat(
                r ->
                    r.referenceSources().contains("reference.example")
                        && r.objective().startsWith("MODEL_REFERENCES_V1 INTERNATIONAL_TO_BR_V1")
                        && "BR".equals(r.country())
                        && "pt-BR".equals(r.language())));
  }

  /** Um novo produto ainda é intenção, não cadastro fabricado ou campanha autorizada. */
  @Test
  void preparesNewProductWithoutCreatingCommercialProduct() {
    var response =
        service.prepare(
            new SaveModelingRequest(
                UUID.randomUUID(),
                null,
                "Nova proposta",
                "Poupar esforço",
                List.of("https://reference.example/")));
    assertThat(response.productId()).isNull();
    assertThat(response.productName()).isEqualTo("Nova proposta");
    assertThat(response.status()).isEqualTo("DRAFT");
    verifyNoInteractions(products);
  }

  /** Recusa URLs de rede interna, tokens na URL e protocolos inadequados antes de persistir. */
  @Test
  void rejectsUnsafeReferences() {
    for (String url :
        List.of(
            "http://example.com/",
            "https://127.0.0.1/",
            "https://user:secret@example.com/",
            "https://a.local/",
            "https://example.com/?token=secret",
            "https://example.com:443/")) {
      assertThatThrownBy(() -> ModelingService.validateUrls(List.of(url)))
          .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }
    verifyNoInteractions(models, discovery);
  }

  /** A mesma entrada após perda da resposta reutiliza a pesquisa e não chama Argos novamente. */
  @Test
  void replaysSamePreparation() {
    UUID key = UUID.randomUUID();
    var m = new ProductModeling();
    m.setId(91L);
    m.setRequestKey(key.toString());
    m.setProductName("Nova");
    m.setDesiredOutcome("Poupar esforço");
    m.setReferencesJson("[\"https://reference.example/\"]");
    m.setDiscoveryCycleId(9001L);
    when(models.findByRequestKey(key.toString())).thenReturn(Optional.of(m));
    service.prepare(
        new SaveModelingRequest(
            key, null, "Nova", "Poupar esforço", List.of("https://reference.example/")));
    verifyNoInteractions(discovery);
    assertThatThrownBy(
            () ->
                service.prepare(
                    new SaveModelingRequest(
                        key,
                        null,
                        "Nova",
                        "Outro resultado",
                        List.of("https://reference.example/"))))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
  }

  /** STOP impede iniciar e uma retomada repetida não cria outro ciclo. */
  @Test
  void respectsStopAndIdempotentStart() {
    var m = new ProductModeling();
    m.setId(91L);
    m.setProductId(42L);
    m.setReferencesJson("[]");
    m.setDiscoveryCycleId(9001L);
    when(models.findLockedById(91L)).thenReturn(Optional.of(m));
    var p = new Product();
    p.setAutomaticExecutionEnabled(false);
    when(products.findById(42L)).thenReturn(Optional.of(p));
    assertThatThrownBy(() -> service.start(91L))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    p.setAutomaticExecutionEnabled(true);
    cycle.setStatus(ProductDiscoveryCycleStatus.DRAFT);
    service.start(91L);
    service.start(91L);
    verify(discovery, times(2)).startPreparedCycle(9001L);
  }

  /**
   * Produtos distintos recebem referências distintas e a descoberta legada conserva seu contrato.
   */
  @Test
  void preservesIdentityAndLegacyHandoff() {
    for (long id : List.of(4L, 99122L)) {
      var m = new ProductModeling();
      m.setId(id + 100);
      m.setProductId(id);
      m.setDiscoveryCycleId(id + 200);
      m.setReferencesJson("[\"https://reference.example/\"]");
      m.setDesiredOutcome("Resultado próprio");
      when(models.findByDiscoveryCycleId(id + 200)).thenReturn(Optional.of(m));
      assertThat(handoff.sourceReference(id + 200))
          .isEqualTo("product-discovery-cycle:" + (id + 200));
      assertThat(handoff.context(id + 200).path("identityMode").asText()).isEqualTo("PRESERVE");
      assertThat(handoff.context(id + 200).path("commercialAuthorizationInherited").asBoolean())
          .isFalse();
      assertThat(handoff.context(id + 200).path("researchScope").asText())
          .isEqualTo("MARKET_REFERENCES_V1");
      m.setResearchScope("INTERNATIONAL_TO_BR_V1");
      assertThat(handoff.context(id + 200).path("researchScope").asText())
          .isEqualTo("INTERNATIONAL_TO_BR_V1");
      assertThat(handoff.context(id + 200).path("targetMarket").asText()).isEqualTo("BR");
    }
    assertThat(handoff.sourceReference(9999L)).isEqualTo("product-discovery-cycle:9999");
    assertThat(handoff.context(9999L).isNull()).isTrue();
  }
}

package com.marketinghub.communication.v1;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.CommunicationMaterializationContextProvider;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.automation.v1.ProcessRun;
import com.marketinghub.businessprocess.automation.v1.service.ProcessRunContext;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

/** Responsabilidade: provar mudança semântica de entrada sem repetir mensagem ou tarefas pagas. */
class IrisCreativeExecutionInputVersionTest {
  private final ObjectMapper json = new ObjectMapper();

  /** Mudança privada altera somente a proteção criativa para o caso original e outra identidade. */
  @ParameterizedTest
  @ValueSource(longs = {10L, 110L})
  void recognizesPrivateDeclarationWithoutInvalidatingMessage(long productId) {
    String reference = "experiment:" + (productId + 92);
    var current = new AtomicReference<>(input(reference));
    var provider = provider(reference, current);
    var run = new ProcessRun();
    run.setProductId(productId);
    run.setProcessDefinitionId(94164L);
    run.setSourceReference(reference);
    var products = mock(ProductRepository.class);
    when(products.findById(productId))
        .thenReturn(Optional.of(Product.builder().id(productId).build()));
    var definitions = mock(BusinessProcessDefinitionRepository.class);
    var definition = new BusinessProcessDefinition();
    definition.setProcessCode("creative-production-approval");
    when(definitions.findById(94164L)).thenReturn(Optional.of(definition));
    var context =
        new ProcessRunContext(
            null,
            definitions,
            null,
            null,
            products,
            json,
            null,
            mock(com.marketinghub.repository.jpa.agenttask.AgentTaskRepository.class));
    inject(context, provider);
    String legacy = context.inputVersion(run);
    String messageHash = IrisCommunicationInputFingerprint.hash(json, current.get());
    var enriched = new LinkedHashMap<>(current.get());
    enriched.put(PrivateCreativePreparationContext.FIELD, declaration(reference));
    current.set(enriched);
    assertThat(context.inputVersion(run)).startsWith(legacy + "|").isNotEqualTo(legacy);
    assertThat(IrisCommunicationInputFingerprint.hash(json, enriched)).isEqualTo(messageHash);
    definition.setProcessCode("pde-communication-sales-journey");
    assertThat(context.inputVersion(run)).isEqualTo(legacy);
    assertThat(
            CommunicationMaterializationContextProvider.empty()
                .executionInputVersion("creative-production-approval", reference))
        .isEmpty();
  }

  /** Ordem, escala numérica e resultados posteriores não representam outra entrada paga. */
  @Test
  void ignoresRepresentationAndDownstreamArtifacts() {
    String reference = "experiment:95102";
    var first = new LinkedHashMap<>(input(reference));
    var contract = declaration(reference);
    first.put(PrivateCreativePreparationContext.FIELD, contract);
    var current = new AtomicReference<Map<String, Object>>(first);
    var provider = provider(reference, current);
    var original = provider.executionInputVersion("creative-production-approval", reference);
    var reordered = new LinkedHashMap<String, Object>();
    new ArrayList<>(contract.keySet())
        .reversed()
        .forEach(key -> reordered.put(key, contract.get(key)));
    reordered.put("videoProductionRequestId", new BigDecimal("0.000"));
    var after = new LinkedHashMap<>(first);
    after.put(PrivateCreativePreparationContext.FIELD, reordered);
    after.put("communicationArtifacts", List.of(Map.of("taskId", 95999, "status", "COMPLETED")));
    after.put("estimatedCostUsd", new BigDecimal("9.99"));
    after.put("updatedAt", "2026-10-07T18:00:00Z");
    current.set(after);
    assertThat(provider.executionInputVersion("creative-production-approval", reference))
        .isEqualTo(original);
    assertThat(original).isPresent();
  }

  /** Pedido governado posterior altera a entrada sem aprovar sua produção ou custo. */
  @Test
  void recognizesGovernedVideoRequestAsChangedInput() {
    String reference = "experiment:95103";
    var before = new LinkedHashMap<>(input(reference));
    before.put(PrivateCreativePreparationContext.FIELD, declaration(reference));
    var current = new AtomicReference<Map<String, Object>>(before);
    var provider = provider(reference, current);
    var brief = provider.executionInputVersion("creative-production-approval", reference);
    var request = declaration(reference);
    request.put("audiovisualProductionIntent", "GOVERNED_PRODUCTION_REQUESTED");
    request.put("videoProductionRequestId", 95600L);
    var after = new LinkedHashMap<>(before);
    after.put(PrivateCreativePreparationContext.FIELD, request);
    current.set(after);
    var production = provider.executionInputVersion("creative-production-approval", reference);
    assertThat(production).isPresent().isNotEqualTo(brief);
    assertThat(request)
        .containsEntry("spendAuthorized", false)
        .containsEntry("publicationAuthorized", false);
  }

  /** Contrato legado, indisponível, comercial ou de outra identidade não muda a proteção antiga. */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "LEGACY",
        "UNAVAILABLE",
        "NOT_READY",
        "COMMERCIAL",
        "REFERENCE",
        "VERSION",
        "SPEND",
        "PUBLICATION",
        "EVIDENCE",
        "INTENT"
      })
  void preservesLegacyAndRejectsInvalidBoundaries(String boundary) {
    String reference = "experiment:95104";
    var input = new LinkedHashMap<>(input(reference));
    var contract = declaration(reference);
    switch (boundary) {
      case "UNAVAILABLE" -> input.put("availability", "UNAVAILABLE");
      case "NOT_READY" -> input.put("inputReadiness", "BLOCKED");
      case "COMMERCIAL" -> input.put("mode", "COMMERCIAL");
      case "REFERENCE" -> contract.put("sourceReference", "experiment:99999");
      case "VERSION" -> contract.put("contractVersion", "UNKNOWN");
      case "SPEND" -> contract.put("spendAuthorized", true);
      case "PUBLICATION" -> contract.put("publicationAuthorized", true);
      case "EVIDENCE" -> contract.put("commercialEvidenceClaimed", true);
      case "INTENT" -> contract.put("audiovisualProductionIntent", "UNKNOWN");
      case "LEGACY" -> {}
      default -> throw new AssertionError("Caso não documentado.");
    }
    if (!"LEGACY".equals(boundary)) input.put(PrivateCreativePreparationContext.FIELD, contract);
    var provider = provider(reference, new AtomicReference<Map<String, Object>>(input));
    assertThat(provider.executionInputVersion("creative-production-approval", reference)).isEmpty();
  }

  /** Usa o provedor real com a fonte persistida substituída por um contrato controlado local. */
  private IrisCommunicationMaterializationContextProvider provider(
      String reference, AtomicReference<Map<String, Object>> current) {
    var provider =
        spy(
            new IrisCommunicationMaterializationContextProvider(
                null, null, null, null, null, json));
    doAnswer(ignored -> Optional.of(current.get())).when(provider).resolve(reference);
    return provider;
  }

  /** Injeta o contrato existente como no container Spring, sem dependência concreta no núcleo. */
  private void inject(
      ProcessRunContext context, CommunicationMaterializationContextProvider provider) {
    var factory = new DefaultListableBeanFactory();
    factory.registerSingleton("communicationInputs", provider);
    var injector = new AutowiredAnnotationBeanPostProcessor();
    injector.setBeanFactory(factory);
    injector.processInjection(context);
  }

  /** Define somente contexto privado apto à preparação sem sinais de execução posterior. */
  private Map<String, Object> input(String reference) {
    return Map.of(
        "availability",
        "AVAILABLE",
        "inputReadiness",
        "READY",
        "mode",
        "LEARNING_CYCLE_PRIVATE",
        "sourceReference",
        reference);
  }

  /** Declara o mesmo contrato restrito emitido pelo backend, sem mídia ou pagamento. */
  private LinkedHashMap<String, Object> declaration(String reference) {
    var result = new LinkedHashMap<String, Object>();
    result.put("contractVersion", PrivateCreativePreparationContext.VERSION);
    result.put("scope", "PRIVATE_PREPARATION");
    result.put("sourceReference", reference);
    result.put("prototypeVersion", "QA_PRIVATE_V3");
    result.put("nonAudiovisualEvidenceRequired", true);
    result.put("audiovisualProductionIntent", "BRIEF_ONLY");
    result.put("videoProductionRequestId", 0L);
    result.put("publicationAuthorized", false);
    result.put("spendAuthorized", false);
    result.put("commercialEvidenceClaimed", false);
    return result;
  }
}

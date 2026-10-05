package com.marketinghub.businessprocess.automation.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.automation.v1.ProcessRun;
import com.marketinghub.businessprocesschain.*;
import com.marketinghub.product.Product;
import com.marketinghub.producttype.ProductTypeDefinition;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import com.marketinghub.repository.jpa.processautomation.ProcessRunRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger ida e retorno contextual, versões e segregação das chamadas. */
class ProcessRunNavigationTest {
  private final BusinessProcessDefinitionRepository processes =
      mock(BusinessProcessDefinitionRepository.class);
  private final BusinessProcessChainDefinitionRepository chains =
      mock(BusinessProcessChainDefinitionRepository.class);
  private final ProcessRunRepository runs = mock(ProcessRunRepository.class);
  private final ProductRepository products = mock(ProductRepository.class);
  private final ProcessRunNavigation navigation =
      new ProcessRunNavigation(processes, chains, runs, products, new ObjectMapper());

  /** Links de reserva preservam a referência exata com e sem ciclo, sem inferir o experimento. */
  @Test
  void linksExactQueueReservationWithEncodedReferenceAndAnchor() {
    var queued = run(9L, 63L);
    queued.setSourceReference("experiment:93001");
    queued.setCurrentActivityId("economics");
    assertThat(navigation.executionUrl(queued))
        .isEqualTo(
            "/products/4/value-chain-history/processes/63/activities?chainId=14&learningCycleId=2&sourceReference=experiment%3A93001#activity-economics");
    queued.setLearningCycleId(null);
    queued.setSourceReference("product:4@validation v1&variant=2");
    queued.setCurrentActivityId(null);
    assertThat(navigation.executionUrl(queued))
        .isEqualTo(
            "/products/4/value-chain-history/processes/63/activities?chainId=14&sourceReference=product%3A4%40validation%20v1%26variant%3D2");
  }

  /**
   * Mantém links após conclusão e preserva cadeia, ciclo e referência sem carregar outro contexto.
   */
  @Test
  void linksBothDirectionsBeforeAndAfterCompletion() {
    var parent =
        definition(
            63L,
            "parent",
            "{\"nodes\":[{\"id\":\"creatives\",\"type\":\"TASK\",\"label\":\"Criativos\",\"subprocessCode\":\"child\"}]}");
    var child = definition(64L, "child", "{\"nodes\":[]}");
    var chain = new BusinessProcessChainDefinition();
    var item = new BusinessProcessChainItem();
    item.setProcessDefinition(parent);
    chain.getItems().add(item);
    when(chains.findById(14L)).thenReturn(Optional.of(chain));
    when(processes.findFirstByProcessCodeAndStatusOrderByVersionNumberDesc("child", "PUBLISHED"))
        .thenReturn(Optional.of(child));
    var parentRun = run(1L, 63L);
    var childRun = run(2L, 64L);
    assertThat(navigation.children(parentRun))
        .singleElement()
        .satisfies(
            link -> {
              assertThat(link.processDefinitionId()).isEqualTo(64L);
              assertThat(link.navigationUrl())
                  .isEqualTo(
                      "/products/4/value-chain-history/processes/64/activities?chainId=14&learningCycleId=2&sourceReference=experiment%3A92");
            });
    assertThat(navigation.parents(childRun))
        .singleElement()
        .satisfies(
            link ->
                assertThat(link.navigationUrl())
                    .endsWith(
                        "/63/activities?chainId=14&learningCycleId=2&sourceReference=experiment%3A92#activity-creatives"));
    childRun.setParentRunId(1L);
    childRun.setStatus("COMPLETED");
    when(runs.findById(1L)).thenReturn(Optional.of(parentRun));
    when(runs.findAllByParentRunId(1L)).thenReturn(java.util.List.of(childRun));
    var newerChild = definition(65L, "child", "{\"nodes\":[]}");
    newerChild.setVersionNumber(8);
    when(processes.findFirstByProcessCodeAndStatusOrderByVersionNumberDesc("child", "PUBLISHED"))
        .thenReturn(Optional.of(newerChild));
    assertThat(navigation.children(parentRun))
        .singleElement()
        .satisfies(link -> assertThat(link.processDefinitionId()).isEqualTo(64L));
    parent.setStatus("RETIRED");
    assertThat(navigation.parents(childRun))
        .singleElement()
        .satisfies(link -> assertThat(link.processVersion()).isEqualTo(7));
    parentRun.setProductId(10L);
    assertThatThrownBy(() -> navigation.parents(childRun)).hasMessageContaining("outro contexto");
  }

  /**
   * Preserva a referência sem ciclo na ida, no retorno e no atalho da atividade, sem duplicá-la.
   */
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {
        "experiment:93097",
        "product:93011@validation v1&variant=2",
        "commercial-plan:93034@v3:journey"
      })
  void preservesExactReferenceAcrossSubprocessLinksWithoutCycle(String reference) throws Exception {
    var parent =
        definition(
            93013L,
            "communication",
            "{\"nodes\":[{\"id\":\"destination\",\"type\":\"TASK\",\"subprocessCode\":\"landing\"}]}");
    var child = definition(93014L, "landing", "{\"nodes\":[]}");
    when(processes.findFirstByProcessCodeAndStatusOrderByVersionNumberDesc("landing", "PUBLISHED"))
        .thenReturn(Optional.of(child));
    var parentRun = run(93044L, parent.getId());
    var childRun = run(93045L, child.getId());
    parentRun.setLearningCycleId(null);
    childRun.setLearningCycleId(null);
    parentRun.setSourceReference(reference);
    childRun.setSourceReference(reference);
    parentRun.setCurrentActivityId("communicationContract");
    childRun.setParentRunId(parentRun.getId());
    when(runs.findById(parentRun.getId())).thenReturn(Optional.of(parentRun));
    String encoded =
        java.net.URLEncoder.encode(reference, java.nio.charset.StandardCharsets.UTF_8)
            .replace("+", "%20");

    assertThat(navigation.children(parentRun))
        .singleElement()
        .satisfies(
            link ->
                assertThat(link.navigationUrl())
                    .isEqualTo(
                        "/products/4/value-chain-history/processes/93014/activities?chainId=14&sourceReference="
                            + encoded));
    assertThat(navigation.parents(childRun))
        .singleElement()
        .satisfies(
            link ->
                assertThat(link.navigationUrl())
                    .isEqualTo(
                        "/products/4/value-chain-history/processes/93013/activities?chainId=14&sourceReference="
                            + encoded
                            + "#activity-destination"));
    assertThat(navigation.activityUrl(parentRun, "communicationContract"))
        .isEqualTo(navigation.executionUrl(parentRun))
        .containsOnlyOnce("sourceReference=");
    assertThat(navigation.activityUrl(childRun, "customer"))
        .endsWith("&sourceReference=" + encoded + "#activity-customer");
    verifyNoInteractions(products);
    String evidence = System.getProperty("process.navigation.evidence");
    if (evidence != null && reference.startsWith("experiment:")) {
      java.nio.file.Files.writeString(
          java.nio.file.Path.of(evidence),
          new ObjectMapper()
              .writeValueAsString(
                  java.util.Map.of(
                      "sourceReference", reference,
                      "children", navigation.children(parentRun),
                      "parents", navigation.parents(childRun),
                      "activityUrl", navigation.activityUrl(parentRun, "communicationContract"))));
    }
  }

  /** Não inventa referência quando a projeção anterior ao início ainda não possui esse vínculo. */
  @Test
  void keepsUnboundProjectionWithoutSyntheticReference() {
    var projected = run(null, 93013L);
    projected.setLearningCycleId(null);
    projected.setSourceReference(null);
    assertThat(navigation.activityUrl(projected, "journey"))
        .isEqualTo(
            "/products/4/value-chain-history/processes/93013/activities?chainId=14#activity-journey");
  }

  /** Mostra somente chamadas da cadeia selecionada, inclusive quando mais de um pai é legítimo. */
  @Test
  void listsAllCallSitesAndRejectsUnrelatedParent() {
    var child = definition(64L, "child", "{\"nodes\":[]}");
    var parent =
        definition(
            63L,
            "parent",
            "{\"nodes\":[{\"id\":\"a\",\"type\":\"TASK\",\"subprocessCode\":\"child\"},{\"id\":\"b\",\"type\":\"TASK\",\"subprocessCode\":\"child\"}]}");
    var unrelated = definition(90L, "unrelated", "{\"nodes\":[]}");
    when(processes.findFirstByProcessCodeAndStatusOrderByVersionNumberDesc("child", "PUBLISHED"))
        .thenReturn(Optional.of(child));
    var chain = new BusinessProcessChainDefinition();
    var item = new BusinessProcessChainItem();
    item.setProcessDefinition(parent);
    chain.getItems().add(item);
    when(chains.findById(14L)).thenReturn(Optional.of(chain));
    assertThat(navigation.parents(run(2L, 64L)))
        .extracting(r -> r.activityId())
        .containsExactly("a", "b");
    assertThat(navigation.parents(run(3L, 90L))).isEmpty();
  }

  /** Navega somente pela rota correspondente ao tipo oficial e à versão declarada no BPM. */
  @Test
  void followsTypedSubprocessRoute() {
    var parent =
        definition(
            56L,
            "pde-commercial-homologation-activation",
            "{\"nodes\":[{\"id\":\"commercialPreparation\",\"type\":\"TASK\",\"subprocessRoutes\":[{\"productTypeCode\":\"PDE\",\"subprocessCode\":\"opala-commercial-preparation-v1\",\"subprocessVersion\":1},{\"productTypeCode\":\"QUARTZ\",\"subprocessCode\":\"quartzo-preparation\",\"subprocessVersion\":2}]}]}");
    var opala = definition(77L, "opala-commercial-preparation-v1", "{\"nodes\":[]}");
    opala.setVersionNumber(1);
    when(processes.findByProcessCodeAndVersionNumber("opala-commercial-preparation-v1", 1))
        .thenReturn(Optional.of(opala));
    when(products.findById(4L))
        .thenReturn(
            Optional.of(
                Product.builder()
                    .id(4L)
                    .productTypeDefinition(ProductTypeDefinition.builder().code("PDE").build())
                    .build()));
    var chain = new BusinessProcessChainDefinition();
    var item = new BusinessProcessChainItem();
    item.setProcessDefinition(parent);
    chain.getItems().add(item);
    when(chains.findById(14L)).thenReturn(Optional.of(chain));

    assertThat(navigation.children(run(1L, 56L)))
        .singleElement()
        .satisfies(
            relation -> {
              assertThat(relation.processDefinitionId()).isEqualTo(77L);
              assertThat(relation.activityId()).isEqualTo("commercialPreparation");
            });
    assertThat(navigation.parents(run(2L, 77L)))
        .singleElement()
        .satisfies(relation -> assertThat(relation.processDefinitionId()).isEqualTo(56L));
    verify(processes, never()).findByProcessCodeAndVersionNumber("quartzo-preparation", 2);
  }

  /** Prepara uma definição versionada mínima do catálogo. */
  private BusinessProcessDefinition definition(Long id, String code, String diagram) {
    var d = new BusinessProcessDefinition();
    d.setId(id);
    d.setProcessCode(code);
    d.setName(code);
    d.setVersionNumber(7);
    d.setStatus("PUBLISHED");
    d.setDiagramJson(diagram);
    when(processes.findById(id)).thenReturn(Optional.of(d));
    return d;
  }

  /** Prepara identidades sintéticas iguais às usadas pelos links, sem escrita externa. */
  private ProcessRun run(Long id, Long process) {
    var r = new ProcessRun();
    r.setId(id);
    r.setProductId(4L);
    r.setProcessDefinitionId(process);
    r.setChainDefinitionId(14L);
    r.setLearningCycleId(2L);
    r.setSourceReference("experiment:92");
    return r;
  }
}

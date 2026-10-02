package com.marketinghub.catalogovivo.v1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocesschain.BusinessProcessChainDefinition;
import com.marketinghub.businessprocesschain.BusinessProcessChainItem;
import com.marketinghub.catalogovivo.v1.service.adoption.OpalaAdoption;
import com.marketinghub.repository.jdbc.catalogovivo.OpalaAdoptionRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

/**
 * Responsabilidade: comprovar a posição da cadeia consultada, com ou sem ciclo, preservando a
 * preparação Opala histórica.
 */
class OpalaAdoptionChainPositionResolverTest {
  /** Expõe 5.2 somente quando a adesão e a cadeia histórica são comprovadamente as mesmas. */
  @Test
  void resolvesCommercialPreparationAtProcessFivePointTwo() {
    var adoptions = Mockito.mock(OpalaAdoptionRepository.class);
    var chains = Mockito.mock(BusinessProcessChainDefinitionRepository.class);
    var resolver = new OpalaAdoptionChainPositionResolver(adoptions, chains);
    when(adoptions.find(2L))
        .thenReturn(
            Optional.of(
                new OpalaAdoption(
                    2,
                    77,
                    4,
                    92,
                    "musa-pde-entry-v12-primeiro-ajuste-aplicavel",
                    14,
                    "Paulo",
                    "Preparação comercial da v12",
                    Instant.parse("2026-09-16T00:00:00Z"))));
    when(adoptions.permits(2L, 4L, 14L, 77L, "experiment:92")).thenReturn(true);
    var parent = new BusinessProcessDefinition();
    parent.setProcessCode("pde-commercial-homologation-activation");
    parent.setName("Homologação e ativação comercial do PDE");
    var item = new BusinessProcessChainItem();
    item.setSequenceNumber(5);
    item.setProcessDefinition(parent);
    var chain = new BusinessProcessChainDefinition();
    chain.setItems(List.of(item));
    when(chains.findById(14L)).thenReturn(Optional.of(chain));

    assertThat(resolver.resolve(4L, 77L, "opala-commercial-preparation-v1", 2L, 14L))
        .hasValueSatisfying(
            position -> {
              assertThat(position.sequenceLabel()).isEqualTo("5.2");
              assertThat(position.parentProcessCode())
                  .isEqualTo("pde-commercial-homologation-activation");
            });
  }

  /** Recusa mostrar posição quando a definição consultada não é a que foi adotada no ciclo. */
  @Test
  void doesNotInferPositionForAnotherDefinition() {
    var adoptions = Mockito.mock(OpalaAdoptionRepository.class);
    var resolver =
        new OpalaAdoptionChainPositionResolver(
            adoptions, Mockito.mock(BusinessProcessChainDefinitionRepository.class));
    when(adoptions.find(2L))
        .thenReturn(
            Optional.of(
                new OpalaAdoption(
                    2,
                    77,
                    4,
                    92,
                    "v12",
                    14,
                    "Paulo",
                    "Preparação comercial",
                    Instant.parse("2026-09-16T00:00:00Z"))));

    assertThat(resolver.resolve(4L, 78L, "opala-commercial-preparation-v1", 2L, 14L)).isEmpty();
  }

  /** Expõe 5.1 pela rota tipada do Processo 5, inclusive sem ciclo comercial associado. */
  @ParameterizedTest
  @NullSource
  @ValueSource(longs = {3})
  void resolvesProcessFivePointOneFromTypedRoute(Long cycleId) {
    var adoptions = Mockito.mock(OpalaAdoptionRepository.class);
    var chains = Mockito.mock(BusinessProcessChainDefinitionRepository.class);
    var processes = Mockito.mock(BusinessProcessDefinitionRepository.class);
    var resolver =
        new OpalaAdoptionChainPositionResolver(
            adoptions, chains, processes, new com.fasterxml.jackson.databind.ObjectMapper());
    var child = new BusinessProcessDefinition();
    child.setId(77L);
    child.setProcessCode("opala-commercial-preparation-v1");
    child.setVersionNumber(1);
    when(processes.findById(77L)).thenReturn(Optional.of(child));
    var parent = new BusinessProcessDefinition();
    parent.setId(56L);
    parent.setProcessCode("pde-commercial-homologation-activation");
    parent.setName("Homologação e ativação comercial");
    parent.setDiagramJson(
        "{\"nodes\":[{\"id\":\"start\",\"type\":\"START\"},{\"id\":\"commercialPreparation\",\"type\":\"TASK\",\"subprocessRoutes\":[{\"productTypeCode\":\"PDE\",\"subprocessCode\":\"opala-commercial-preparation-v1\",\"subprocessVersion\":1}]},{\"id\":\"review\",\"type\":\"TASK\"}],\"flows\":[]}");
    var item = new BusinessProcessChainItem();
    item.setSequenceNumber(5);
    item.setProcessDefinition(parent);
    var chain = new BusinessProcessChainDefinition();
    chain.setItems(List.of(item));
    when(chains.findById(16L)).thenReturn(Optional.of(chain));

    assertThat(resolver.resolve(4L, 77L, "opala-commercial-preparation-v1", cycleId, 16L))
        .hasValueSatisfying(
            position -> {
              assertThat(position.sequenceLabel()).isEqualTo("5.1");
              assertThat(position.parentProcessCode())
                  .isEqualTo("pde-commercial-homologation-activation");
            });
  }

  /** Numera a homologação técnica como 5.4 pela atividade real, sem exigir ciclo comercial. */
  @ParameterizedTest
  @NullSource
  @ValueSource(longs = {3})
  void resolvesTechnicalHomologationAtProcessFivePointFour(Long cycleId) {
    var adoptions = Mockito.mock(OpalaAdoptionRepository.class);
    var chains = Mockito.mock(BusinessProcessChainDefinitionRepository.class);
    var processes = Mockito.mock(BusinessProcessDefinitionRepository.class);
    var resolver =
        new OpalaAdoptionChainPositionResolver(
            adoptions, chains, processes, new com.fasterxml.jackson.databind.ObjectMapper());
    var child = new BusinessProcessDefinition();
    child.setId(88L);
    child.setProcessCode("experiment-homologation-activation");
    child.setVersionNumber(5);
    when(processes.findById(88L)).thenReturn(Optional.of(child));
    var parent = new BusinessProcessDefinition();
    parent.setId(56L);
    parent.setProcessCode("pde-commercial-homologation-activation");
    parent.setName("Homologação e ativação comercial");
    parent.setDiagramJson(
        "{\"nodes\":[{\"id\":\"start\",\"type\":\"START\"},{\"id\":\"commercialPreparation\",\"type\":\"TASK\"},{\"id\":\"humanExperienceReview\",\"type\":\"TASK\"},{\"id\":\"commercialIntegrityReview\",\"type\":\"TASK\"},{\"id\":\"preflight\",\"type\":\"TASK\",\"subprocessCode\":\"experiment-homologation-activation\"}],\"flows\":[]}");
    var item = new BusinessProcessChainItem();
    item.setSequenceNumber(5);
    item.setProcessDefinition(parent);
    var chain = new BusinessProcessChainDefinition();
    chain.setItems(List.of(item));
    when(chains.findById(16L)).thenReturn(Optional.of(chain));

    assertThat(resolver.resolve(4L, 88L, "experiment-homologation-activation", cycleId, 16L))
        .hasValueSatisfying(position -> assertThat(position.sequenceLabel()).isEqualTo("5.4"));
  }

  /** Preserva 5.4 quando os nós estão embaralhados e há retorno de retrabalho no grafo. */
  @Test
  void resolvesTechnicalHomologationByCausalOrder() {
    var adoptions = Mockito.mock(OpalaAdoptionRepository.class);
    var chains = Mockito.mock(BusinessProcessChainDefinitionRepository.class);
    var processes = Mockito.mock(BusinessProcessDefinitionRepository.class);
    var resolver =
        new OpalaAdoptionChainPositionResolver(
            adoptions, chains, processes, new com.fasterxml.jackson.databind.ObjectMapper());
    var child = new BusinessProcessDefinition();
    child.setId(58L);
    child.setProcessCode("experiment-homologation-activation");
    child.setVersionNumber(5);
    when(processes.findById(58L)).thenReturn(Optional.of(child));
    var parent = new BusinessProcessDefinition();
    parent.setId(96L);
    parent.setProcessCode("pde-commercial-homologation-activation");
    parent.setName("Homologação e ativação comercial");
    parent.setDiagramJson(
        """
        {"nodes":[
          {"id":"preflight","type":"TASK","subprocessCode":"experiment-homologation-activation"},
          {"id":"integrity","type":"TASK"},
          {"id":"preparation","type":"TASK"},
          {"id":"experience","type":"TASK"},
          {"id":"start","type":"START"}
        ],"flows":[
          {"from":"start","to":"preparation"},
          {"from":"preparation","to":"experience"},
          {"from":"experience","to":"integrity"},
          {"from":"integrity","to":"preflight"},
          {"from":"preflight","to":"preparation","kind":"REWORK"}
        ]}
        """);
    var item = new BusinessProcessChainItem();
    item.setSequenceNumber(5);
    item.setProcessDefinition(parent);
    var chain = new BusinessProcessChainDefinition();
    chain.setItems(List.of(item));
    when(chains.findById(24L)).thenReturn(Optional.of(chain));

    assertThat(resolver.resolve(10L, 58L, "experiment-homologation-activation", null, 24L))
        .hasValueSatisfying(position -> assertThat(position.sequenceLabel()).isEqualTo("5.4"));
    Mockito.verifyNoInteractions(adoptions);
  }

  /** Não inventa numeração quando a consulta não identifica sua cadeia. */
  @Test
  void doesNotInferPositionWithoutChain() {
    var adoptions = Mockito.mock(OpalaAdoptionRepository.class);
    var chains = Mockito.mock(BusinessProcessChainDefinitionRepository.class);
    var resolver = new OpalaAdoptionChainPositionResolver(adoptions, chains);

    assertThat(resolver.resolve(10L, 58L, "experiment-homologation-activation", null, null))
        .isEmpty();
    Mockito.verifyNoInteractions(adoptions, chains);
  }
}

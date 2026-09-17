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
import org.mockito.Mockito;

/**
 * Responsabilidade: impedir que a posição global do produto oculte a posição comercial da
 * preparação Opala adotada no ciclo.
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

  /** Expõe 5.1 quando a cadeia nova chama a versão Opala pela rota tipada do Processo 5. */
  @Test
  void resolvesProcessFivePointOneFromTypedRoute() {
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

    assertThat(resolver.resolve(4L, 77L, "opala-commercial-preparation-v1", 3L, 16L))
        .hasValueSatisfying(
            position -> {
              assertThat(position.sequenceLabel()).isEqualTo("5.1");
              assertThat(position.parentProcessCode())
                  .isEqualTo("pde-commercial-homologation-activation");
            });
  }

  /** Numera a homologação técnica como 5.4 pela atividade real, não como segundo subprocesso. */
  @Test
  void resolvesTechnicalHomologationAtProcessFivePointFour() {
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

    assertThat(resolver.resolve(4L, 88L, "experiment-homologation-activation", 3L, 16L))
        .hasValueSatisfying(position -> assertThat(position.sequenceLabel()).isEqualTo("5.4"));
  }
}

package com.marketinghub.businessprocess;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: comprovar validação, versionamento e publicação dos processos de negócio. */
class BusinessProcessDefinitionServiceTest {
  private final ObjectMapper mapper = new ObjectMapper();

  /** Cadastra um grafo íntegro como rascunho auditável. */
  @Test
  void createsValidatedDraft() throws Exception {
    BusinessProcessDefinitionRepository repository =
        mock(BusinessProcessDefinitionRepository.class);
    when(repository.findByProcessCodeAndVersionNumber("landing", 2)).thenReturn(Optional.empty());
    when(repository.save(any()))
        .thenAnswer(
            invocation -> {
              BusinessProcessDefinition value = invocation.getArgument(0);
              value.setId(10L);
              return value;
            });
    Instant now = Instant.parse("2026-08-14T20:00:00Z");
    var service =
        new BusinessProcessDefinitionService(repository, mapper, Clock.fixed(now, ZoneOffset.UTC));

    var result = service.create(request(validDiagram()));

    assertThat(result.status()).isEqualTo("DRAFT");
    assertThat(result.createdAt()).isEqualTo(now);
    assertThat(result.diagram().path("nodes")).hasSize(3);
  }

  /** Rejeita fluxo que aponta para elemento inexistente. */
  @Test
  void rejectsBrokenFlow() throws Exception {
    var repository = mock(BusinessProcessDefinitionRepository.class);
    when(repository.findByProcessCodeAndVersionNumber("landing", 2)).thenReturn(Optional.empty());
    var service = new BusinessProcessDefinitionService(repository, mapper);
    var diagram =
        mapper.readTree(
            "{\"nodes\":[{\"id\":\"start\",\"type\":\"START\",\"label\":\"Início\"},{\"id\":\"task\",\"type\":\"TASK\",\"label\":\"Fazer\"},{\"id\":\"end\",\"type\":\"END\",\"label\":\"Fim\"}],\"flows\":[{\"from\":\"start\",\"to\":\"unknown\"}]}");

    assertThatThrownBy(() -> service.create(request(diagram)))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Todo fluxo");
  }

  /** Publica a candidata e aposenta a versão anteriormente vigente. */
  @Test
  void publishesAndRetiresPreviousVersion() throws Exception {
    var repository = mock(BusinessProcessDefinitionRepository.class);
    BusinessProcessDefinition previous = entity(1L, 1, "PUBLISHED", validDiagram().toString());
    BusinessProcessDefinition candidate = entity(2L, 2, "DRAFT", validDiagram().toString());
    when(repository.findById(2L)).thenReturn(Optional.of(candidate));
    when(repository.findAllByProcessCodeOrderByVersionNumberDesc("landing"))
        .thenReturn(List.of(candidate, previous));
    when(repository.save(candidate)).thenReturn(candidate);
    var service = new BusinessProcessDefinitionService(repository, mapper);

    var result = service.publish(2L);

    assertThat(result.status()).isEqualTo("PUBLISHED");
    assertThat(previous.getStatus()).isEqualTo("RETIRED");
  }

  /** Monta uma requisição mínima para concentrar os cenários no comportamento governado. */
  private BusinessProcessDefinitionRequest request(
      com.fasterxml.jackson.databind.JsonNode diagram) {
    return new BusinessProcessDefinitionRequest(
        "landing", "Landing", "Vender", "Operação", "Briefing", "Aprovada", 2, "pipeline", diagram);
  }

  /** Fornece o menor grafo BPM válido. */
  private com.fasterxml.jackson.databind.JsonNode validDiagram() throws Exception {
    return mapper.readTree(
        "{\"nodes\":[{\"id\":\"start\",\"type\":\"START\",\"label\":\"Início\"},{\"id\":\"task\",\"type\":\"TASK\",\"label\":\"Fazer\"},{\"id\":\"end\",\"type\":\"END\",\"label\":\"Fim\"}],\"flows\":[{\"from\":\"start\",\"to\":\"task\"},{\"from\":\"task\",\"to\":\"end\"}]}");
  }

  /** Monta uma versão persistida para testar troca de vigência. */
  private BusinessProcessDefinition entity(Long id, int version, String status, String diagram) {
    BusinessProcessDefinition value = new BusinessProcessDefinition();
    value.setId(id);
    value.setProcessCode("landing");
    value.setName("Landing");
    value.setPurpose("Vender");
    value.setOwnerName("Operação");
    value.setTriggerDescription("Briefing");
    value.setOutcomeDescription("Aprovada");
    value.setVersionNumber(version);
    value.setStatus(status);
    value.setDiagramJson(diagram);
    value.setCreatedAt(Instant.now());
    return value;
  }
}

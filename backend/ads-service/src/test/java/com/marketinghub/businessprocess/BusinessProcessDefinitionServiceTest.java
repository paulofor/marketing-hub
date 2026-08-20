package com.marketinghub.businessprocess;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocessresource.BusinessProcessExecutionResource;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.businessprocessresource.BusinessProcessExecutionResourceRepository;
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
  private final AgentTaskRepository tasks = mock(AgentTaskRepository.class);

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
        new BusinessProcessDefinitionService(
            repository, tasks, mapper, Clock.fixed(now, ZoneOffset.UTC));

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
    var service = new BusinessProcessDefinitionService(repository, tasks, mapper);
    var diagram =
        mapper.readTree(
            "{\"nodes\":[{\"id\":\"start\",\"type\":\"START\",\"label\":\"Início\"},{\"id\":\"task\",\"type\":\"TASK\",\"label\":\"Fazer\"},{\"id\":\"end\",\"type\":\"END\",\"label\":\"Fim\"}],\"flows\":[{\"from\":\"start\",\"to\":\"unknown\"}]}");

    assertThatThrownBy(() -> service.create(request(diagram)))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Todo fluxo");
  }

  /** Persiste uma atividade com recurso quando o código está ativo no catálogo oficial. */
  @Test
  void acceptsActiveExecutionResourceOnTask() throws Exception {
    var repository = mock(BusinessProcessDefinitionRepository.class);
    var resources = mock(BusinessProcessExecutionResourceRepository.class);
    when(repository.findByProcessCodeAndVersionNumber("landing", 2)).thenReturn(Optional.empty());
    when(resources.findByResourceCodeAndActiveTrue("themis-image-studio"))
        .thenReturn(Optional.of(studio()));
    when(repository.save(any()))
        .thenAnswer(
            invocation -> {
              BusinessProcessDefinition value = invocation.getArgument(0);
              value.setId(10L);
              return value;
            });
    var service =
        new BusinessProcessDefinitionService(
            repository, tasks, resources, mapper, Clock.systemUTC());

    var result = service.create(request(diagramWithResource("TASK", "themis-image-studio")));

    assertThat(result.diagram().path("nodes").get(1).path("executionResourceCode").asText())
        .isEqualTo("themis-image-studio");
  }

  /** Rejeita recurso inexistente antes de persistir ou publicar a versão. */
  @Test
  void rejectsUnavailableExecutionResource() throws Exception {
    var repository = mock(BusinessProcessDefinitionRepository.class);
    var resources = mock(BusinessProcessExecutionResourceRepository.class);
    when(repository.findByProcessCodeAndVersionNumber("landing", 2)).thenReturn(Optional.empty());
    when(resources.findByResourceCodeAndActiveTrue("missing-studio")).thenReturn(Optional.empty());
    var service =
        new BusinessProcessDefinitionService(
            repository, tasks, resources, mapper, Clock.systemUTC());

    assertThatThrownBy(() -> service.create(request(diagramWithResource("TASK", "missing-studio"))))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("não está disponível");
    verify(repository, never()).save(any());
  }

  /** Rejeita recurso em evento ou gate porque somente atividades possuem executor. */
  @Test
  void rejectsExecutionResourceOutsideTask() throws Exception {
    var repository = mock(BusinessProcessDefinitionRepository.class);
    when(repository.findByProcessCodeAndVersionNumber("landing", 2)).thenReturn(Optional.empty());
    var service =
        new BusinessProcessDefinitionService(
            repository,
            tasks,
            mock(BusinessProcessExecutionResourceRepository.class),
            mapper,
            Clock.systemUTC());

    assertThatThrownBy(
            () -> service.create(request(diagramWithResource("GATEWAY", "themis-image-studio"))))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Somente atividades");
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
    var service = new BusinessProcessDefinitionService(repository, tasks, mapper);

    var result = service.publish(2L);

    assertThat(result.status()).isEqualTo("PUBLISHED");
    assertThat(previous.getStatus()).isEqualTo("RETIRED");
  }

  /** Permite alterar conteúdo e grafo do rascunho preservando sua identidade versionada. */
  @Test
  void updatesDraft() throws Exception {
    var repository = mock(BusinessProcessDefinitionRepository.class);
    BusinessProcessDefinition draft = entity(2L, 2, "DRAFT", validDiagram().toString());
    when(repository.findById(2L)).thenReturn(Optional.of(draft));
    when(repository.save(draft)).thenReturn(draft);
    var service = new BusinessProcessDefinitionService(repository, tasks, mapper);

    var result = service.updateDraft(2L, request(validDiagram()));

    assertThat(result.purpose()).isEqualTo("Vender");
    assertThat(result.status()).isEqualTo("DRAFT");
  }

  /** Impede edição silenciosa da fonte de verdade já publicada. */
  @Test
  void rejectsPublishedEdition() throws Exception {
    var repository = mock(BusinessProcessDefinitionRepository.class);
    BusinessProcessDefinition published = entity(1L, 2, "PUBLISHED", validDiagram().toString());
    when(repository.findById(1L)).thenReturn(Optional.of(published));
    var service = new BusinessProcessDefinitionService(repository, tasks, mapper);

    assertThatThrownBy(() -> service.updateDraft(1L, request(validDiagram())))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Somente versões em rascunho");
  }

  /** Rejeita processo equivalente mesmo quando outro código tenta contornar a identidade. */
  @Test
  void rejectsEquivalentNameWithDifferentCode() throws Exception {
    var repository = mock(BusinessProcessDefinitionRepository.class);
    BusinessProcessDefinition existing = entity(5L, 1, "DRAFT", validDiagram().toString());
    existing.setName("Operação e otimização de experimento");
    existing.setProcessCode("experiment-optimization");
    when(repository.findByProcessCodeAndVersionNumber("other-code", 2))
        .thenReturn(Optional.empty());
    when(repository.findAllByOrderByNameAscVersionNumberDesc()).thenReturn(List.of(existing));
    var service = new BusinessProcessDefinitionService(repository, tasks, mapper);
    BusinessProcessDefinitionRequest duplicate =
        new BusinessProcessDefinitionRequest(
            "other-code",
            "OPERACAO  E OTIMIZAÇÃO DE EXPERIMENTO",
            "Vender",
            "Operação",
            "Início",
            "Fim",
            2,
            null,
            validDiagram());

    assertThatThrownBy(() -> service.create(duplicate))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("processo equivalente");
    verify(repository, never()).save(any());
  }

  /** Exclui rascunho sem tarefa e preserva qualquer definição utilizada. */
  @Test
  void deletesOnlyUnusedDraft() throws Exception {
    var repository = mock(BusinessProcessDefinitionRepository.class);
    BusinessProcessDefinition draft = entity(7L, 1, "DRAFT", validDiagram().toString());
    when(repository.findById(7L)).thenReturn(Optional.of(draft));
    when(tasks.existsByProcessDefinitionId(7L)).thenReturn(false);
    var service = new BusinessProcessDefinitionService(repository, tasks, mapper);

    service.deleteDraft(7L);

    verify(repository).delete(draft);
  }

  /** Bloqueia a exclusão quando o rascunho já possui tarefa operacional vinculada. */
  @Test
  void rejectsDeletionOfUsedDraft() throws Exception {
    var repository = mock(BusinessProcessDefinitionRepository.class);
    BusinessProcessDefinition draft = entity(7L, 1, "DRAFT", validDiagram().toString());
    when(repository.findById(7L)).thenReturn(Optional.of(draft));
    when(tasks.existsByProcessDefinitionId(7L)).thenReturn(true);
    var service = new BusinessProcessDefinitionService(repository, tasks, mapper);

    assertThatThrownBy(() -> service.deleteDraft(7L))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("tarefas vinculadas");
    verify(repository, never()).delete(any());
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

  /** Monta um grafo cujo elemento central declara o recurso sob validação. */
  private com.fasterxml.jackson.databind.JsonNode diagramWithResource(
      String type, String resourceCode) throws Exception {
    return mapper.readTree(
        "{\"nodes\":[{\"id\":\"start\",\"type\":\"START\",\"label\":\"Início\"},"
            + "{\"id\":\"task\",\"type\":\""
            + type
            + "\",\"label\":\"Fazer\",\"executionResourceCode\":\""
            + resourceCode
            + "\"},{\"id\":\"end\",\"type\":\"END\",\"label\":\"Fim\"}],"
            + "\"flows\":[{\"from\":\"start\",\"to\":\"task\"},{\"from\":\"task\",\"to\":\"end\"}]}");
  }

  /** Monta o recurso ativo usado nas atividades visuais de Têmis. */
  private BusinessProcessExecutionResource studio() {
    BusinessProcessExecutionResource resource = new BusinessProcessExecutionResource();
    resource.setResourceCode("themis-image-studio");
    resource.setName("Estúdio de Imagens de Têmis");
    resource.setResponsibleAgentKey("meta-ad-approver");
    resource.setActive(true);
    return resource;
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

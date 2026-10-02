package com.marketinghub.businessprocess;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocessresource.BusinessProcessExecutionResource;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.businessprocessresource.BusinessProcessExecutionResourceRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar os contratos que serão publicados pela interface da cadeia. */
class CycleChangeDefinitionsTest {
  /** Confere todas as candidatas com o mesmo validador usado pelo cadastro e pela publicação. */
  @Test
  void candidateDefinitionsRespectBackendContractsAndCarryIsolationObjectives() throws Exception {
    var mapper = new ObjectMapper();
    Path root = Path.of("").toAbsolutePath();
    while (!Files.exists(root.resolve("infra/testing/cycle-change-policy/candidates.json"))) {
      root = root.getParent();
      if (root == null) throw new IllegalStateException("Contrato versionado não encontrado.");
    }
    var candidates =
        mapper.readTree(root.resolve("infra/testing/cycle-change-policy/candidates.json").toFile());
    var repository = mock(BusinessProcessDefinitionRepository.class);
    var resources = mock(BusinessProcessExecutionResourceRepository.class);
    var definitions = new HashMap<String, BusinessProcessDefinition>();
    for (var candidate : candidates) {
      var definition = new BusinessProcessDefinition();
      definition.setProcessCode(candidate.path("processCode").asText());
      definition.setProcessType(candidate.path("processType").asText());
      definition.setParentProcessCode(candidate.path("parentProcessCode").asText(null));
      definition.setStatus("PUBLISHED");
      definitions.put(definition.getProcessCode(), definition);
      for (var node : candidate.path("diagram").path("nodes")) {
        if (node.hasNonNull("executionResourceCode")) {
          var resource = new BusinessProcessExecutionResource();
          resource.setResponsibleAgentKey(node.path("responsibleAgentKeys").path(0).asText());
          when(resources.findByResourceCodeAndActiveTrue(
                  node.path("executionResourceCode").asText()))
              .thenReturn(Optional.of(resource));
        }
        if ("TASK".equals(node.path("type").asText())) {
          assertThat(node.path("description").asText())
              .contains(
                  "CHANGE_PER_CYCLE_V1",
                  "novo ciclo e novo experimento",
                  "custos",
                  "resultados anteriores");
        }
      }
    }
    when(repository.findFirstByProcessCodeAndStatusOrderByVersionNumberDesc(
            anyString(), eq("PUBLISHED")))
        .thenAnswer(call -> Optional.ofNullable(definitions.get(call.getArgument(0))));
    when(repository.save(any()))
        .thenAnswer(
            call -> {
              BusinessProcessDefinition value = call.getArgument(0);
              value.setId(99001L);
              return value;
            });
    var service =
        new BusinessProcessDefinitionService(
            repository,
            mock(AgentTaskRepository.class),
            resources,
            mapper,
            java.time.Clock.systemUTC());
    assertThat(candidates).hasSize(13);
    for (var candidate : candidates) {
      var request = mapper.treeToValue(candidate, BusinessProcessDefinitionRequest.class);
      assertDoesNotThrow(() -> service.create(request), request.processCode());
      assertDoesNotThrow(
          () -> new BusinessProcessGraphTopology(candidate.path("diagram")), request.processCode());
    }
    var cycle =
        java.util.stream.StreamSupport.stream(candidates.spliterator(), false)
            .filter(c -> c.path("processCode").asText().equals("value-chain-learning-sales-cycle"))
            .findFirst()
            .orElseThrow();
    assertThat(cycle.path("diagram").path("experimentChangePolicy").asText())
        .isEqualTo("CHANGE_PER_CYCLE_V1");
    assertThat(cycle.path("diagram").toString())
        .doesNotContain(
            "SCALE_AUTHORIZATION", "corrigir a mesma iteração", "revalidar o mesmo experimento");
  }
}

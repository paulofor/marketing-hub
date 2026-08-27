package com.marketinghub.businessprocess;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.repository.jpa.businessprocess.BusinessProcessActivityDefinitionRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Responsabilidade: comprovar a substituição ordenada das atividades versionadas de um processo.
 */
@DataJpaTest
@TestPropertySource(properties = "spring.liquibase.enabled=false")
class BusinessProcessActivityDefinitionRepositoryTest {
  @Autowired private BusinessProcessDefinitionRepository processRepository;
  @Autowired private BusinessProcessActivityDefinitionRepository activityRepository;

  /** Remove a atividade anterior antes de inserir outra com a mesma identidade funcional. */
  @Test
  void replacesActivityWithoutViolatingUniqueConstraint() {
    BusinessProcessDefinition process = processRepository.saveAndFlush(process());
    activityRepository.saveAndFlush(activity(process, "Descrição anterior"));

    activityRepository.deleteByProcessDefinitionId(process.getId());
    activityRepository.flush();
    activityRepository.saveAndFlush(activity(process, "Descrição atualizada"));

    assertThat(activityRepository.findAllByProcessDefinitionIdOrderByIdAsc(process.getId()))
        .singleElement()
        .extracting(BusinessProcessActivityDefinition::getObjective)
        .isEqualTo("Descrição atualizada");
  }

  /** Monta a definição mínima exigida pela persistência do catálogo. */
  private BusinessProcessDefinition process() {
    BusinessProcessDefinition process = new BusinessProcessDefinition();
    process.setProcessCode("discovery");
    process.setName("Descoberta");
    process.setPurpose("Descobrir oportunidade.");
    process.setOwnerName("Inteligência de Mercado");
    process.setTriggerDescription("Sinal recebido.");
    process.setOutcomeDescription("Oportunidade decidida.");
    process.setVersionNumber(2);
    process.setStatus("DRAFT");
    process.setProcessType("VALUE_PROCESS");
    process.setDiagramJson("{\"nodes\":[],\"flows\":[]}");
    process.setCreatedAt(Instant.parse("2026-08-26T06:00:00Z"));
    return process;
  }

  /** Monta uma atividade com identidade repetível para validar a ordem física das operações. */
  private BusinessProcessActivityDefinition activity(
      BusinessProcessDefinition process, String objective) {
    BusinessProcessActivityDefinition activity = new BusinessProcessActivityDefinition();
    activity.setProcessDefinition(process);
    activity.setActivityId("inspiration");
    activity.setName("Curar inspirações atualizadas");
    activity.setObjective(objective);
    activity.setOwnerName("Argos e Dédalo");
    activity.setDefinitionJson("{\"id\":\"inspiration\",\"type\":\"TASK\"}");
    activity.setCreatedAt(Instant.parse("2026-08-26T06:00:00Z"));
    return activity;
  }
}

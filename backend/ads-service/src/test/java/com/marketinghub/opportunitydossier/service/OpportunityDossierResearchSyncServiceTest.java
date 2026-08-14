package com.marketinghub.opportunitydossier.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.opportunitydossier.OpportunityDossier;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryOpportunity;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.opportunitydossier.OpportunityDossierRepository;
import com.marketinghub.repository.jpa.opportunitydossier.OpportunityEvidenceRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger a correlação entre pesquisa de Argos, dossiê e mesa. */
class OpportunityDossierResearchSyncServiceTest {
  /** Conclui somente a tarefa com referência exata e preserva a evidência real. */
  @Test
  void synchronizesEvidenceAndExactTask() {
    OpportunityDossierRepository dossiers = mock(OpportunityDossierRepository.class);
    OpportunityEvidenceRepository evidence = mock(OpportunityEvidenceRepository.class);
    AgentTaskRepository tasks = mock(AgentTaskRepository.class);
    OpportunityDossier dossier = OpportunityDossier.builder().id(1L).build();
    ProductDiscoveryOpportunity opportunity = new ProductDiscoveryOpportunity();
    opportunity.setName("Produto melhorado por IA");
    opportunity.setEvidenceJson(
        "{\"publicEvidence\":[{\"url\":\"https://example.test/evidencia\",\"snippet\":\"Preço e avaliações verificáveis\"}]}");
    AgentTask task = new AgentTask();
    task.setStatus("IN_PROGRESS");
    when(dossiers.findByProductDiscoveryCycleId(42L)).thenReturn(Optional.of(dossier));
    when(tasks.findTopBySourceReferenceOrderByUpdatedAtDescIdDesc("opportunity-dossier:1"))
        .thenReturn(Optional.of(task));

    new OpportunityDossierResearchSyncService(dossiers, evidence, tasks)
        .synchronize(42L, List.of(opportunity));

    assertThat(task.getStatus()).isEqualTo("COMPLETED");
    verify(evidence).save(org.mockito.ArgumentMatchers.any());
    verify(tasks).save(task);
  }
}

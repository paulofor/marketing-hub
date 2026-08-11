package com.marketinghub.opportunitydossier.service;

import com.marketinghub.opportunitydossier.OpportunityEvidence;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryOpportunity;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.opportunitydossier.OpportunityDossierRepository;
import com.marketinghub.repository.jpa.opportunitydossier.OpportunityEvidenceRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/** Responsabilidade: incorporar no dossiê as evidências produzidas pelo executor de Argos. */
@Service
public class OpportunityDossierResearchSyncService {
  private final OpportunityDossierRepository dossierRepository;
  private final OpportunityEvidenceRepository evidenceRepository;
  private final AgentTaskRepository taskRepository;

  /** Configura as fontes persistidas usadas na sincronização auditável. */
  public OpportunityDossierResearchSyncService(
      OpportunityDossierRepository dossierRepository,
      OpportunityEvidenceRepository evidenceRepository,
      AgentTaskRepository taskRepository) {
    this.dossierRepository = dossierRepository;
    this.evidenceRepository = evidenceRepository;
    this.taskRepository = taskRepository;
  }

  /** Anexa resultados reais ao dossiê sem fabricar evidência quando a busca vier vazia. */
  public void synchronize(Long cycleId, List<ProductDiscoveryOpportunity> opportunities) {
    dossierRepository
        .findByProductDiscoveryCycleId(cycleId)
        .ifPresent(
            dossier -> {
              for (ProductDiscoveryOpportunity opportunity : opportunities) {
                if (opportunity.getEvidenceJson() == null
                    || opportunity.getEvidenceJson().isBlank()) {
                  continue;
                }
                evidenceRepository.save(
                    OpportunityEvidence.builder()
                        .dossier(dossier)
                        .sourceUrl("product-discovery-cycle:" + cycleId)
                        .summary(opportunity.getName() + " — " + opportunity.getEvidenceJson())
                        .createdBy("market-radar")
                        .build());
              }
              finishTask(dossier.getId(), "COMPLETED");
            });
  }

  /** Marca a tarefa como ativa somente quando o executor efetivamente reserva o ciclo. */
  public void start(Long cycleId) {
    dossierRepository
        .findByProductDiscoveryCycleId(cycleId)
        .ifPresent(dossier -> finishTask(dossier.getId(), "IN_PROGRESS"));
  }

  /** Expõe a falha da pesquisa na mesa de Argos para impedir trabalho fantasma no monitor. */
  public void fail(Long cycleId) {
    dossierRepository
        .findByProductDiscoveryCycleId(cycleId)
        .ifPresent(dossier -> finishTask(dossier.getId(), "BLOCKED"));
  }

  /** Atualiza a tarefa vinculada preservando o instante da transição operacional. */
  private void finishTask(Long dossierId, String status) {
    taskRepository
        .findTopBySourceReferenceOrderByUpdatedAtDescIdDesc("opportunity-dossier:" + dossierId)
        .ifPresent(
            task -> {
              task.setStatus(status);
              task.setUpdatedAt(java.time.Instant.now());
              taskRepository.save(task);
            });
  }
}

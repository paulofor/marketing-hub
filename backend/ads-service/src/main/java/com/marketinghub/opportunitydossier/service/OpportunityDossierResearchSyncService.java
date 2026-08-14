package com.marketinghub.opportunitydossier.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.opportunitydossier.OpportunityEvidence;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryOpportunity;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.opportunitydossier.OpportunityDossierRepository;
import com.marketinghub.repository.jpa.opportunitydossier.OpportunityEvidenceRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Responsabilidade: incorporar no dossiê as evidências produzidas pelo executor de Argos. */
@Service
public class OpportunityDossierResearchSyncService {
  private static final Logger log =
      LoggerFactory.getLogger(OpportunityDossierResearchSyncService.class);
  private static final ObjectMapper JSON = new ObjectMapper();
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
                persistSources(dossier.getId(), dossier, opportunity, cycleId);
              }
              finishTask(dossier.getId(), "COMPLETED");
            });
  }

  /** Persiste cada fonte real individualmente, com idempotência e proveniência. */
  private void persistSources(
      Long dossierId,
      com.marketinghub.opportunitydossier.OpportunityDossier dossier,
      ProductDiscoveryOpportunity opportunity,
      Long cycleId) {
    try {
      JsonNode root = JSON.readTree(opportunity.getEvidenceJson());
      JsonNode sources = root.path("publicEvidence");
      if (!sources.isArray()) return;
      for (JsonNode source : sources) {
        String url = source.path("url").asText("").trim();
        if (url.isBlank() || evidenceRepository.existsByDossierIdAndSourceUrl(dossierId, url)) {
          continue;
        }
        String summary = source.path("snippet").asText(source.path("title").asText("")).trim();
        if (summary.isBlank()) continue;
        evidenceRepository.save(
            OpportunityEvidence.builder()
                .dossier(dossier)
                .sourceUrl(url)
                .summary(opportunity.getName() + " — " + summary)
                .createdBy("ARGOS:product-discovery-cycle:" + cycleId)
                .build());
      }
    } catch (Exception ex) {
      log.error(
          "Falha ao persistir evidências de Argos cycleId={} dossierId={} opportunity={}",
          cycleId,
          dossierId,
          opportunity.getName(),
          ex);
    }
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

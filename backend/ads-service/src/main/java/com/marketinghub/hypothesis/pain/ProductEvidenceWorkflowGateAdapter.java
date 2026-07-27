package com.marketinghub.hypothesis.pain;

import com.marketinghub.hypothesis.pain.service.HypothesisProductEvidenceGate;
import com.marketinghub.mds.productevidence.v1.service.ProductEvidenceWorkflowService;
import org.springframework.stereotype.Component;

/** Responsabilidade: adaptar o gate científico de hipótese ao workflow de evidências do MDS. */
@Component
public class ProductEvidenceWorkflowGateAdapter implements HypothesisProductEvidenceGate {
  private final ProductEvidenceWorkflowService workflowService;

  /** Inicializa o adaptador com o workflow canônico de evidências de produto. */
  public ProductEvidenceWorkflowGateAdapter(ProductEvidenceWorkflowService workflowService) {
    this.workflowService = workflowService;
  }

  /** Garante que existe uma pesquisa científica iniciada para o nicho informado. */
  @Override
  public void ensureProductEvidenceStarted(Long marketNicheId) {
    workflowService.ensureProductEvidenceStarted(marketNicheId);
  }

  /** Bloqueia o avanço comercial quando o pacote científico ainda não foi aprovado. */
  @Override
  public void requireApprovedEvidencePack(Long marketNicheId) {
    workflowService.requireApprovedEvidencePack(marketNicheId);
  }

  /** Informa se o nicho já possui pacote científico final concluído. */
  @Override
  public boolean hasApprovedEvidencePack(Long marketNicheId) {
    return workflowService.hasApprovedEvidencePack(marketNicheId);
  }
}

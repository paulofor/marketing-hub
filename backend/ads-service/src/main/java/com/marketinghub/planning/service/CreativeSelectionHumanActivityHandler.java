package com.marketinghub.planning.service;

import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.humanactivity.HumanProductProcessActivityCompletion;
import com.marketinghub.businessprocess.execution.service.humanactivity.HumanProductProcessActivityHandler;
import com.marketinghub.businessprocess.execution.service.humanactivity.HumanProductProcessActivityReadiness;
import com.marketinghub.businessprocess.execution.service.humanactivity.HumanProductProcessActivityRequirement;
import com.marketinghub.businessprocess.execution.service.requestProductProcessActivityExecution.ProductProcessActivityExecutionRequest;
import com.marketinghub.communication.v1.PrivateCreativeSelection;
import com.marketinghub.product.Product;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Responsabilidade: aplicar a seleção humana de criativos no contrato privado ou comercial
 * correspondente.
 */
@Service
public class CreativeSelectionHumanActivityHandler implements HumanProductProcessActivityHandler {
  private final CommercialPlanApprovedProcessAssetService approvedAssets;
  private final PrivateCreativeSelection privateSelection;

  /** Configura os efeitos segregados da seleção privada e da biblioteca comercial. */
  public CreativeSelectionHumanActivityHandler(
      CommercialPlanApprovedProcessAssetService approvedAssets,
      PrivateCreativeSelection privateSelection) {
    this.approvedAssets = approvedAssets;
    this.privateSelection = privateSelection;
  }

  /** Reconhece somente a decisão humana terminal da produção e aprovação de criativos. */
  @Override
  public boolean supports(
      BusinessProcessDefinition process, BusinessProcessActivityDefinition activityDefinition) {
    return process != null
        && activityDefinition != null
        && CommercialPlanApprovedProcessAssetService.PROCESS_CODE.equals(process.getProcessCode())
        && CommercialPlanApprovedProcessAssetService.HUMAN_ACTIVITY_ID.equals(
            activityDefinition.getActivityId());
  }

  /** Explica os pareceres dos mesmos pixels e mantém a confirmação no escopo aprovado. */
  @Override
  @Transactional(readOnly = true)
  public HumanProductProcessActivityReadiness readiness(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activityDefinition,
      Product product,
      String sourceReference) {
    var privateReadiness = privateSelection.readiness(process, product, sourceReference);
    if (privateReadiness.isPresent()) return privateReadiness.get();
    CommercialPlanApprovedProcessAssetService.ImportReadiness importReadiness =
        approvedAssets.readiness(product, sourceReference);
    List<HumanProductProcessActivityRequirement> requirements =
        List.of(
            new HumanProductProcessActivityRequirement(
                "APPROVED_CREATIVE_PIXELS",
                "Peça final revisada pelos dois gates",
                importReadiness.ready(),
                importReadiness.reason(),
                importReadiness.ready()
                    ? "Confirme somente as peças que deseja disponibilizar ao destino."
                    : "Conclua produção, Psique e Têmis sobre o mesmo artefato antes da decisão."));
    return new HumanProductProcessActivityReadiness(
        importReadiness.ready(),
        importReadiness.reason(),
        "Aprovar e vincular peças",
        "A aprovação registra as peças na biblioteca do plano para anúncio e landing. Não publica campanha nem autoriza mídia.",
        "Selecionar peças aprovadas para uso",
        "Confirmo que revisei os pixels finais, Psique, Têmis e o impacto comercial. Esta decisão não publica campanha nem autoriza gasto.",
        "CONFIRM:"
            + CommercialPlanApprovedProcessAssetService.PROCESS_CODE
            + ":"
            + CommercialPlanApprovedProcessAssetService.HUMAN_ACTIVITY_ID,
        null,
        importReadiness.commercialPlanId(),
        requirements);
  }

  /** Confere a seleção privada ou importa a comercial quando o executor usa o contrato padrão. */
  @Override
  @Transactional
  public void approve(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activityDefinition,
      Product product,
      String sourceReference,
      ProductProcessActivityExecutionRequest request) {
    if (privateSelection
        .completeApproval(process, product, sourceReference, request.confirmationToken())
        .isPresent()) return;
    approvedAssets.importForHumanDecision(product, sourceReference);
  }

  /** Acrescenta a prova privada ou os ativos do plano à evidência da decisão humana. */
  @Override
  @Transactional
  public HumanProductProcessActivityCompletion completeApproval(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activityDefinition,
      Product product,
      String sourceReference,
      ProductProcessActivityExecutionRequest request) {
    var privateCompletion =
        privateSelection.completeApproval(
            process, product, sourceReference, request.confirmationToken());
    if (privateCompletion.isPresent()) return privateCompletion.get();
    CommercialPlanApprovedProcessAssetService.ImportResult imported =
        approvedAssets.importForHumanDecision(product, sourceReference);
    Map<String, Object> evidence = new LinkedHashMap<>(request.structuredEvidence());
    evidence.put("commercialPlanId", imported.commercialPlanId());
    evidence.put("creativePackageId", imported.creativePackageId());
    evidence.put(
        "visualAssets",
        imported.assets().stream()
            .map(
                asset ->
                    Map.of(
                        "assetId", asset.id(),
                        "assetUrl", asset.assetUrl(),
                        "sha256", asset.contentSha256()))
            .toList());
    evidence.put("published", false);
    evidence.put("externalMediaSpendAuthorized", false);
    return new HumanProductProcessActivityCompletion(
        true,
        "Peças aprovadas e vinculadas ao plano sem publicação ou gasto de mídia.",
        null,
        evidence);
  }
}

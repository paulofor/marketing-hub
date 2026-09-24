package com.marketinghub.experiment.service;

import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.backendactivity.BackendProductProcessActivityReadiness;
import com.marketinghub.businessprocess.execution.service.productProcessExecutions.ProductProcessActivityRequirementResponse;
import com.marketinghub.product.Product;
import java.util.List;

/**
 * Responsabilidade: orientar o cadastro comercial ausente sem criar execução ou autorizar gasto.
 */
public final class CommercialExperimentPreparationEntry {
  /** Impede instâncias de uma projeção sem estado ou efeitos externos. */
  private CommercialExperimentPreparationEntry() {}

  /** Expõe a fonte a completar e as identidades já conhecidas do produto e do percurso. */
  public static BackendProductProcessActivityReadiness describe(
      Product product, BusinessProcessDefinition target) {
    Long nicheId = product.getMarketNiche() == null ? null : product.getMarketNiche().getId();
    boolean nicheReady = nicheId != null;
    String productTypeCode = product.getProductTypeDefinition().getCode();
    return new BackendProductProcessActivityReadiness(
        false,
        nicheReady
            ? "A preparação exige um experimento comercial explícito; a validação privada não comprova oferta pública, compra ou utilidade humana."
            : "A preparação exige um experimento comercial explícito, mas o produto ainda não possui nicho cadastrado.",
        nicheReady ? "Criar experimento comercial" : "Completar cadastro comercial",
        "Materialize somente as decisões comerciais persistidas; não publique, não ative campanha e não autorize orçamento nesta etapa.",
        "COMMERCIAL_EXPERIMENT",
        product.getId(),
        List.of(
            new ProductProcessActivityRequirementResponse(
                "PRODUCT_TYPE", "Tipo cadastrado", true, productTypeCode, "Preserve o tipo."),
            new ProductProcessActivityRequirementResponse(
                "TYPE_ROUTE",
                "Percurso do tipo",
                true,
                target.getProcessCode() + " v" + target.getVersionNumber(),
                "Execute somente este subprocesso."),
            new ProductProcessActivityRequirementResponse(
                "MARKET_NICHE",
                "Nicho comercial",
                nicheReady,
                nicheReady ? "Nicho #" + nicheId + " vinculado." : "Nicho ausente.",
                nicheReady ? "Preserve o nicho." : "Cadastre o nicho antes do experimento."),
            new ProductProcessActivityRequirementResponse(
                "COMMERCIAL_EXPERIMENT",
                "Experimento comercial",
                false,
                "Nenhum experimento foi selecionado para esta execução.",
                "Crie o experimento com oferta, canal e métricas explícitos; a prova privada permanece apenas como referência.")),
        null,
        nicheReady
            ? "/experiments/new?nicheId=" + nicheId + "&productId=" + product.getId()
            : "/products/" + product.getId() + "/edit");
  }
}

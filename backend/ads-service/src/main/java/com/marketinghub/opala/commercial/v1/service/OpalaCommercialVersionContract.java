package com.marketinghub.opala.commercial.v1.service;

import static com.marketinghub.opala.commercial.v1.service.OpalaCommercialContext.require;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.pde.service.PdeCommercialCheckoutContractResolver;
import com.marketinghub.pde.service.PdeCommercialCheckoutContractResolver.CanonicalCheckout;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Responsabilidade: validar checkout e acesso contra a identidade exata da candidata Opala. */
@Component
@RequiredArgsConstructor
public class OpalaCommercialVersionContract {
  private final PdeCommercialCheckoutContractResolver checkoutResolver;

  /**
   * Reúne somente um contrato de versão íntegro, sem criar preferência, cobrança ou concessão de
   * acesso.
   */
  public Resolved resolve(
      OpalaCommercialContext.Scope scope, OpalaCommercialContext.Candidate candidate) {
    var cycle = scope.cycle();
    var experiment = scope.experiment();
    var product = experiment.getProduct();
    JsonNode contract = candidate.productContract();
    require(contract != null && contract.isObject(), "O contrato candidato do PDE está ausente.");
    String version = contract.path("experienceVersion").asText("").trim();
    require(
        Objects.equals(cycle.getProductVersion(), version),
        "O contrato candidato não pertence à versão vigente do ciclo.");
    JsonNode binding = contract.path("commercialBinding");
    require(
        binding.isObject()
            && binding.path("experimentId").asLong() == experiment.getId()
            && binding.path("priceBrl").isNumber()
            && experiment.getUnitPrice() != null
            && binding.path("priceBrl").decimalValue().compareTo(experiment.getUnitPrice()) == 0
            && "ONE_TIME".equals(binding.path("billingModel").asText()),
        "O vínculo comercial da candidata diverge do experimento, preço ou cobrança.");
    CanonicalCheckout checkout =
        checkoutResolver
            .resolve(product, contract)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "O contrato candidato não possui checkout canônico versionado."));
    require(
        checkout.priceBrl().compareTo(experiment.getUnitPrice()) == 0,
        "O preço do checkout candidato diverge do experimento.");
    require(
        experiment.getCommercialCheckoutUrl() == null
            || experiment.getCommercialCheckoutUrl().isBlank()
            || Objects.equals(experiment.getCommercialCheckoutUrl(), checkout.checkoutUrl()),
        "O experimento aponta para outro checkout; preserve a divergência para revisão.");
    JsonNode access = contract.path("commercialAccess");
    require(
        access.isObject()
            && version.equals(access.path("experienceVersion").asText())
            && access.path("accessDays").isIntegralNumber()
            && access.path("accessDays").asInt() > 0
            && !access.path("renewal").asBoolean(true)
            && "PAYMENT_APPROVED".equals(access.path("activationTrigger").asText())
            && !access.path("scope").asText().isBlank(),
        "O acesso pago precisa declarar versão, prazo, escopo e ativação pela compra aprovada.");
    return new Resolved(contract, checkout, access.path("accessDays").asInt());
  }

  /** Representa o contrato comercial candidato já reconciliado e ainda sem efeitos externos. */
  public record Resolved(JsonNode productContract, CanonicalCheckout checkout, int accessDays) {}
}

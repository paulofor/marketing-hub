package com.marketinghub.experiment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentType;
import java.net.URI;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: resolver a identidade e o pós-compra explícitos de um kit low-ticket. */
@Component
@RequiredArgsConstructor
@Slf4j
public class LowTicketCheckoutContract {
  private final ObjectMapper json;

  /** Valida o contrato administrativo vigente sem inferir URLs ou exigir um slot de assinatura. */
  public Delivery resolve(Experiment experiment) {
    var product = experiment.getProduct();
    require(
        experiment.getExperimentType() == ExperimentType.LOW_TICKET_PRODUCT
            && product != null
            && product.getProductTypeDefinition() != null
            && "LOW_TICKET_DIGITAL_PRODUCT".equals(product.getProductTypeDefinition().getCode()),
        "O tipo do produto e do experimento precisa ser low-ticket.");
    require(
        experiment.getUnitPrice() != null
            && experiment.getUnitPrice().signum() > 0
            && product.getCurrentPriceBrl() != null
            && experiment.getUnitPrice().compareTo(product.getCurrentPriceBrl()) == 0,
        "O preço persistido do produto diverge do experimento.");
    JsonNode root;
    try {
      root = json.readTree(Objects.toString(product.getValidationDefinitionJson(), "{}"));
    } catch (JsonProcessingException ex) {
      log.error(
          "Contrato de checkout low-ticket inválido productId={} experimentId={}",
          product.getId(),
          experiment.getId(),
          ex);
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Contrato de validação do kit não contém JSON válido.", ex);
    }
    require(
        root != null && root.isObject(), "O contrato de validação do kit precisa ser um objeto.");
    var checkout = root.path("commercialCheckout");
    require(
        checkout.isObject(),
        "Registre commercialCheckout explícito no contrato de validação do kit.");
    require(
        "LOW_TICKET_COMMERCIAL_CHECKOUT_V1".equals(checkout.path("contractVersion").asText()),
        "A versão do contrato commercialCheckout não é suportada.");
    String version = text(checkout, "productVersion");
    require(
        version.equals(product.getValidationDefinitionVersion()),
        "A versão do checkout diverge do contrato vigente do produto.");
    require(
        "MERCADO_PAGO".equals(text(checkout, "provider"))
            && "BRL".equals(text(checkout, "currency"))
            && "ONE_TIME".equals(text(checkout, "billingModel")),
        "O checkout do kit exige Mercado Pago, BRL e compra única.");
    require(
        checkout.path("priceBrl").isNumber()
            && checkout.path("priceBrl").decimalValue().compareTo(experiment.getUnitPrice()) == 0,
        "O preço do checkout diverge do experimento.");
    String key = text(root.path("checkoutIdentity"), "externalReference");
    require(
        key.matches("[a-z0-9]+(?:-[a-z0-9]+)*"), "A identidade de pagamento do kit é inválida.");
    String deliveryUrl = text(checkout, "deliveryPageUrl");
    require(
        securePublicUrl(deliveryUrl), "O pós-compra do kit precisa ter URL HTTPS pública válida.");
    return new Delivery(key, deliveryUrl);
  }

  /** Lê somente texto explícito, sem converter números, booleanos ou valores ausentes. */
  private String text(JsonNode node, String field) {
    var value = node.path(field);
    require(
        value.isTextual() && !value.textValue().isBlank(),
        "Campo obrigatório do checkout low-ticket: " + field + ".");
    return value.textValue().trim();
  }

  /** Recusa credenciais, fragmentos e destinos locais sem fazer requisições externas. */
  private boolean securePublicUrl(String value) {
    try {
      var uri = URI.create(value);
      String host = uri.getHost();
      return "https".equals(uri.getScheme())
          && host != null
          && host.contains(".")
          && !host.equals("localhost")
          && !host.matches("[0-9.]+")
          && uri.getUserInfo() == null
          && uri.getFragment() == null;
    } catch (IllegalArgumentException ex) {
      log.warn("URL de pós-compra low-ticket inválida", ex);
      return false;
    }
  }

  /** Interrompe a preparação antes da integração quando a fonte administrativa não é suficiente. */
  private void require(boolean valid, String reason) {
    if (!valid) throw new ResponseStatusException(HttpStatus.CONFLICT, reason);
  }

  /** Transporta apenas a referência comercial e a página de retorno já declaradas. */
  public record Delivery(String productKey, String deliveryPageUrl) {}
}

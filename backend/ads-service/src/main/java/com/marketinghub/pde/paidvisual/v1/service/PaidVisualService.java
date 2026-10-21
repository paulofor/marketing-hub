package com.marketinghub.pde.paidvisual.v1.service;

import static com.marketinghub.pde.visualpersonalization.v1.service.VisualPreparationService.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.imagegenerator.ImageGenerationRequest;
import com.marketinghub.pde.paidvisual.v1.PaidVisualOrder;
import com.marketinghub.pde.paidvisual.v1.service.contract.PaidVisualContract.*;
import com.marketinghub.pde.visualpersonalization.v1.service.VisualPreparationBudget;
import com.marketinghub.pde.visualpersonalization.v1.service.VisualPreparationService;
import com.marketinghub.repository.jpa.imagegenerator.ImageGenerationRequestRepository;
import com.marketinghub.repository.jpa.pde.PaidVisualOrderRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Responsabilidade: coordenar compra, acesso e tentativas pagas mantendo a fila visual existente.
 */
@Service
@Slf4j
public class PaidVisualService {
  private final PaidVisualOrderRepository orders;
  private final ProductRepository products;
  private final ImageGenerationRequestRepository images;
  private final PaidVisualPolicy policy;
  private final PaidVisualMail mail;
  private final VisualPreparationService visual;

  /** Recebe repositórios, contrato e os mecanismos compartilhados de entrega e recuperação. */
  public PaidVisualService(
      PaidVisualOrderRepository orders,
      ProductRepository products,
      ImageGenerationRequestRepository images,
      PaidVisualPolicy policy,
      PaidVisualMail mail,
      VisualPreparationService visual) {
    this.orders = orders;
    this.products = products;
    this.images = images;
    this.policy = policy;
    this.mail = mail;
    this.visual = visual;
  }

  /** Expõe somente oferta e condições, sem entrada pessoal, chave ou relatório financeiro. */
  @Transactional(readOnly = true)
  public JsonNode offer(Long experimentId) {
    var source = policy.resolve(experimentId, false);
    var out = source.objectNode();
    for (String key :
        List.of(
            "productName",
            "productVersion",
            "experimentId",
            "priceBrl",
            "selling",
            "checkoutUrl",
            "accessDays",
            "maxRegenerations")) if (source.has(key)) out.set(key, source.get(key));
    for (String key :
        List.of(
            "contractVersion",
            "productSlug",
            "checkoutPrepared",
            "supportEmail",
            "termsUrl",
            "privacyUrl",
            "refundPolicyUrl")) if (source.has(key)) out.set(key, source.get(key));
    out.put("accessDeliveryReady", mail.ready());
    if (!mail.ready()) {
      out.put("selling", false);
      out.remove("checkoutUrl");
    }
    return out;
  }

  /**
   * Reconcilia apenas pagamentos verificados, recusando mudança de identidade e regressão após
   * reembolso.
   */
  @Transactional
  public Map<String, String> payment(Payment input) {
    String status = input.status().toLowerCase(Locale.ROOT);
    require(
        List.of("approved", "refunded", "charged_back").contains(status),
        "Estado financeiro não final.");
    long experimentId = number(input.metadata().get("experimentId"));
    long productId = number(input.metadata().get("productId"));
    products.findLockedById(productId).orElseThrow(() -> fail(404, "Produto não encontrado."));
    var existing = orders.lockById(input.paymentId());
    ObjectNode contract =
        existing
            .map(o -> (ObjectNode) visual.read(o.getContractJson()))
            .orElseGet(
                () ->
                    policy.resolveSettlement(
                        experimentId, String.valueOf(input.metadata().get("experienceVersion"))));
    require(
        PaidVisualPolicy.VERSION.equals(String.valueOf(input.metadata().get("deliveryContract")))
            && Boolean.TRUE.equals(input.metadata().get("providerLiveMode"))
            && contract.path("productId").asLong() == productId
            && contract.path("experimentId").asLong() == experimentId
            && contract
                .path("productVersion")
                .asText()
                .equals(String.valueOf(input.metadata().get("experienceVersion")))
            && contract.path("productSlug").asText().equals(input.externalReference())
            && "BRL".equals(input.currency())
            && contract.path("priceBrl").decimalValue().compareTo(input.amount()) == 0,
        "Pagamento diverge do produto, preço, versão ou experimento homologados.");
    String email = input.email().trim().toLowerCase(Locale.ROOT);
    require(
        !email.endsWith("@sandbox.local"),
        "Pagamento de teste não gera receita ou direito comercial.");
    var order = existing.orElseGet(PaidVisualOrder::new);
    if (existing.isPresent()) {
      require(
          order.getBuyerEmail().equals(email)
              && order
                  .getPaidAt()
                  .equals(input.dateApproved().truncatedTo(java.time.temporal.ChronoUnit.SECONDS)),
          "Callback mudou a identidade da compra.");
      if (!"approved".equals(order.getPaymentStatus()) && "approved".equals(status))
        return Map.of("status", order.getPaymentStatus());
    } else {
      require(
          "approved".equals(status), "Compra aprovada original não encontrada para o reembolso.");
      require(
          !input.dateApproved().isAfter(Instant.now().plusSeconds(120)),
          "Data de aprovação inválida.");
      order.setPaymentId(input.paymentId());
      order.setProductId(productId);
      order.setExperimentId(experimentId);
      order.setProductSlug(input.externalReference());
      order.setExperienceVersion(contract.path("productVersion").asText());
      order.setBuyerEmail(email);
      order.setAmountBrl(input.amount());
      order.setPaidAt(input.dateApproved().truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
      order.setExpiresAt(input.dateApproved().plusSeconds(7 * 86400L));
      order.setContractJson(visual.write(contract));
      order.setCreatedAt(Instant.now());
    }
    order.setPaymentStatus(status);
    order.setUpdatedAt(Instant.now());
    orders.saveAndFlush(order);
    log.info(
        "Compra visual reconciliada paymentId={} productId={} experimentId={} status={}",
        input.paymentId(),
        productId,
        experimentId,
        status);
    return Map.of("status", status);
  }

  /**
   * Envia link somente ao e-mail confirmado, com resposta opaca e limite de frequência por pedido.
   */
  @Transactional
  public Map<String, String> login(Long experimentId, Login input) {
    var candidate =
        orders.findFirstByExperimentIdAndBuyerEmailOrderByPaidAtDesc(
            experimentId, input.email().trim().toLowerCase(Locale.ROOT));
    if (candidate.isPresent()) {
      var order = locked(candidate.get().getPaymentId());
      if (active(order)
          && (order.getLastEmailAt() == null
              || order.getLastEmailAt().isBefore(Instant.now().minusSeconds(60)))) {
        String token = UUID.randomUUID().toString() + UUID.randomUUID();
        String url =
            visual.read(order.getContractJson()).path("deliveryUrl").asText() + "#access=" + token;
        order.setAccessHash(VisualPreparationBudget.hash(token));
        order.setLastEmailAt(Instant.now());
        order.setUpdatedAt(Instant.now());
        orders.saveAndFlush(order);
        mail.send(order.getPaymentId(), order.getBuyerEmail(), url);
      }
    }
    return Map.of(
        "message",
        "Se houver uma compra aprovada com esse e-mail, enviaremos seu acesso. Confira também o spam.");
  }

  /** Recupera resultados sem nova inferência e sem expor informações da compra a outra pessoa. */
  @Transactional(readOnly = true)
  public JsonNode session(String token) {
    var order = access(token);
    var out = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
    out.put("productName", visual.read(order.getContractJson()).path("productName").asText());
    out.put("productVersion", order.getExperienceVersion());
    var contract = visual.read(order.getContractJson());
    for (String key : List.of("supportEmail", "termsUrl", "privacyUrl", "refundPolicyUrl"))
      if (contract.has(key)) out.set(key, contract.get(key));
    out.put("expiresAt", order.getExpiresAt().toString());
    out.put(
        "regenerationAvailable",
        order.getInitialJobId() != null && order.getRegenerationJobId() == null);
    var jobs = out.putArray("deliveries");
    for (String id : Arrays.asList(order.getInitialJobId(), order.getRegenerationJobId()))
      if (id != null) {
        var row = images.findFirstByJobId(id).orElseThrow();
        var source = visual.view(row);
        var delivery = source.objectNode();
        for (String key : List.of("jobId", "status", "input", "imageBase64"))
          if (source.has(key)) delivery.set(key, source.get(key));
        delivery.set("occasionAt", visual.read(row.getPrompt()).path("occasionAt"));
        jobs.add(delivery);
      }
    return out;
  }

  /**
   * Enfileira entrada após compra e limita a regeneração, serializando solicitações concorrentes.
   */
  @Transactional
  public JsonNode submit(String token, Submit input) {
    var order = locked(access(token).getPaymentId());
    require(active(order), "Acesso encerrado ou reembolsado.");
    var contract = visual.read(order.getContractJson());
    String current = input.regeneration() ? order.getRegenerationJobId() : order.getInitialJobId();
    String hash = VisualPreparationBudget.hash(visual.write(input.input()) + input.occasionAt());
    if (current != null) {
      var row = images.findFirstByJobId(current).orElseThrow();
      var ctx = visual.read(row.getPrompt());
      require(
          input.operationKey().equals(ctx.path("operationKey").asText())
              && hash.equals(ctx.path("submissionHash").asText()),
          "Essa entrega já foi solicitada. Recupere o resultado sem repetir a geração.");
      return session(token);
    }
    require(input.occasionAt().isAfter(Instant.now()), "Informe uma ocasião futura.");
    if (input.regeneration()) {
      require(order.getInitialJobId() != null, "A primeira entrega ainda não foi solicitada.");
      var original = images.findFirstByJobId(order.getInitialJobId()).orElseThrow();
      require(
          "PDE_COMPLETED".equals(original.getStatus()),
          "A primeira entrega precisa estar concluída antes da regeneração.");
      require(
          visual
              .read(original.getPrompt())
              .path("occasionAt")
              .asText()
              .equals(input.occasionAt().toString()),
          "A regeneração atende à mesma ocasião.");
    }
    String serialized = visual.write(input.input());
    require(
        !serialized
            .toLowerCase(Locale.ROOT)
            .matches(
                "(?s).*(emagrec|peso corporal|cirurgia|diagnóstico|nudez|foto corporal|foto do corpo|avaliar meu corpo).*"),
        "Informe somente ocasião, roupas e restrições práticas.");
    if (input.occasionAt().isBefore(order.getExpiresAt())) order.setExpiresAt(input.occasionAt());
    var ctx = contract.deepCopy();
    ((ObjectNode) ctx)
        .put("contractVersion", PaidVisualPolicy.VERSION)
        .put("trafficClass", "CUSTOMER")
        .put("paymentId", order.getPaymentId())
        .put("operationKey", input.operationKey())
        .put("submissionHash", hash)
        .put("inputHash", VisualPreparationBudget.hash(serialized))
        .put("occasionAt", input.occasionAt().toString());
    ((ObjectNode) ctx).set("input", visual.read(serialized));
    var row = new ImageGenerationRequest();
    row.setProductId(order.getProductId());
    row.setExperimentId(order.getExperimentId());
    row.setCommercialPlanId(contract.path("commercialPlanId").asLong());
    row.setJobId(PREFIX + UUID.randomUUID());
    row.setBatchJobId("paid:" + order.getPaymentId());
    row.setStatus("PDE_QUEUED");
    row.setModel(contract.path("model").asText());
    row.setServiceTier("flex");
    row.setOutputFormat("png");
    row.setPrompt(visual.write(ctx));
    row.setCreatedAt(Instant.now());
    images.saveAndFlush(row);
    if (input.regeneration()) order.setRegenerationJobId(row.getJobId());
    else order.setInitialJobId(row.getJobId());
    order.setUpdatedAt(Instant.now());
    orders.saveAndFlush(order);
    log.info(
        "Entrega visual enfileirada productId={} experimentId={} paymentId={} jobId={} inputHash={}",
        order.getProductId(),
        order.getExperimentId(),
        order.getPaymentId(),
        row.getJobId(),
        hash);
    return session(token);
  }

  /** Exige vínculo pago ativo antes de o executor reservar ou enviar uma nova chamada. */
  public BigDecimal authorizeGeneration(ImageGenerationRequest row) {
    var ctx = visual.read(row.getPrompt());
    var order = locked(ctx.path("paymentId").asText());
    require(
        active(order)
            && row.getProductId().equals(order.getProductId())
            && row.getExperimentId().equals(order.getExperimentId())
            && (row.getJobId().equals(order.getInitialJobId())
                || row.getJobId().equals(order.getRegenerationJobId()))
            && order.getExperienceVersion().equals(ctx.path("productVersion").asText()),
        "Direito pago inválido para esta tentativa.");
    return visual.read(order.getContractJson()).path("maxGenerationUsd").decimalValue();
  }

  /** Autoriza conciliação apenas de uma tentativa vinculada ao bearer da própria compra. */
  @Transactional(readOnly = true)
  public void authorizeReplay(String token, String jobId) {
    var order = access(token);
    require(
        jobId.equals(order.getInitialJobId()) || jobId.equals(order.getRegenerationJobId()),
        "Entrega de outro acesso.");
  }

  /**
   * Distingue o contrato pago, que nunca pode usar autorização sintética ou orçamento de
   * preparação.
   */
  public boolean isPaid(ImageGenerationRequest row) {
    return PaidVisualPolicy.VERSION.equals(
        visual.read(row.getPrompt()).path("contractVersion").asText());
  }

  /** Localiza o bearer sem persistir sua forma recuperável. */
  private PaidVisualOrder access(String token) {
    if (token == null || token.length() < 32 || token.length() > 96)
      throw fail(401, "Acesso inválido.");
    var order =
        orders
            .findByAccessHash(VisualPreparationBudget.hash(token))
            .orElseThrow(() -> fail(401, "Acesso inválido."));
    if (!active(order)) throw fail(403, "Acesso encerrado ou reembolsado.");
    return order;
  }

  /** Mantém a mesma ordem de locks do executor, serializando pedidos do produto. */
  private PaidVisualOrder locked(String id) {
    var reference = orders.findById(id).orElseThrow(() -> fail(404, "Compra não encontrada."));
    var product = products.findLockedById(reference.getProductId()).orElseThrow();
    require(
        !Boolean.FALSE.equals(product.getAutomaticExecutionEnabled()), "O produto está em STOP.");
    return orders.lockById(id).orElseThrow();
  }

  /** Confere aprovação e prazo sem reinterpretar reembolso como nova autorização. */
  private boolean active(PaidVisualOrder order) {
    return "approved".equals(order.getPaymentStatus())
        && order.getExpiresAt().isAfter(Instant.now());
  }

  /** Lê identificadores da confirmação interna sem aceitar texto arbitrário. */
  private long number(Object value) {
    try {
      return Long.parseLong(String.valueOf(value));
    } catch (NumberFormatException ex) {
      log.warn("Identidade ausente no callback visual pago", ex);
      throw fail(409, "Identidade financeira inválida.");
    }
  }
}

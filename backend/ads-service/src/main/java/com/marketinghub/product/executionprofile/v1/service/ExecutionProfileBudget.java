package com.marketinghub.product.executionprofile.v1.service;

import com.marketinghub.product.executionprofile.v1.ExecutionProfileConsumption;
import com.marketinghub.repository.jpa.product.ProductRepository;
import com.marketinghub.repository.jpa.productexecution.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Responsabilidade: reservar consumo atomicamente antes do provedor e conservar custos
 * desconhecidos.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExecutionProfileBudget {
  private final ProductRepository products;
  private final ExecutionProfileRepository profiles;
  private final ExecutionProfileBindingRepository bindings;
  private final ExecutionProfileConsumptionRepository consumption;
  private final ExecutionProfileContext context;

  /** Reserva entrega por pacote ou produção privada total no contexto exato antes do consumo. */
  @Transactional
  public Long reserve(
      Long productId,
      String reference,
      String usageKey,
      String operationKey,
      String input,
      String model,
      int units,
      boolean testData) {
    products.findLockedById(productId).orElseThrow();
    var bound = bindings.findByProductIdAndSourceReference(productId, reference);
    if (bound.isEmpty()) {
      if (bindings.existsByProductId(productId))
        throw conflict(
            "Informe a referência vinculada à ficha antes de gerar imagens deste produto.");
      return null;
    }
    if (consumption.existsByBindingIdAndTestDataAndStatusIn(
        bound.get().getId(), testData, Set.of("COST_PENDING", "OVER_BUDGET")))
      throw conflict(
          "Há custo desconhecido ou acima da reserva nesta execução. Concilie antes de iniciar outro pacote.");
    if (operationKey == null
        || !operationKey.matches("[A-Za-z0-9._:-]{1,100}")
        || usageKey == null
        || usageKey.length() > 100
        || units < 1) throw conflict("A geração exige correlação idempotente e quantidade válida.");
    String hash = hash(input + "|" + model + "|" + units + "|" + usageKey + "|" + testData);
    var previous = consumption.findByBindingIdAndOperationKey(bound.get().getId(), operationKey);
    if (previous.isPresent())
      throw conflict(
          previous.get().getInputHash().equals(hash)
              ? "Esta operação já foi reservada. Consulte seu resultado; não repita a chamada paga."
              : "A chave de operação já identifica outra entrada.");
    var profile =
        profiles.findByIdAndProductId(bound.get().getProfileId(), productId).orElseThrow();
    for (String checkpoint :
        testData
            ? List.of("OFFER", "DELIVERY_DESIGN")
            : List.of("OFFER", "DELIVERY_DESIGN", "HOMOLOGATION", "OPERATION")) {
      String blocked = context.financialBlocker(profile, checkpoint);
      if (blocked != null) throw conflict(blocked);
    }
    var contract = context.contract(profile);
    var production = contract.productionBudget();
    String costModel = testData ? production.costModel() : contract.costModel();
    int maximumAttempts = testData ? production.maximumAttempts() : contract.maximumAttempts();
    BigDecimal attemptCost =
        testData ? production.maximumAttemptCostBrl() : contract.maximumAttemptCostBrl();
    BigDecimal totalLimit =
        testData ? production.maximumTotalCostBrl() : contract.maximumDeliveryCostBrl();
    if (!costModel.equals(model))
      throw conflict("O modelo mudou. Atualize a ficha e o parecer antes de consumir.");
    if (attemptCost.signum() <= 0 || totalLimit.signum() <= 0)
      throw conflict("Não há orçamento aprovado para consumo de IA neste escopo.");
    var history =
        testData
            ? consumption.findByBindingIdAndTestData(bound.get().getId(), true)
            : consumption.findByBindingIdAndUsageKeyAndTestData(
                bound.get().getId(), usageKey, false);
    if (history.stream()
        .anyMatch(e -> Set.of("COST_PENDING", "OVER_BUDGET").contains(e.getStatus())))
      throw conflict(
          "Há custo desconhecido ou acima da reserva. Concilie a causa antes de novas chamadas.");
    int attempted = history.stream().mapToInt(ExecutionProfileConsumption::getUnits).sum();
    BigDecimal used =
        history.stream()
            .map(
                e ->
                    e.getActualBrl() == null
                        ? e.getReservedBrl()
                        : e.getActualBrl().max(e.getReservedBrl()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal reserved = attemptCost.multiply(BigDecimal.valueOf(units));
    if (attempted + units > maximumAttempts || used.add(reserved).compareTo(totalLimit) > 0)
      throw conflict("O pacote atingiu o limite de tentativas ou o teto financeiro aprovado.");
    var entry = new ExecutionProfileConsumption();
    entry.setBindingId(bound.get().getId());
    entry.setOperationKey(operationKey);
    entry.setUsageKey(usageKey);
    entry.setInputHash(hash);
    entry.setUnits(units);
    entry.setReservedBrl(reserved);
    entry.setTestData(testData);
    entry.setStatus("RESERVED");
    entry.setCreatedAt(Instant.now());
    return consumption.saveAndFlush(entry).getId();
  }

  /** Concilia a resposta preservando reserva, falha cobrada, sobrecusto e evidência de origem. */
  @Transactional
  public void settle(
      Long productId, Long reservationId, BigDecimal actualBrl, String evidence, boolean failed) {
    if (reservationId == null) return;
    products.findLockedById(productId).orElseThrow();
    var entry = consumption.findById(reservationId).orElseThrow();
    var binding = bindings.findById(entry.getBindingId()).orElseThrow();
    if (!productId.equals(binding.getProductId()))
      throw conflict("A reserva não pertence ao produto.");
    if (actualBrl != null && actualBrl.signum() < 0)
      throw conflict("Custo realizado não pode ser negativo.");
    if (evidence == null || evidence.isBlank())
      throw conflict("Informe a evidência da resposta e do custo.");
    if (entry.getActualBrl() != null) {
      if (actualBrl == null || entry.getActualBrl().compareTo(actualBrl) != 0)
        throw conflict("O custo desta operação já foi conciliado.");
      return;
    }
    entry.setActualBrl(actualBrl);
    entry.setEvidence(evidence);
    entry.setFinishedAt(Instant.now());
    entry.setStatus(
        actualBrl == null
            ? "COST_PENDING"
            : actualBrl.compareTo(entry.getReservedBrl()) > 0
                ? "OVER_BUDGET"
                : failed ? "FAILED_CHARGED" : "SETTLED");
    consumption.saveAndFlush(entry);
  }

  /** Produz correlação de entrada sem persistir o dado pessoal bruto no ledger financeiro. */
  private String hash(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception ex) {
      log.error("Falha no hash da reserva de consumo.", ex);
      throw new IllegalStateException(ex);
    }
  }

  /** Expõe o motivo de bloqueio antes de qualquer chamada externa. */
  private ResponseStatusException conflict(String reason) {
    return new ResponseStatusException(HttpStatus.CONFLICT, reason);
  }
}

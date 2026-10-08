package com.marketinghub.pde.visualpersonalization.v1.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.experimentstrategist.ExperimentStrategistExecutionRepository;
import com.marketinghub.repository.jpa.financialagent.FinancialAgentExecutionRepository;
import com.marketinghub.repository.jpa.financialagent.StudioCostLedgerEntryRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: conferir autorização já registrada e consumo da preparação privada exata. */
@Component
@RequiredArgsConstructor
@Slf4j
public class VisualPreparationBudget {
  private static final Pattern AUTHORIZATION =
      Pattern.compile(
          "Autorização explícita do usuário em (\\d{2}/\\d{2}/\\d{4}): teto TOTAL de USD ([0-9]+(?:\\.[0-9]{1,2})?) para homologar geração personalizada real");
  private final StudioCostLedgerEntryRepository ledger;
  private final FinancialAgentExecutionRepository financial;
  private final ExperimentStrategistExecutionRepository strategist;
  private final ObjectMapper json;

  /** Responsabilidade: expor a origem, limites e consumo conhecido sem autorizar outro contexto. */
  public record Budget(
      String authorizationHash,
      Instant authorizedSince,
      BigDecimal maximumUsd,
      BigDecimal knownEstimatedUsd,
      BigDecimal availableUsd,
      String productVersion,
      boolean costComplete,
      String blocker) {}

  /**
   * Lê apenas o formato explícito já persistido; texto ambíguo ou de outro produto falha fechado.
   */
  public Budget read(Product product, CommercialPlan plan, Long experimentId, String excludedJob) {
    String evidence = plan.getNextAction() == null ? "" : plan.getNextAction();
    var matcher = AUTHORIZATION.matcher(evidence);
    if (!matcher.find()
        || !evidence.contains("produto " + product.getId() + ", experimento " + experimentId + ",")
        || !evidence.contains("sem mídia, campanha, cobrança real")) {
      throw conflict(
          "A preparação exige autorização explícita de IA do mesmo produto e experimento.");
    }
    String date = matcher.group(1);
    BigDecimal maximum = new BigDecimal(matcher.group(2));
    if (maximum.signum() <= 0 || matcher.find())
      throw conflict("A autorização de preparação é ambígua ou não possui teto positivo.");
    Instant since =
        LocalDate.parse(date, DateTimeFormatter.ofPattern("dd/MM/uuuu"))
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant();
    if (since.isAfter(Instant.now())) throw conflict("A autorização ainda não está vigente.");
    String version;
    try {
      var experience = json.readTree(product.getPdeExperienceJson());
      version = experience.path("privatePrototypeAcceptance").path("prototypeVersion").asText();
      if (version.isBlank()
          || !"PASS".equals(experience.path("agentValidation").path("status").asText()))
        throw conflict("Preserve e conclua primeiro a experiência privada de referência.");
    } catch (ResponseStatusException ex) {
      log.warn(
          "Contrato privado indisponível productId={} planId={}",
          product.getId(),
          plan.getId(),
          ex);
      throw ex;
    } catch (Exception ex) {
      log.error(
          "Falha ao ler contrato privado productId={} planId={}",
          product.getId(),
          plan.getId(),
          ex);
      throw conflict("O contrato privado do produto não pôde ser conferido.");
    }
    BigDecimal total = BigDecimal.ZERO;
    boolean complete = true;
    String blocker = null;
    for (var entry : ledger.findByCommercialPlanIdOrderByCreatedAtAsc(plan.getId())) {
      if (!experimentId.equals(entry.getExperimentId())
          || entry.getCreatedAt().isBefore(since)
          || entry.getSourceId().equals(excludedJob)) continue;
      var cost =
          entry.getProviderCostUsd() != null
              ? entry.getProviderCostUsd()
              : entry.getEstimatedCostUsd();
      if (cost == null || cost.signum() < 0) {
        complete = false;
        blocker =
            "Há tentativa com custo desconhecido; recupere e concilie a resposta antes de nova inferência.";
      } else total = total.add(cost);
    }
    for (var entry : financial.findByCommercialPlanIdOrderByCreatedAtDesc(plan.getId())) {
      if (entry.getCreatedAt().isBefore(since)) continue;
      if (!java.util.List.of("COMPLETED", "FAILED").contains(entry.getStatus().name())
          || entry.getEstimatedCost() == null
          || entry.getEstimatedCost().signum() < 0) {
        complete = false;
        blocker = "Existe parecer de Plutus em execução ou com custo pendente nesta preparação.";
      } else total = total.add(entry.getEstimatedCost());
    }
    for (var entry : strategist.findByCommercialPlanIdOrderByCreatedAtDesc(plan.getId())) {
      if (entry.getCreatedAt().isBefore(since)) continue;
      if (!java.util.List.of("COMPLETED", "FAILED").contains(entry.getStatus().name())
          || entry.getEstimatedCost() == null
          || entry.getEstimatedCost().signum() < 0) {
        complete = false;
        blocker = "Existe parecer de Atena em execução ou com custo pendente nesta preparação.";
      } else total = total.add(entry.getEstimatedCost());
    }
    BigDecimal available = maximum.subtract(total).max(BigDecimal.ZERO);
    if (available.signum() == 0) blocker = "A preparação atingiu o teto total autorizado de IA.";
    return new Budget(hash(evidence), since, maximum, total, available, version, complete, blocker);
  }

  /** Gera a identidade da autorização atual sem expor seu texto a consumidores privados. */
  public static String hash(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception ex) {
      log.error("Falha ao identificar autorização de preparação.", ex);
      throw new IllegalStateException("Não foi possível identificar a autorização.", ex);
    }
  }

  /** Mantém o motivo de recusa visível antes de qualquer consumo. */
  private static ResponseStatusException conflict(String message) {
    return new ResponseStatusException(HttpStatus.CONFLICT, message);
  }
}

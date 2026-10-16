package com.marketinghub.salesvideo.service.reconcileNarrationCosts;

import static com.marketinghub.salesvideo.service.reconcileNarrationCosts.NarrationUsageEvidence.require;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.financialagent.service.StudioCostLedgerService;
import com.marketinghub.financialagent.service.narrationCost.NarrationCostView;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoJobEventRepository;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoJobRepository;
import com.marketinghub.salesvideo.*;
import com.marketinghub.salesvideo.tenant.TenantContextHolder;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Persiste a conciliação incremental de locução com recibos, contexto e auditoria imutáveis. */
@Component
@RequiredArgsConstructor
@Slf4j
public class NarrationCostReconciliation {
  static final String COHORT = "OPENAI_USAGE_COHORT_ESTIMATE";
  static final String REUSED = "PRESERVED_NARRATION_NO_NEW_CALL";
  static final String BEFORE_TTS = "FAILED_BEFORE_TTS_AUDITED";
  private static final String CONTRACT = "NARRATION_COST_RECONCILIATION_V1";
  private final SalesVideoJobRepository jobs;
  private final StudioCostLedgerService ledger;
  private final SalesVideoJobEventRepository events;
  private final ObjectMapper json;

  /**
   * Valida o lote inteiro antes de registrar custos; não chama provedor, aprova ou retoma tarefas.
   */
  @Transactional
  public ReconcileNarrationCostsResponse reconcile(
      Long experimentId, ReconcileNarrationCostsRequest input) {
    String tenant = TenantContextHolder.requireTenant();
    require(experimentId != null && experimentId > 0, "Experimento inválido.");
    require(
        !json.valueToTree(input).toString().matches("(?s).*sk-[A-Za-z0-9_-]{16,}.*"),
        "O extrato não pode conter credenciais.");
    require(
        input.jobIds() != null
            && !input.jobIds().isEmpty()
            && input.jobIds().size() <= 50
            && input.jobIds().stream().allMatch(id -> id != null && id > 0)
            && new HashSet<>(input.jobIds()).size() == input.jobIds().size(),
        "Selecione tentativas únicas.");
    require(
        input.evidenceReference() != null
            && !input.evidenceReference().isBlank()
            && input.evidenceReference().length() <= 1000,
        "Informe a fonte conferida da conciliação.");
    var attempts = new ArrayList<Attempt>();
    var paid = new ArrayList<NarrationUsageEvidence.PaidSpeech>();
    var requestIds = new HashSet<String>();
    for (long id : input.jobIds().stream().sorted().toList()) {
      SalesVideoJob job =
          jobs.findByIdForUpdate(id)
              .orElseThrow(() -> NarrationUsageEvidence.invalid("Tentativa não encontrada."));
      JsonNode metadata = NarrationUsageEvidence.read(job.getMetadataJson(), id, json);
      require(
          tenant.equals(job.getTenantId())
              && metadata.path("experimentId").asLong() == experimentId
              && job.getProfile() != null
              && job.getProfile().getProduct() != null
              && metadata.path("productId").asLong() == job.getProfile().getProduct().getId(),
          "A tentativa pertence a outro tenant, produto ou experimento.");
      require(
          job.getJobType() == SalesVideoJobType.POST_PRODUCTION
              && "MUSA_POST_PRODUCTION".equals(job.getProviderName())
              && Set.of(SalesVideoStatus.VIDEO_FAILED, SalesVideoStatus.VIDEO_READY)
                  .contains(job.getStatus()),
          "Somente finalizações encerradas podem ser conciliadas.");
      var entry =
          ledger
              .narrationCost(id)
              .orElseThrow(
                  () ->
                      NarrationUsageEvidence.invalid("A tentativa não tem lançamento financeiro."));
      require(
          Objects.equals(entry.experimentId(), experimentId)
              && Objects.equals(entry.productId(), job.getProfile().getProduct().getId())
              && "AUDIO".equals(entry.assetType())
              && entry.providerCostUsd() == null,
          "O lançamento diverge do contexto ou já tem cobrança confirmada pelo provedor.");
      JsonNode audit = NarrationUsageEvidence.audit(job, json);
      boolean zero = NarrationUsageEvidence.withoutNewSpeech(job, audit, metadata);
      NarrationUsageEvidence.PaidSpeech speech =
          zero ? null : NarrationUsageEvidence.paid(job, audit, requestIds);
      if (speech != null) paid.add(speech);
      if (zero && metadata.has("preservedNarration")) {
        long parent = metadata.at("/preservedNarration/sourceJobId").asLong();
        var previous = ledger.narrationCost(parent).orElse(null);
        require(
            parent > 0
                && previous != null
                && Objects.equals(previous.experimentId(), experimentId)
                && Objects.equals(previous.productId(), job.getProfile().getProduct().getId())
                && (previous.providerCostUsd() != null
                    || previous.estimatedCostUsd() != null
                    || attempts.stream().anyMatch(value -> value.job().getId() == parent)),
            "Concilie primeiro a fonte da voz preservada, sem transferir custo entre experimentos.");
      }
      attempts.add(
          new Attempt(
              job,
              entry,
              speech,
              zero ? metadata.has("preservedNarration") ? REUSED : BEFORE_TTS : COHORT));
    }
    BigDecimal total =
        NarrationUsageEvidence.cost(input.usage(), paid, input.exclusiveUsageConfirmed());
    int calls = paid.stream().mapToInt(NarrationUsageEvidence.PaidSpeech::count).sum();
    String hash = hash(json.valueToTree(input).toString());
    var values = new LinkedHashMap<Long, BigDecimal>();
    for (var attempt : attempts) {
      BigDecimal value =
          attempt.speech() == null
              ? BigDecimal.ZERO.setScale(6)
              : total
                  .multiply(BigDecimal.valueOf(attempt.speech().count()))
                  .divide(BigDecimal.valueOf(calls), 6, RoundingMode.CEILING);
      var entry = attempt.entry();
      if (Set.of(COHORT, REUSED, BEFORE_TTS).contains(Objects.toString(entry.costEvidence(), "")))
        require(
            entry.estimatedCostUsd() != null && entry.estimatedCostUsd().compareTo(value) == 0,
            "Esta tentativa já possui outra conciliação; preserve a auditoria antes de corrigir o valor.");
      values.put(attempt.job().getId(), value);
    }
    for (var attempt : attempts) {
      var entry = attempt.entry();
      if (attempt.evidence().equals(entry.costEvidence()) && entry.estimatedCostUsd() != null)
        continue;
      BigDecimal reconciledCost = values.get(attempt.job().getId());
      ledger.reconcileNarrationCost(
          attempt.job().getId(),
          entry.productId(),
          experimentId,
          reconciledCost,
          attempt.evidence());
      var details = json.createObjectNode();
      details
          .put("contractVersion", CONTRACT)
          .put("evidenceHash", hash)
          .put("evidenceReference", input.evidenceReference())
          .put("operatorName", TenantContextHolder.resolveUserEmail(null))
          .put("estimatedCostUsd", reconciledCost)
          .put("cohortCostUsd", total)
          .put("providerRequests", calls)
          .put("allocation", "REQUEST_COUNT_CONSERVATIVE_ESTIMATE")
          .put("providerInvoiceConfirmed", false)
          .put("newProviderCall", false);
      if (input.usage() != null) details.set("usage", input.usage());
      events.save(
          SalesVideoJobEvent.builder()
              .job(attempt.job())
              .eventType(SalesVideoJobEventType.PROGRESS)
              .oldStatus(attempt.job().getStatus())
              .newStatus(attempt.job().getStatus())
              .message("Conciliação auditável da locução: " + attempt.evidence())
              .detailsJson(details.toString())
              .build());
    }
    return new ReconcileNarrationCostsResponse(
        experimentId,
        values.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add),
        calls,
        List.copyOf(values.keySet()),
        hash,
        false);
  }

  /** Identifica o mesmo extrato e a seleção, sem persistir segredo de acesso ao provedor. */
  private String hash(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception ex) {
      log.error("Falha ao identificar conciliação de locução", ex);
      throw new IllegalStateException("Não foi possível identificar a conciliação.", ex);
    }
  }

  /** Mantém o lançamento e o recibo sob o mesmo contexto validado na transação. */
  private record Attempt(
      SalesVideoJob job,
      NarrationCostView entry,
      NarrationUsageEvidence.PaidSpeech speech,
      String evidence) {}
}

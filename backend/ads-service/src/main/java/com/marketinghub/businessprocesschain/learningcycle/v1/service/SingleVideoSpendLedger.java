package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import com.marketinghub.businessprocesschain.learningcycle.v1.SingleVideoSpend;
import com.marketinghub.repository.jpa.learningcycle.SingleVideoSpendRepository;
import com.marketinghub.videocontract.v1.BoundedSpendContracts.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Responsabilidade: reservar e consumir uma única vez operações sob o lock do recibo original. */
@Component
@RequiredArgsConstructor
@Slf4j
public class SingleVideoSpendLedger {
  private java.time.Clock clock = java.time.Clock.systemUTC();

  /** Controla vencimento em testes sem oferecer desvio de relógio ao executor. */
  void setClock(java.time.Clock clock) {
    this.clock = Objects.requireNonNull(clock);
  }

  private final SingleVideoSpendRepository spends;
  private final LearningCycleJson json;

  /** Soma inclusive envios incertos; resposta ou expiração não significam custo conciliado. */
  public BigDecimal liability(Long receiptId) {
    return spends.findByReceiptIdOrderByIdAsc(receiptId).stream()
        .map(s -> s.getSettledCostUsd() == null ? s.getMaxCostUsd() : s.getSettledCostUsd())
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /** Consulta a operação preservada antes de ler credencial ou consultar novamente o provedor. */
  public Lease find(Long receiptId, String operation) {
    return spends
        .findByReceiptIdAndOperationKey(receiptId, operation)
        .map(row -> lease(row, false))
        .orElse(null);
  }

  /** Expõe apenas valores retidos e identidades, sem publicar prompts ou respostas brutas. */
  public Map<String, Object> report(Long receiptId, BigDecimal totalLimit) {
    var rows = spends.report(receiptId);
    BigDecimal retained =
        rows.stream()
            .map(
                row ->
                    row.getSettledCostUsd() == null ? row.getMaxCostUsd() : row.getSettledCostUsd())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    return Map.of(
        "receiptId",
        receiptId,
        "totalLimitUsd",
        totalLimit,
        "retainedMaximumUsd",
        retained,
        "unallocatedAuthorizationUsd",
        totalLimit.subtract(retained).max(BigDecimal.ZERO),
        "uncertainOperations",
        rows.stream()
            .filter(row -> Set.of("SUBMITTED", "UNCERTAIN").contains(row.getState()))
            .count(),
        "unsettledOperations",
        rows.stream()
            .filter(row -> row.getMaxCostUsd().signum() > 0 && row.getSettledCostUsd() == null)
            .count(),
        "guidance",
        "Máximos retidos incluem respostas incertas e não são custo faturado. A reserva de clipes"
            + " já está incluída; não a some novamente. A parcela ainda não reservada não comprova"
            + " saldo disponível: cada chamada reconfere custos externos e vigência.",
        "voiceRoute",
        Map.of(
            "model",
            "tts-1-hd",
            "voice",
            "nova",
            "usdPerMillionCharacters",
            30,
            "notice",
            "Rota explícita de voz sintética HD, cobrada por caracteres; qualidade final depende de"
                + " Psique. Sem substituição automática por voz local."));
  }

  /** Identifica custo já coberto pelo teto sem somar novamente o mesmo ledger do Estúdio. */
  public boolean covers(Long receiptId, String prefix) {
    return spends.findByReceiptIdOrderByIdAsc(receiptId).stream()
        .anyMatch(row -> row.getOperationKey().startsWith(prefix));
  }

  /** Fixa um único render no recibo; fases de montagem e revisão reutilizam essa candidata. */
  public void bindRenderJob(Long receiptId, Long cycleId, Long projectId, Long jobId) {
    var old = spends.findByReceiptIdAndOperationKey(receiptId, "RENDER_BINDING");
    String request = "{\"jobId\":" + jobId + "}";
    if (old.isPresent()) {
      require(
          old.get().getRequestJson().equals(request),
          "Este recibo já está vinculado a outro render; produção paralela recusada.");
      return;
    }
    var row = new SingleVideoSpend();
    row.setReceiptId(receiptId);
    row.setCommercialCycleId(cycleId);
    row.setVideoProjectId(projectId);
    row.setOperationKey("RENDER_BINDING");
    row.setRequestJson(request);
    row.setRequestSha256(sha256(request));
    row.setProvider("BACKEND_CONTROL");
    row.setModelCode("NO_EXTERNAL_CALL");
    row.setState("BOUND");
    row.setMaxCostUsd(BigDecimal.ZERO);
    row.setPricingEvidenceJson("{\"externalCalls\":0,\"purpose\":\"SINGLE_RENDER_IDENTITY\"}");
    row.setCreatedAt(Instant.now(clock));
    row.setUpdatedAt(row.getCreatedAt());
    spends.saveAndFlush(row);
  }

  /** Reserva uma operação idêntica sem ampliar o teto, com recusa de alteração e duplicação. */
  public Lease reserve(
      Long receiptId,
      Long commercialCycleId,
      Long projectId,
      String operation,
      String provider,
      String model,
      String request,
      BigDecimal maximum,
      BigDecimal limit,
      BigDecimal outsideLiability,
      String pricingEvidence) {
    String hash = sha256(request);
    var old = spends.findByReceiptIdAndOperationKey(receiptId, operation);
    if (old.isPresent()) {
      require(old.get().getRequestSha256().equals(hash), "Operação já identifica outro request.");
      require(old.get().getVideoProjectId().equals(projectId), "Operação de outro projeto.");
      var row = old.get();
      if ("RESERVED".equals(row.getState())) {
        require(maximum != null && maximum.signum() > 0, "Máximo renovado não comprovado.");
        BigDecimal refreshed = maximum.max(row.getMaxCostUsd());
        require(
            liability(receiptId)
                    .subtract(row.getMaxCostUsd())
                    .add(refreshed)
                    .add(Objects.requireNonNull(outsideLiability))
                    .compareTo(limit)
                <= 0,
            "O teto não cobre a renovação da mesma chamada.");
        // A cotação anterior continua no histórico; nunca se reduz reserva por mera expiração.
        var history = json.read(row.getPricingEvidenceJson());
        row.setPricingEvidenceJson(
            json.write(
                Map.of(
                    "renewedAt",
                    Instant.now(clock).toString(),
                    "previous",
                    history,
                    "current",
                    json.read(pricingEvidence))));
        row.setMaxCostUsd(refreshed);
        row.setUpdatedAt(Instant.now(clock));
        spends.saveAndFlush(row);
      }
      return lease(row, false);
    }
    require(maximum != null && maximum.signum() > 0, "Custo máximo não comprovado.");
    require(outsideLiability != null && outsideLiability.signum() >= 0, "Há custo desconhecido.");
    require(
        liability(receiptId).add(outsideLiability).add(maximum).compareTo(limit) <= 0,
        "O teto integral não cobre esta chamada, custos pendentes e reservas. Nenhum envio foi"
            + " autorizado.");
    var row = new SingleVideoSpend();
    row.setReceiptId(receiptId);
    row.setCommercialCycleId(commercialCycleId);
    row.setVideoProjectId(projectId);
    row.setOperationKey(operation);
    row.setRequestSha256(hash);
    row.setRequestJson(request);
    row.setProvider(provider);
    row.setModelCode(model);
    row.setMaxCostUsd(maximum);
    row.setState("RESERVED");
    row.setPricingEvidenceJson(pricingEvidence);
    row.setCreatedAt(Instant.now(clock));
    row.setUpdatedAt(row.getCreatedAt());
    return lease(spends.saveAndFlush(row), false);
  }

  /**
   * Transfere parte da reserva de clipes para uma chamada concreta, sem reservar o mesmo custo duas
   * vezes.
   */
  public Lease reserveClip(
      Long receiptId,
      Long cycleId,
      Long projectId,
      String envelopeKey,
      String operation,
      String model,
      String request,
      BigDecimal maximum,
      BigDecimal limit,
      BigDecimal outsideLiability,
      String evidence) {
    var prior = spends.findByReceiptIdAndOperationKey(receiptId, operation);
    if (prior.isPresent())
      return reserve(
          receiptId,
          cycleId,
          projectId,
          operation,
          "RUNWAY",
          model,
          request,
          maximum,
          limit,
          outsideLiability,
          evidence);
    var envelope =
        spends
            .findByReceiptIdAndOperationKey(receiptId, envelopeKey)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Reserva de clipes ausente do teto compartilhado."));
    require(Objects.equals(envelope.getVideoProjectId(), projectId), "Reserva de outro projeto.");
    require(
        maximum != null && maximum.signum() > 0 && envelope.getMaxCostUsd().compareTo(maximum) >= 0,
        "A reserva de clipes não cobre esta tomada.");
    require(
        outsideLiability != null
            && liability(receiptId).add(outsideLiability).compareTo(limit) <= 0,
        "Custos ou pendências novos ultrapassam a cobertura do teto integral.");
    envelope.setMaxCostUsd(envelope.getMaxCostUsd().subtract(maximum));
    envelope.setUpdatedAt(Instant.now(clock));
    spends.saveAndFlush(envelope);
    return reserve(
        receiptId,
        cycleId,
        projectId,
        operation,
        "RUNWAY",
        model,
        request,
        maximum,
        limit,
        outsideLiability,
        evidence);
  }

  /** Marca envio antes da rede; somente a primeira transição permite a chamada paga. */
  public Lease begin(Long id, Long receiptId, Long projectId, String operation) {
    var row = owned(id, receiptId, projectId, operation);
    boolean invoke = "RESERVED".equals(row.getState());
    if (invoke) {
      require(
          row.getUpdatedAt().plusSeconds(300).isAfter(Instant.now(clock)),
          "Cotação da chamada venceu; renovar a reserva original antes de enviar.");
      row.setState("SUBMITTED");
      row.setUpdatedAt(Instant.now(clock));
      spends.saveAndFlush(row);
    }
    return lease(row, invoke);
  }

  /**
   * Preserva resposta idempotente sem transformar estimativa, erro ou timeout em saldo liberado.
   */
  public Lease record(Long id, Long receiptId, Long projectId, String operation, Result result) {
    var row = owned(id, receiptId, projectId, operation);
    require(!"RESERVED".equals(row.getState()), "A operação ainda não foi enviada.");
    String raw = result == null ? null : result.rawResponse();
    if (row.getRawResponse() != null) {
      require(Objects.equals(raw, row.getRawResponse()), "Resposta já preservada com outro corpo.");
      return lease(row, false);
    }
    if (raw == null || raw.isBlank()) {
      row.setState("UNCERTAIN");
    } else {
      row.setRawResponse(raw);
      row.setProviderReference(result.providerRequestId());
      row.setState("RESPONSE_RECORDED");
    }
    row.setUpdatedAt(Instant.now(clock));
    return lease(spends.saveAndFlush(row), false);
  }

  /** Recusa identidade cruzada mesmo quando o identificador da reserva é conhecido. */
  private SingleVideoSpend owned(Long id, Long receiptId, Long projectId, String operation) {
    var row = spends.findById(id).orElseThrow();
    require(
        Objects.equals(row.getReceiptId(), receiptId)
            && Objects.equals(row.getVideoProjectId(), projectId)
            && Objects.equals(row.getOperationKey(), operation),
        "Reserva pertence a outro contexto.");
    return row;
  }

  /**
   * Projeta o mesmo corpo persistido e o saldo máximo retido, sem aceitar metadata de aprovação.
   */
  private Lease lease(SingleVideoSpend row, boolean invoke) {
    return new Lease(
        row.getId(),
        row.getReceiptId(),
        row.getState(),
        invoke,
        row.getMaxCostUsd(),
        row.getRequestSha256(),
        json.read(row.getRequestJson()),
        row.getRawResponse(),
        row.getUpdatedAt().plusSeconds(300));
  }

  /** Calcula identidade estável do request sem registrar credenciais ou cabeçalhos. */
  private String sha256(String request) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest(request.getBytes(StandardCharsets.UTF_8)));
    } catch (java.security.NoSuchAlgorithmException ex) {
      log.error("Algoritmo de integridade indisponível no ledger do vídeo", ex);
      throw new AssertionError(ex);
    }
  }

  /** Mantém recusas de contrato explícitas e sem efeito de consumo. */
  private static void require(boolean valid, String reason) {
    if (!valid) throw new IllegalArgumentException(reason);
  }
}

package com.marketinghub.facebookads.service;

import com.marketinghub.repository.jdbc.facebookads.FacebookPixelConversionJdbcRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * Responsabilidade: expor compras autoritativas pendentes e registrar a entrega idempotente de
 * conversões ao pixel da Meta.
 */
@Service
public class FacebookPixelConversionService {

  private final FacebookPixelConversionJdbcRepository repository;

  /** Configura a persistência canônica de conversões sem acesso direto ao banco. */
  public FacebookPixelConversionService(FacebookPixelConversionJdbcRepository repository) {
    this.repository = repository;
  }

  /** Lista compras aprovadas do Lead Portal que ainda não foram enviadas ao pixel. */
  public List<PixelConversion> listApprovedPurchasesPendingPixel(int limit) {
    int safeLimit = Math.max(1, Math.min(limit, 200));
    return repository.findPendingLegacyConversions(safeLimit).stream()
        .map(
            row ->
                new PixelConversion(
                    row.purchaseId(),
                    row.experimentId(),
                    row.experimentName(),
                    row.pixelId(),
                    row.paymentId(),
                    row.amount(),
                    row.currency(),
                    row.paymentApprovedAt(),
                    row.hashedEmail(),
                    row.eventSourceUrl()))
        .toList();
  }

  /** Lista pagamentos PDE reais e aprovados que ainda não foram entregues ao CAPI. */
  public List<PdePixelConversion> listApprovedPdePurchasesPendingPixel(int limit) {
    int safeLimit = Math.max(1, Math.min(limit, 200));
    return repository.findPendingPdeConversions(safeLimit).stream()
        .map(
            row ->
                new PdePixelConversion(
                    row.sourceReference(),
                    row.experimentId(),
                    row.experimentName(),
                    row.pixelId(),
                    row.eventId(),
                    row.amount(),
                    row.currency(),
                    row.paymentApprovedAt(),
                    row.hashedEmail(),
                    row.eventSourceUrl()))
        .toList();
  }

  /** Marca uma compra legada como entregue depois da confirmação da Meta. */
  public void markConversionRecorded(long purchaseId) {
    repository.markLegacyConversionRecorded(purchaseId);
  }

  /** Registra a entrega PDE somente quando a origem, o pixel e o experimento continuam válidos. */
  public void markPdeConversionRecorded(String sourceReference, String pixelId) {
    if (!StringUtils.hasText(sourceReference) || !StringUtils.hasText(pixelId)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Referência e pixel são obrigatórios para confirmar CAPI.");
    }
    String normalizedReference = sourceReference.trim();
    String normalizedPixelId = pixelId.trim();
    int inserted = repository.insertPdeDeliveryIfEligible(normalizedReference, normalizedPixelId);
    if (inserted > 0 || repository.pdeDeliveryExists(normalizedReference, normalizedPixelId)) {
      return;
    }
    throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "Conversão PDE não corresponde a uma compra aprovada e a um pixel vigente.");
  }

  /** Representa uma compra legada pronta para envio pela Conversions API. */
  public record PixelConversion(
      Long purchaseId,
      Long experimentId,
      String experimentName,
      String pixelId,
      String paymentId,
      BigDecimal amount,
      String currency,
      Instant paymentApprovedAt,
      String hashedEmail,
      String eventSourceUrl) {
    /** Normaliza a moeda antes do envio à Meta. */
    public String normalizedCurrency() {
      return StringUtils.hasText(currency) ? currency.trim().toUpperCase() : null;
    }
  }

  /** Representa uma compra PDE pronta para envio idempotente pela Conversions API. */
  public record PdePixelConversion(
      String sourceReference,
      Long experimentId,
      String experimentName,
      String pixelId,
      String eventId,
      BigDecimal amount,
      String currency,
      Instant paymentApprovedAt,
      String hashedEmail,
      String eventSourceUrl) {
    /** Normaliza a moeda antes do envio à Meta. */
    public String normalizedCurrency() {
      return StringUtils.hasText(currency) ? currency.trim().toUpperCase() : null;
    }
  }
}

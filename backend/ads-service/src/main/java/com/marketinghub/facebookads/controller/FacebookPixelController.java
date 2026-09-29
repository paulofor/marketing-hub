package com.marketinghub.facebookads.controller;

import com.marketinghub.facebookads.service.FacebookPixelConversionService;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.dto.MarketNicheDto;
import com.marketinghub.niche.mapper.MarketNicheMapper;
import com.marketinghub.niche.service.MarketNicheService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Agrupa endpoints de pixels e conversões da integração Facebook Ads. */
@RestController
@RequestMapping("/api/facebook-pixels")
public class FacebookPixelController {

  private final MarketNicheService marketNicheService;
  private final MarketNicheMapper marketNicheMapper;
  private final FacebookPixelConversionService conversionService;

  /** Configura os serviços canônicos de pixel e conversão. */
  public FacebookPixelController(
      MarketNicheService marketNicheService,
      MarketNicheMapper marketNicheMapper,
      FacebookPixelConversionService conversionService) {
    this.marketNicheService = marketNicheService;
    this.marketNicheMapper = marketNicheMapper;
    this.conversionService = conversionService;
  }

  /** Lista solicitações de pixel pendentes para o worker oficial. */
  @GetMapping("/pending")
  public List<NichePixelDto> listPendingPixelRequests() {
    return marketNicheService.listPendingPixelRequests().stream()
        .map(niche -> new NichePixelDto(niche.getId(), niche.getName()))
        .toList();
  }

  /** Registra a solicitação manual de criação de pixel para o nicho informado. */
  @PostMapping("/niches/{nicheId}/request")
  public MarketNicheDto requestPixel(@PathVariable("nicheId") Long nicheId) {
    return marketNicheMapper.toDto(marketNicheService.requestFacebookPixel(nicheId));
  }

  /** Registra no nicho o pixel criado pelo Facebook Ads Worker. */
  @PostMapping
  public MarketNicheDto registerPixel(@RequestBody CreatePixelRequest request) {
    MarketNiche niche =
        marketNicheService.attachFacebookPixel(
            request.nicheId(), request.pixelId(), request.pixelCode(), request.createdAt());
    return marketNicheMapper.toDto(niche);
  }

  /** Lista compras legadas do Lead Portal ainda não entregues à Meta. */
  @GetMapping("/conversions-ready")
  public List<PixelConversionDto> listConversionsReady(
      @RequestParam(name = "limit", defaultValue = "50") int limit) {
    return conversionService.listApprovedPurchasesPendingPixel(limit).stream()
        .map(
            conv ->
                new PixelConversionDto(
                    conv.purchaseId(),
                    conv.experimentId(),
                    conv.experimentName(),
                    conv.pixelId(),
                    conv.paymentId(),
                    conv.amount(),
                    conv.normalizedCurrency(),
                    conv.paymentApprovedAt(),
                    conv.hashedEmail(),
                    conv.eventSourceUrl()))
        .toList();
  }

  /** Lista compras autoritativas do PDE ainda não entregues à Meta. */
  @GetMapping("/pde-conversions-ready")
  public List<PdePixelConversionDto> listPdeConversionsReady(
      @RequestParam(name = "limit", defaultValue = "50") int limit) {
    return conversionService.listApprovedPdePurchasesPendingPixel(limit).stream()
        .map(
            conversion ->
                new PdePixelConversionDto(
                    conversion.sourceReference(),
                    conversion.experimentId(),
                    conversion.experimentName(),
                    conversion.pixelId(),
                    conversion.eventId(),
                    conversion.amount(),
                    conversion.normalizedCurrency(),
                    conversion.paymentApprovedAt(),
                    conversion.hashedEmail(),
                    conversion.eventSourceUrl()))
        .toList();
  }

  /** Confirma a entrega de uma conversão legada depois da resposta de sucesso da Meta. */
  @PostMapping("/conversions/{purchaseId}/ack")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void acknowledgeConversion(@PathVariable("purchaseId") long purchaseId) {
    conversionService.markConversionRecorded(purchaseId);
  }

  /** Confirma de forma idempotente a entrega CAPI de um pagamento PDE aprovado. */
  @PostMapping("/pde-conversions/ack")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void acknowledgePdeConversion(@Valid @RequestBody PdeConversionAckRequest request) {
    conversionService.markPdeConversionRecorded(request.sourceReference(), request.pixelId());
  }

  /** Identifica um nicho cuja criação de pixel está pendente. */
  public record NichePixelDto(Long nicheId, String nicheName) {}

  /** Transporta o pixel criado pelo worker para a fonte de verdade do nicho. */
  public record CreatePixelRequest(
      Long nicheId, String pixelId, String pixelCode, Instant createdAt) {}

  /** Transporta uma compra legada pronta para envio pela Conversions API. */
  public record PixelConversionDto(
      Long purchaseId,
      Long experimentId,
      String experimentName,
      String pixelId,
      String paymentId,
      java.math.BigDecimal amount,
      String currency,
      Instant paymentApprovedAt,
      String hashedEmail,
      String eventSourceUrl) {}

  /** Transporta uma compra PDE autoritativa pronta para envio pela Conversions API. */
  public record PdePixelConversionDto(
      String sourceReference,
      Long experimentId,
      String experimentName,
      String pixelId,
      String eventId,
      java.math.BigDecimal amount,
      String currency,
      Instant paymentApprovedAt,
      String hashedEmail,
      String eventSourceUrl) {}

  /** Confirma a referência financeira e o pixel aceitos pela Meta. */
  public record PdeConversionAckRequest(
      @NotBlank String sourceReference, @NotBlank String pixelId) {}
}

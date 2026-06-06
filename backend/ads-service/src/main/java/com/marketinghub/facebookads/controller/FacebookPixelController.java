package com.marketinghub.facebookads.controller;

import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.dto.MarketNicheDto;
import com.marketinghub.niche.mapper.MarketNicheMapper;
import com.marketinghub.niche.service.MarketNicheService;
import com.marketinghub.facebookads.service.FacebookPixelConversionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/**
 * Agrupa endpoints de pixels e conversões da integração Facebook Ads.
 */
@RestController
@RequestMapping("/api/facebook-pixels")
public class FacebookPixelController {

    private final MarketNicheService marketNicheService;
    private final MarketNicheMapper marketNicheMapper;
    private final FacebookPixelConversionService conversionService;

    public FacebookPixelController(MarketNicheService marketNicheService,
                                   MarketNicheMapper marketNicheMapper,
                                   FacebookPixelConversionService conversionService) {
        this.marketNicheService = marketNicheService;
        this.marketNicheMapper = marketNicheMapper;
        this.conversionService = conversionService;
    }

    @GetMapping("/niches-ready")
    // Executa a operação listNichesReadyForPixel da integração Facebook Ads.
    public List<NichePixelDto> listNichesReadyForPixel() {
        return marketNicheService.listReadyForPixel().stream()
                .map(niche -> new NichePixelDto(niche.getId(), niche.getName()))
                .toList();
    }

    @PostMapping
    // Executa a operação registerPixel da integração Facebook Ads.
    public MarketNicheDto registerPixel(@RequestBody CreatePixelRequest request) {
        MarketNiche niche = marketNicheService.attachFacebookPixel(
                request.nicheId(),
                request.pixelId(),
                request.pixelCode(),
                request.createdAt()
        );
        return marketNicheMapper.toDto(niche);
    }

    @GetMapping("/conversions-ready")
    public List<PixelConversionDto> listConversionsReady(
            @RequestParam(name = "limit", defaultValue = "50") int limit) {
        return conversionService.listApprovedPurchasesPendingPixel(limit).stream()
                .map(conv -> new PixelConversionDto(
                        conv.purchaseId(),
                        conv.experimentId(),
                        conv.experimentName(),
                        conv.pixelId(),
                        conv.paymentId(),
                        conv.amount(),
                        conv.normalizedCurrency(),
                        conv.paymentApprovedAt()
                ))
                .toList();
    }

    @PostMapping("/conversions/{purchaseId}/ack")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    // Executa a operação acknowledgeConversion da integração Facebook Ads.
    public void acknowledgeConversion(@PathVariable("purchaseId") long purchaseId) {
        conversionService.markConversionRecorded(purchaseId);
    }

    public record NichePixelDto(Long nicheId, String nicheName) {}
    public record CreatePixelRequest(Long nicheId, String pixelId, String pixelCode, Instant createdAt) {}
    public record PixelConversionDto(
            Long purchaseId,
            Long experimentId,
            String experimentName,
            String pixelId,
            String paymentId,
            java.math.BigDecimal amount,
            String currency,
            Instant paymentApprovedAt
    ) {}
}

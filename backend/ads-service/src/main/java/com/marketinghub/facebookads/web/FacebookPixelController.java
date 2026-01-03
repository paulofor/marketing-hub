package com.marketinghub.facebookads.web;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.dto.ExperimentDto;
import com.marketinghub.experiment.mapper.ExperimentMapper;
import com.marketinghub.experiment.service.ExperimentService;
import com.marketinghub.facebookads.service.FacebookPixelConversionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/facebook-pixels")
public class FacebookPixelController {

    private final ExperimentService experimentService;
    private final ExperimentMapper experimentMapper;
    private final FacebookPixelConversionService conversionService;

    public FacebookPixelController(ExperimentService experimentService,
                                   ExperimentMapper experimentMapper,
                                   FacebookPixelConversionService conversionService) {
        this.experimentService = experimentService;
        this.experimentMapper = experimentMapper;
        this.conversionService = conversionService;
    }

    @GetMapping("/experiments-ready")
    public List<ExperimentPixelDto> listExperimentsReadyForPixel() {
        return experimentService.listReadyForPixel().stream()
                .map(exp -> new ExperimentPixelDto(exp.getId(), exp.getName()))
                .toList();
    }

    @PostMapping
    public ExperimentDto registerPixel(@RequestBody CreatePixelRequest request) {
        Experiment experiment = experimentService.attachFacebookPixel(
                request.experimentId(),
                request.pixelId(),
                request.pixelCode(),
                request.createdAt()
        );
        return experimentMapper.toDto(experiment);
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
    public void acknowledgeConversion(@PathVariable("purchaseId") long purchaseId) {
        conversionService.markConversionRecorded(purchaseId);
    }

    public record ExperimentPixelDto(Long experimentId, String experimentName) {}
    public record CreatePixelRequest(Long experimentId, String pixelId, String pixelCode, Instant createdAt) {}
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

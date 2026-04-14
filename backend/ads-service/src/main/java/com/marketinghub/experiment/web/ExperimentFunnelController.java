package com.marketinghub.experiment.web;

import com.marketinghub.experiment.funnel.ExperimentFunnelDiagnosticService;
import com.marketinghub.experiment.funnel.ExperimentFunnelService;
import com.marketinghub.experiment.funnel.dto.ExperimentFunnelDiagnosticsResponseDto;
import com.marketinghub.experiment.funnel.dto.ExperimentFunnelResetResponse;
import com.marketinghub.experiment.funnel.dto.ExperimentFunnelStageDto;
import com.marketinghub.experiment.funnel.dto.RegisterExperimentFunnelEventRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints para consultar e registrar o funil de vendas de um experimento.
 */
@RestController
@RequestMapping("/api/experiments/{experimentId}/funnel")
@RequiredArgsConstructor
public class ExperimentFunnelController {

    private final ExperimentFunnelService experimentFunnelService;
    private final ExperimentFunnelDiagnosticService experimentFunnelDiagnosticService;

    @GetMapping
    public List<ExperimentFunnelStageDto> summarize(@PathVariable Long experimentId) {
        return experimentFunnelService.summarize(experimentId);
    }


    @GetMapping("/diagnostics")
    public ExperimentFunnelDiagnosticsResponseDto diagnostics(@PathVariable Long experimentId) {
        return experimentFunnelDiagnosticService.diagnose(experimentId);
    }

    @PostMapping("/events")
    public void registerEvent(@PathVariable Long experimentId,
                              @RequestBody RegisterExperimentFunnelEventRequest request) {
        experimentFunnelService.registerEvent(experimentId, request);
    }

    @PostMapping("/reset")
    public ExperimentFunnelResetResponse reset(@PathVariable Long experimentId) {
        return new ExperimentFunnelResetResponse(experimentFunnelService.resetFunnel(experimentId));
    }
}

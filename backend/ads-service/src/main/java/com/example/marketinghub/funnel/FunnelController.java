package com.example.marketinghub.funnel;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST endpoints for managing funnels.
 */
@RestController
@RequestMapping("/api/funnels")
@RequiredArgsConstructor
public class FunnelController {
    private final FunnelService funnelService;

    @GetMapping
    public List<SalesFunnel> listByExperiment(@RequestParam Long experimentId) {
        return funnelService.findByExperiment(experimentId);
    }

    @PostMapping
    public SalesFunnel create(@RequestParam Long experimentId, @RequestBody SalesFunnel funnel) {
        return funnelService.create(experimentId, funnel);
    }

    @PostMapping("/{id}/steps")
    public FunnelStep addStep(@PathVariable UUID id, @RequestBody FunnelStep step) {
        return funnelService.addStep(id, step);
    }

    @GetMapping("/{id}/performance")
    public List<StepMetricSnapshot> performance(@PathVariable UUID id,
                                               @RequestParam(defaultValue = "7d") String window) {
        // Simplified: return latest snapshots
        return funnelService.getSnapshots(id);
    }

    @GetMapping("/best")
    public List<SalesFunnel> best(@RequestParam String metric) {
        return funnelService.findBest(metric);
    }
}

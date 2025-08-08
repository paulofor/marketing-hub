package com.example.marketinghub.funnel;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import com.example.marketinghub.funnel.dto.SalesFunnelDto;

/**
 * REST endpoints for managing funnels.
 */
@RestController
@RequestMapping("/api/funnels")
@RequiredArgsConstructor
public class FunnelController {
    private final FunnelService funnelService;

    @GetMapping
    public List<SalesFunnelDto> list() {
        return funnelService.list();
    }

    @GetMapping("/{id}")
    public SalesFunnel get(@PathVariable UUID id) {
        return funnelService.get(id);
    }

    @PutMapping("/{id}")
    public SalesFunnel update(@PathVariable UUID id, @RequestBody SalesFunnel funnel) {
        return funnelService.update(id, funnel);
    }

    @PostMapping
    public SalesFunnel create(@RequestBody SalesFunnel funnel) {
        return funnelService.create(funnel);
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

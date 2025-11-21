package com.marketinghub.leadportal.web;

import com.marketinghub.leadportal.dto.LeadPortalExperimentMetricsDto;
import com.marketinghub.leadportal.service.LeadPortalMetricsService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints para acompanhar métricas do portal do lead.
 */
@RestController
@RequestMapping("/api/lead-portal/metrics")
public class LeadPortalMetricsController {
    private final LeadPortalMetricsService metricsService;

    public LeadPortalMetricsController(LeadPortalMetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GetMapping("/experiments")
    public List<LeadPortalExperimentMetricsDto> listExperimentMetrics() {
        return metricsService.listExperimentMetrics();
    }
}

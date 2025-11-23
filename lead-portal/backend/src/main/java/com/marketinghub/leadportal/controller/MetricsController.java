package com.marketinghub.leadportal.controller;

import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/lead-portal")
public class MetricsController {

    private final PrometheusMeterRegistry prometheusMeterRegistry;

    public MetricsController(PrometheusMeterRegistry prometheusMeterRegistry) {
        this.prometheusMeterRegistry = prometheusMeterRegistry;
    }

    @GetMapping(value = "/metrics", produces = TextFormat.CONTENT_TYPE_004)
    public String scrape() {
        return prometheusMeterRegistry.scrape();
    }
}

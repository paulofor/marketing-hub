package com.marketinghub.experiment.web;

import com.marketinghub.experiment.*;
import com.marketinghub.experiment.service.LandingService;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.stream.StreamSupport;

@RestController
@RequestMapping("/api/experiments/{experimentId}/landing")
public class LandingPageController {
    private final LandingService service;

    public LandingPageController(LandingService service) {
        this.service = service;
    }

    @PostMapping
    public LandingPage create(@PathVariable Long experimentId,
                              @RequestBody CreateLandingRequest request) throws IOException {
        return service.create(experimentId, request.getType(), request.getHtml());
    }

    @GetMapping
    public List<LandingPage> list(@PathVariable Long experimentId) {
        return StreamSupport.stream(service.listByExperiment(experimentId).spliterator(), false)
                .toList();
    }

    @Data
    public static class CreateLandingRequest {
        private LandingPageType type;
        private String html;
    }
}

package com.marketinghub.marketresearch.controller;

import com.marketinghub.marketresearch.domain.MarketResearchTask;
import com.marketinghub.marketresearch.dto.MarketResearchRequest;
import com.marketinghub.marketresearch.dto.MarketResearchResponse;
import com.marketinghub.marketresearch.service.MarketResearchMapper;
import com.marketinghub.marketresearch.service.MarketResearchService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/market-research")
public class MarketResearchController {

    private final MarketResearchService service;

    public MarketResearchController(MarketResearchService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<MarketResearchResponse> create(@Valid @RequestBody MarketResearchRequest request) {
        MarketResearchTask task = service.execute(request);
        return ResponseEntity.ok(MarketResearchMapper.toResponse(task));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MarketResearchResponse> find(@PathVariable Long id) {
        MarketResearchTask task = service.findOrThrow(id);
        return ResponseEntity.ok(MarketResearchMapper.toResponse(task));
    }
}

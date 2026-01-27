package com.marketinghub.niche.web;

import com.marketinghub.niche.dto.CreateMarketNicheRequest;
import com.marketinghub.niche.dto.MarketNicheDto;
import com.marketinghub.niche.mapper.MarketNicheMapper;
import com.marketinghub.niche.service.MarketNicheService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.StreamSupport;

/**
 * REST controller for market niches.
 */
@RestController
@RequestMapping("/api/niches")
public class MarketNicheController {
    private final MarketNicheService service;
    private final MarketNicheMapper mapper;

    public MarketNicheController(MarketNicheService service, MarketNicheMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    public MarketNicheDto create(@RequestBody CreateMarketNicheRequest request) {
        return mapper.toDto(service.create(request));
    }

    @GetMapping("/{id}")
    public MarketNicheDto get(@PathVariable Long id) {
        return mapper.toDto(service.get(id));
    }

    @PutMapping("/{id}")
    public MarketNicheDto update(@PathVariable Long id, @RequestBody CreateMarketNicheRequest request) {
        return mapper.toDto(service.update(id, request));
    }

    @PatchMapping("/{id}/interests-to-generate")
    public MarketNicheDto requestInterests(@PathVariable Long id,
                                            @RequestParam("quantity") int quantity,
                                            @RequestParam(value = "model", required = false) String model) {
        return mapper.toDto(service.requestInterests(id, quantity, model));
    }

    @PatchMapping("/{id}/job-titles-to-generate")
    public MarketNicheDto requestJobTitles(@PathVariable Long id,
                                            @RequestParam("quantity") int quantity,
                                            @RequestParam(value = "model", required = false) String model) {
        return mapper.toDto(service.requestJobTitles(id, quantity, model));
    }

    @PatchMapping("/{id}/behaviors-to-generate")
    public MarketNicheDto requestBehaviors(@PathVariable Long id,
                                            @RequestParam("quantity") int quantity,
                                            @RequestParam(value = "model", required = false) String model) {
        return mapper.toDto(service.requestBehaviors(id, quantity, model));
    }

    @PatchMapping("/{id}/hypotheses-to-generate")
    public MarketNicheDto requestHypotheses(@PathVariable Long id,
                                            @RequestParam("quantity") int quantity,
                                            @RequestParam(value = "model", required = false) String model,
                                            @RequestParam(value = "differentiatedTechnologyId", required = false)
                                            Long differentiatedTechnologyId,
                                            @RequestParam(value = "detailedDescriptionId", required = false)
                                            Long detailedDescriptionId) {
        return mapper.toDto(service.requestHypotheses(
                id,
                quantity,
                model,
                differentiatedTechnologyId,
                detailedDescriptionId));
    }

    @PatchMapping("/{id}/detailed-descriptions-to-generate")
    public MarketNicheDto requestDetailedDescriptions(@PathVariable Long id,
                                                      @RequestParam("quantity") int quantity,
                                                      @RequestParam(value = "model", required = false) String model) {
        return mapper.toDto(service.requestDetailedDescriptions(id, quantity, model));
    }

    @GetMapping
    public List<MarketNicheDto> list() {
        return StreamSupport.stream(service.list().spliterator(), false)
                .map(mapper::toDto)
                .toList();
    }
}

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

    @GetMapping
    public List<MarketNicheDto> list() {
        return StreamSupport.stream(service.list().spliterator(), false)
                .map(mapper::toDto)
                .toList();
    }
}

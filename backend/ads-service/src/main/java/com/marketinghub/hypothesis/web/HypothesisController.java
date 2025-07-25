package com.marketinghub.hypothesis.web;

import com.marketinghub.hypothesis.HypothesisStatus;
import com.marketinghub.hypothesis.dto.CreateHypothesisRequest;
import com.marketinghub.hypothesis.dto.HypothesisDto;
import com.marketinghub.hypothesis.mapper.HypothesisMapper;
import com.marketinghub.hypothesis.service.HypothesisKanbanFacade;
import com.marketinghub.hypothesis.service.HypothesisService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.StreamSupport;

@RestController
@RequestMapping("/api")
public class HypothesisController {
    private final HypothesisService service;
    private final HypothesisMapper mapper;
    private final HypothesisKanbanFacade facade;

    public HypothesisController(HypothesisService service, HypothesisMapper mapper, HypothesisKanbanFacade facade) {
        this.service = service;
        this.mapper = mapper;
        this.facade = facade;
    }

    @PostMapping("/hypotheses")
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public HypothesisDto create(@RequestBody CreateHypothesisRequest req) {
        return mapper.toDto(service.create(req));
    }

    @GetMapping("/hypotheses")
    public List<HypothesisDto> listAll(@RequestParam(value = "status", required = false) HypothesisStatus status) {
        return StreamSupport.stream(service.list(status).spliterator(), false)
                .map(mapper::toDto)
                .toList();
    }

    @GetMapping("/niches/{nicheId}/hypotheses")
    public List<HypothesisDto> listByNiche(@PathVariable Long nicheId,
                                           @RequestParam(value = "status", required = false) HypothesisStatus status) {
        return StreamSupport.stream(service.listByMarketNiche(nicheId, status).spliterator(), false)
                .map(mapper::toDto)
                .toList();
    }

    @PatchMapping("/hypotheses/{id}/status")
    public HypothesisDto patchStatus(@PathVariable UUID id, @RequestBody Map<String, HypothesisStatus> body) {
        HypothesisStatus status = body.get("status");
        return mapper.toDto(service.updateStatus(id, status));
    }

    @GetMapping("/niches/{nicheId}/hypotheses/board")
    public Map<HypothesisStatus, List<HypothesisDto>> board(@PathVariable Long nicheId) {
        return facade.board(nicheId);
    }
}

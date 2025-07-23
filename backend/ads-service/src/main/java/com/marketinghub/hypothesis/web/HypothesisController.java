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

    @PostMapping("/experiments/{experimentId}/hypotheses")
    public HypothesisDto create(@PathVariable Long experimentId, @RequestBody CreateHypothesisRequest req) {
        return mapper.toDto(service.create(experimentId, req));
    }

    @GetMapping("/experiments/{experimentId}/hypotheses")
    public List<HypothesisDto> list(@PathVariable Long experimentId,
                                    @RequestParam(value = "status", required = false) HypothesisStatus status) {
        return StreamSupport.stream(service.listByExperiment(experimentId, status).spliterator(), false)
                .map(mapper::toDto)
                .toList();
    }

    @PatchMapping("/hypotheses/{id}/status")
    public HypothesisDto patchStatus(@PathVariable Long id, @RequestParam HypothesisStatus status) {
        return mapper.toDto(service.updateStatus(id, status));
    }

    @GetMapping("/experiments/{experimentId}/hypotheses/board")
    public Map<HypothesisStatus, List<HypothesisDto>> board(@PathVariable Long experimentId) {
        return facade.board(experimentId);
    }
}

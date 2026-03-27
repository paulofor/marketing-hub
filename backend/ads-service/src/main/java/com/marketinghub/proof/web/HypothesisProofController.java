package com.marketinghub.proof.web;

import com.marketinghub.proof.dto.CreateProofArtifactRequest;
import com.marketinghub.proof.dto.ProofArtifactDto;
import com.marketinghub.proof.mapper.ProofArtifactMapper;
import com.marketinghub.proof.service.ProofArtifactService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hypotheses/{hypothesisId}/proofs")
public class HypothesisProofController {
    private final ProofArtifactService service;
    private final ProofArtifactMapper mapper;

    public HypothesisProofController(ProofArtifactService service, ProofArtifactMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    public List<ProofArtifactDto> list(@PathVariable UUID hypothesisId) {
        return service.listByHypothesis(hypothesisId).stream().map(mapper::toDto).toList();
    }

    @PostMapping
    public ProofArtifactDto create(@PathVariable UUID hypothesisId,
                                   @RequestBody CreateProofArtifactRequest request) {
        return mapper.toDto(service.createForHypothesis(hypothesisId, request));
    }
}

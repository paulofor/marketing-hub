package com.marketinghub.proof.web;

import com.marketinghub.proof.dto.CreateProofArtifactRequest;
import com.marketinghub.proof.dto.ProofArtifactDto;
import com.marketinghub.proof.mapper.ProofArtifactMapper;
import com.marketinghub.proof.service.ProofArtifactService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/experiments/{experimentId}/proofs")
public class ExperimentProofController {
    private final ProofArtifactService service;
    private final ProofArtifactMapper mapper;

    public ExperimentProofController(ProofArtifactService service, ProofArtifactMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    public List<ProofArtifactDto> list(@PathVariable Long experimentId) {
        return service.listByExperiment(experimentId).stream().map(mapper::toDto).toList();
    }

    @PostMapping
    public ProofArtifactDto create(@PathVariable Long experimentId,
                                   @RequestBody CreateProofArtifactRequest request) {
        return mapper.toDto(service.createForExperiment(experimentId, request));
    }
}

package com.marketinghub.proof.web;

import com.marketinghub.proof.dto.ProofArtifactDto;
import com.marketinghub.proof.dto.UpdateProofArtifactRequest;
import com.marketinghub.proof.mapper.ProofArtifactMapper;
import com.marketinghub.proof.service.ProofArtifactService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/proofs")
public class ProofArtifactController {
    private final ProofArtifactService service;
    private final ProofArtifactMapper mapper;

    public ProofArtifactController(ProofArtifactService service, ProofArtifactMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PutMapping("/{id}")
    public ProofArtifactDto update(@PathVariable Long id, @RequestBody UpdateProofArtifactRequest request) {
        return mapper.toDto(service.update(id, request));
    }
}

package com.marketinghub.creative.label.service;

import com.marketinghub.creative.label.VisualProof;
import com.marketinghub.creative.label.dto.CreateVisualProofRequest;
import com.marketinghub.creative.label.repository.VisualProofRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VisualProofService {
    private final VisualProofRepository repository;

    public VisualProofService(VisualProofRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public VisualProof create(CreateVisualProofRequest request) {
        VisualProof proof = VisualProof.builder()
                .name(request.getName())
                .proofType(request.getProofType())
                .description(request.getDescription())
                .build();
        return repository.save(proof);
    }

    public VisualProof get(Long id) {
        return repository.findById(id).orElseThrow();
    }

    public Iterable<VisualProof> list() {
        return repository.findAll();
    }
}

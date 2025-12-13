package com.marketinghub.microservice.service;

import com.marketinghub.microservice.Microservice;
import com.marketinghub.microservice.dto.CreateMicroserviceRequest;
import com.marketinghub.microservice.repository.MicroserviceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service layer for microservice registry management.
 */
@Service
public class MicroserviceService {
    private final MicroserviceRepository repository;

    public MicroserviceService(MicroserviceRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Microservice create(CreateMicroserviceRequest request) {
        Microservice microservice = new Microservice();
        applyRequest(microservice, request);
        return repository.save(microservice);
    }

    public Microservice get(Long id) {
        return repository.findById(id).orElseThrow();
    }

    public Iterable<Microservice> list() {
        return repository.findAll();
    }

    @Transactional
    public Microservice update(Long id, CreateMicroserviceRequest request) {
        Microservice microservice = repository.findById(id).orElseThrow();
        applyRequest(microservice, request);
        return repository.save(microservice);
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    private void applyRequest(Microservice microservice, CreateMicroserviceRequest request) {
        microservice.setName(request.getName());
        microservice.setDescription(request.getDescription());
        microservice.setBaseUrl(request.getBaseUrl());
        microservice.setCategory(request.getCategory());
        microservice.setStatus(Optional.ofNullable(request.getStatus()).orElse("ACTIVE"));
        microservice.setOwner(request.getOwner());
        microservice.setDocumentationUrl(request.getDocumentationUrl());
        microservice.setHealthCheckPath(request.getHealthCheckPath());
    }
}

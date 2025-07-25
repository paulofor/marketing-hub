package com.marketinghub.experiment.service;

import com.marketinghub.experiment.*;
import com.marketinghub.experiment.dto.CreateCreativeRequest;
import com.marketinghub.experiment.repository.CreativeVariantRepository;
import com.marketinghub.experiment.repository.ExperimentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer for creative variants.
 */
@Service
public class CreativeVariantService {
    private final CreativeVariantRepository repository;
    private final ExperimentRepository experimentRepository;

    public CreativeVariantService(CreativeVariantRepository repository, ExperimentRepository experimentRepository) {
        this.repository = repository;
        this.experimentRepository = experimentRepository;
    }

    /**
     * Creates and stores a creative variant.
     */
    @Transactional
    public CreativeVariant create(CreateCreativeRequest request) {
        Experiment exp = experimentRepository.findById(request.getExperimentId()).orElseThrow();
        if (exp.getStatus() == ExperimentStatus.FINISHED || exp.getStatus() == ExperimentStatus.FAILED) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "experiment not active");
        }
        CreativeVariant creative = CreativeVariant.builder()
                .experiment(exp)
                .type(request.getType())
                .assetUrl(request.getAssetUrl())
                .titles(request.getTitles())
                .descriptions(request.getDescriptions())
                .build();
        return repository.save(creative);
    }

    public Iterable<CreativeVariant> listByExperiment(Long experimentId) {
        Experiment exp = experimentRepository.findById(experimentId).orElseThrow();
        return repository.findAll().stream().filter(c -> c.getExperiment().equals(exp)).toList();
    }
}

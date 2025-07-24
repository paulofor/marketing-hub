package com.marketinghub.experiment.service;

import com.marketinghub.experiment.*;
import com.marketinghub.experiment.dto.CreateAdSetRequest;
import com.marketinghub.experiment.repository.AdSetRepository;
import com.marketinghub.experiment.repository.ExperimentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer for ad sets.
 */
@Service
public class AdSetService {
    private final AdSetRepository repository;
    private final ExperimentRepository experimentRepository;

    public AdSetService(AdSetRepository repository, ExperimentRepository experimentRepository) {
        this.repository = repository;
        this.experimentRepository = experimentRepository;
    }

    /**
     * Creates and stores an ad set.
     */
    @Transactional
    public AdSet create(CreateAdSetRequest request) {
        Experiment exp = experimentRepository.findById(request.getExperimentId()).orElseThrow();
        if (exp.getStatus() == ExperimentStatus.FINISHED || exp.getStatus() == ExperimentStatus.FAILED) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "experiment not active");
        }
        AdSet adSet = AdSet.builder()
                .experiment(exp)
                .location(request.getLocation())
                .interests(request.getInterests())
                .lookalikes(request.getLookalikes())
                .budget(request.getBudget())
                .durationDays(request.getDurationDays())
                .build();
        return repository.save(adSet);
    }

    public Iterable<AdSet> listByExperiment(Long experimentId) {
        Experiment exp = experimentRepository.findById(experimentId).orElseThrow();
        return repository.findAll().stream().filter(a -> a.getExperiment().equals(exp)).toList();
    }
}

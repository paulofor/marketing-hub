package com.marketinghub.experiment.service;

import com.marketinghub.experiment.*;
import com.marketinghub.experiment.repository.LandingPageRepository;
import com.marketinghub.experiment.repository.ExperimentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Service para gerenciamento de landing pages.
 */
@Service
@RequiredArgsConstructor
public class LandingService {
    private final LandingPageRepository repository;
    private final ExperimentRepository experimentRepository;

    @Transactional
    public LandingPage create(Long experimentId, LandingPageType type, String html) throws IOException {
        Experiment exp = experimentRepository.findById(experimentId).orElseThrow();
        String slug = UUID.randomUUID().toString().substring(0, 8);
        Path dir = Path.of("landings");
        Files.createDirectories(dir);
        Path file = dir.resolve(slug + ".html");
        Files.writeString(file, html);
        LandingPage lp = LandingPage.builder()
                .experiment(exp)
                .url("/landings/" + file.getFileName())
                .type(type)
                .status(LandingPageStatus.DRAFT)
                .build();
        return repository.save(lp);
    }

    public Iterable<LandingPage> listByExperiment(Long experimentId) {
        return repository.findByExperimentId(experimentId);
    }
}

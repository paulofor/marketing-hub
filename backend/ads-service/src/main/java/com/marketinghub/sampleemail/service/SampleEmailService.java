package com.marketinghub.sampleemail.service;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.repository.ExperimentRepository;
import com.marketinghub.sampleemail.SampleEmail;
import com.marketinghub.sampleemail.dto.CreateSampleEmailRequest;
import com.marketinghub.sampleemail.repository.SampleEmailRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Serviço para persistir e consultar e-mails de amostra gerados.
 */
@Service
public class SampleEmailService {
    private final SampleEmailRepository repository;
    private final ExperimentRepository experimentRepository;

    public SampleEmailService(SampleEmailRepository repository, ExperimentRepository experimentRepository) {
        this.repository = repository;
        this.experimentRepository = experimentRepository;
    }

    @Transactional(readOnly = true)
    public List<SampleEmail> listByExperiment(Long experimentId) {
        return repository.findByExperimentIdOrderByCreatedAtDesc(experimentId);
    }

    @Transactional
    public SampleEmail create(Long experimentId, CreateSampleEmailRequest request) {
        Experiment experiment = experimentRepository.findById(experimentId).orElseThrow();
        return create(experiment, request);
    }

    @Transactional
    public SampleEmail create(Experiment experiment, CreateSampleEmailRequest request) {
        SampleEmail email = SampleEmail.builder()
                .experiment(experiment)
                .subject(sanitize(request.getSubject()))
                .previewText(sanitize(request.getPreviewText()))
                .body(request.getBody())
                .callToAction(sanitize(request.getCallToAction()))
                .model(sanitize(request.getModel()))
                .prompt(request.getPrompt())
                .build();
        return repository.save(email);
    }

    private String sanitize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}

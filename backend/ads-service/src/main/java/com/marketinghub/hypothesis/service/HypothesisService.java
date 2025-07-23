package com.marketinghub.hypothesis.service;

import com.marketinghub.creative.label.repository.AngleRepository;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.repository.ExperimentRepository;
import com.marketinghub.hypothesis.*;
import com.marketinghub.hypothesis.dto.CreateHypothesisRequest;
import com.marketinghub.hypothesis.repository.HypothesisRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class HypothesisService {
    private final HypothesisRepository repository;
    private final ExperimentRepository experimentRepository;
    private final AngleRepository angleRepository;
    private final EntityManager em;

    public HypothesisService(HypothesisRepository repository,
                             ExperimentRepository experimentRepository,
                             AngleRepository angleRepository,
                             EntityManager em) {
        this.repository = repository;
        this.experimentRepository = experimentRepository;
        this.angleRepository = angleRepository;
        this.em = em;
    }

    private Experiment attachExperiment(Long id) {
        if (!experimentRepository.existsById(id)) {
            throw new EntityNotFoundException("Experiment not found: " + id);
        }
        return em.getReference(Experiment.class, id);
    }

    private Angle attachAngle(Long id) {
        if (id == null) return null;
        if (!angleRepository.existsById(id)) {
            throw new EntityNotFoundException("Angle not found: " + id);
        }
        return em.getReference(Angle.class, id);
    }

    @Transactional
    public Hypothesis create(Long experimentId, CreateHypothesisRequest req) {
        if (req.getTitle() == null || req.getTitle().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "title required");
        }
        Hypothesis h = Hypothesis.builder()
                .experiment(attachExperiment(experimentId))
                .title(req.getTitle())
                .premiseAngle(attachAngle(req.getPremiseAngleId()))
                .offerType(req.getOfferType() == null ? null : OfferType.valueOf(req.getOfferType()))
                .kpiTargetCpl(req.getKpiTargetCpl())
                .status(HypothesisStatus.BACKLOG)
                .build();
        return repository.save(h);
    }

    public Iterable<Hypothesis> listByExperiment(Long experimentId) {
        return repository.findByExperimentId(experimentId);
    }

    @Transactional
    public Hypothesis updateStatus(Long id, HypothesisStatus status) {
        Hypothesis h = repository.findById(id).orElseThrow();
        h.setStatus(status);
        return h;
    }
}

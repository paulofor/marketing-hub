package com.marketinghub.deliverable.service;

import com.marketinghub.deliverable.Deliverable;
import com.marketinghub.deliverable.DeliverablePackage;
import com.marketinghub.deliverable.dto.CreateDeliverablePackageRequest;
import com.marketinghub.deliverable.dto.UpdateDeliverablePackageRequest;
import com.marketinghub.deliverable.repository.DeliverablePackageRepository;
import com.marketinghub.deliverable.repository.DeliverableRepository;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.repository.ExperimentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Coordinates creation and updates of {@link DeliverablePackage} instances.
 */
@Service
public class DeliverablePackageService {
    private final DeliverablePackageRepository repository;
    private final ExperimentRepository experimentRepository;
    private final DeliverableRepository deliverableRepository;

    public DeliverablePackageService(DeliverablePackageRepository repository,
                                     ExperimentRepository experimentRepository,
                                     DeliverableRepository deliverableRepository) {
        this.repository = repository;
        this.experimentRepository = experimentRepository;
        this.deliverableRepository = deliverableRepository;
    }

    public List<DeliverablePackage> listAll() {
        return repository.findAllLatestFirst();
    }

    public DeliverablePackage get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Deliverable package not found: " + id));
    }

    public List<DeliverablePackage> listByExperiment(Long experimentId) {
        if (!experimentRepository.existsById(experimentId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Experiment not found: " + experimentId);
        }
        return repository.findByExperimentIdOrderByCreatedAtDesc(experimentId);
    }

    @Transactional
    public DeliverablePackage create(CreateDeliverablePackageRequest request) {
        if (request.getExperimentId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "experimentId is required");
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
        }
        if (!StringUtils.hasText(request.getPrompt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "prompt is required");
        }
        Experiment experiment = experimentRepository.findById(request.getExperimentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Experiment not found: " + request.getExperimentId()));
        Set<Deliverable> deliverables = resolveDeliverables(request.getDeliverableIds(), experiment);
        DeliverablePackage pack = DeliverablePackage.builder()
                .experiment(experiment)
                .name(request.getName().trim())
                .description(StringUtils.hasText(request.getDescription()) ? request.getDescription() : null)
                .model(StringUtils.hasText(request.getModel()) ? request.getModel() : null)
                .prompt(request.getPrompt())
                .deliverables(deliverables)
                .build();
        return repository.save(pack);
    }

    @Transactional
    public DeliverablePackage update(Long id, UpdateDeliverablePackageRequest request) {
        DeliverablePackage pack = get(id);
        if (StringUtils.hasText(request.getName())) {
            pack.setName(request.getName().trim());
        }
        if (!StringUtils.hasText(request.getPrompt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "prompt is required");
        }
        pack.setPrompt(request.getPrompt());
        pack.setDescription(StringUtils.hasText(request.getDescription()) ? request.getDescription() : null);
        pack.setModel(StringUtils.hasText(request.getModel()) ? request.getModel() : null);
        pack.setDeliverables(resolveDeliverables(request.getDeliverableIds(), pack.getExperiment()));
        return repository.save(pack);
    }

    private LinkedHashSet<Deliverable> resolveDeliverables(List<Long> deliverableIds, Experiment experiment) {
        if (CollectionUtils.isEmpty(deliverableIds)) {
            return new LinkedHashSet<>();
        }
        Map<Long, Integer> order = new LinkedHashMap<>();
        for (int i = 0; i < deliverableIds.size(); i++) {
            Long id = deliverableIds.get(i);
            if (id == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "deliverableIds cannot contain null");
            }
            order.putIfAbsent(id, i);
        }
        List<Deliverable> found = deliverableRepository.findAllById(order.keySet());
        if (found.size() != order.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "One or more deliverables were not found");
        }
        for (Deliverable deliverable : found) {
            if (!deliverable.getNiche().getId().equals(experiment.getNiche().getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Deliverable " + deliverable.getId() + " does not belong to experiment niche");
            }
        }
        found.sort((a, b) -> Integer.compare(order.get(a.getId()), order.get(b.getId())));
        return new LinkedHashSet<>(found);
    }
}

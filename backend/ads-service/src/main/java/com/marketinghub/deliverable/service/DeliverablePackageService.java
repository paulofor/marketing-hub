package com.marketinghub.deliverable.service;

import com.marketinghub.deliverable.Deliverable;
import com.marketinghub.deliverable.DeliverablePackage;
import com.marketinghub.deliverable.dto.CreateDeliverablePackageRequest;
import com.marketinghub.deliverable.dto.UpdateDeliverablePackageRequest;
import com.marketinghub.deliverable.repository.DeliverablePackageRepository;
import com.marketinghub.deliverable.repository.DeliverableRepository;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.repository.ExperimentRepository;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.hypothesis.repository.HypothesisRepository;
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
import java.util.UUID;

/**
 * Coordinates creation and updates of {@link DeliverablePackage} instances.
 */
@Service
public class DeliverablePackageService {
    private final DeliverablePackageRepository repository;
    private final ExperimentRepository experimentRepository;
    private final HypothesisRepository hypothesisRepository;
    private final DeliverableRepository deliverableRepository;

    public DeliverablePackageService(DeliverablePackageRepository repository,
                                     ExperimentRepository experimentRepository,
                                     HypothesisRepository hypothesisRepository,
                                     DeliverableRepository deliverableRepository) {
        this.repository = repository;
        this.experimentRepository = experimentRepository;
        this.hypothesisRepository = hypothesisRepository;
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

    public List<DeliverablePackage> listByHypothesis(UUID hypothesisId) {
        if (!hypothesisRepository.existsById(hypothesisId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Hypothesis not found: " + hypothesisId);
        }
        LinkedHashMap<Long, DeliverablePackage> ordered = new LinkedHashMap<>();
        repository.findByHypothesisIdOrderByCreatedAtDesc(hypothesisId)
                .forEach(pkg -> ordered.put(pkg.getId(), pkg));
        repository.findByExperimentHypothesisRefIdOrderByCreatedAtDesc(hypothesisId)
                .forEach(pkg -> ordered.putIfAbsent(pkg.getId(), pkg));
        return List.copyOf(ordered.values());
    }

    @Transactional
    public DeliverablePackage create(CreateDeliverablePackageRequest request) {
        boolean hasExperiment = request.getExperimentId() != null;
        boolean hasHypothesis = request.getHypothesisId() != null;
        if (hasExperiment == hasHypothesis) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Either experimentId or hypothesisId must be informed");
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
        }
        if (!StringUtils.hasText(request.getPrompt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "prompt is required");
        }

        Experiment experiment = null;
        Hypothesis hypothesis = null;
        Long nicheId;
        if (hasExperiment) {
            experiment = experimentRepository.findById(request.getExperimentId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Experiment not found: " + request.getExperimentId()));
            nicheId = experiment.getNiche() != null ? experiment.getNiche().getId() : null;
        } else {
            hypothesis = hypothesisRepository.findById(request.getHypothesisId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Hypothesis not found: " + request.getHypothesisId()));
            if (hypothesis.getMarketNiche() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Hypothesis is not linked to a market niche");
            }
            nicheId = hypothesis.getMarketNiche().getId();
        }

        Set<Deliverable> deliverables = resolveDeliverables(request.getDeliverableIds(), nicheId);
        DeliverablePackage pack = DeliverablePackage.builder()
                .experiment(experiment)
                .hypothesis(hypothesis)
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
        Long nicheId = resolveNicheId(pack);
        pack.setDeliverables(resolveDeliverables(request.getDeliverableIds(), nicheId));
        return repository.save(pack);
    }

    private Long resolveNicheId(DeliverablePackage pack) {
        if (pack.getExperiment() != null && pack.getExperiment().getNiche() != null) {
            return pack.getExperiment().getNiche().getId();
        }
        if (pack.getHypothesis() != null && pack.getHypothesis().getMarketNiche() != null) {
            return pack.getHypothesis().getMarketNiche().getId();
        }
        return null;
    }

    private LinkedHashSet<Deliverable> resolveDeliverables(List<Long> deliverableIds, Long nicheId) {
        if (CollectionUtils.isEmpty(deliverableIds)) {
            return new LinkedHashSet<>();
        }
        if (nicheId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot resolve niche to validate deliverables");
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
            if (deliverable.getNiche() == null || deliverable.getNiche().getId() == null
                    || !deliverable.getNiche().getId().equals(nicheId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Deliverable " + deliverable.getId() + " does not belong to the required niche");
            }
        }
        found.sort((a, b) -> Integer.compare(order.get(a.getId()), order.get(b.getId())));
        return new LinkedHashSet<>(found);
    }
}

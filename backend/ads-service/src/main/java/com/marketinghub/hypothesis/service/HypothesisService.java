package com.marketinghub.hypothesis.service;

import com.marketinghub.creative.label.Angle;
import com.marketinghub.creative.label.repository.AngleRepository;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.repository.MarketNicheRepository;
import com.marketinghub.hypothesis.*;
import com.marketinghub.hypothesis.dto.CreateHypothesisRequest;
import com.marketinghub.hypothesis.dto.UpdateHypothesisRequest;
import com.marketinghub.hypothesis.repository.HypothesisRepository;
import com.marketinghub.prompt.PromptAttributeDescription;
import com.marketinghub.prompt.repository.PromptAttributeDescriptionRepository;
import java.math.BigDecimal;
import java.util.UUID;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class HypothesisService {
    private final HypothesisRepository repository;
    private final MarketNicheRepository nicheRepository;
    private final AngleRepository angleRepository;
    private final PromptAttributeDescriptionRepository descriptionRepository;
    private final EntityManager em;

    public HypothesisService(HypothesisRepository repository,
                             MarketNicheRepository nicheRepository,
                             AngleRepository angleRepository,
                             PromptAttributeDescriptionRepository descriptionRepository,
                             EntityManager em) {
        this.repository = repository;
        this.nicheRepository = nicheRepository;
        this.angleRepository = angleRepository;
        this.descriptionRepository = descriptionRepository;
        this.em = em;
    }

    private MarketNiche attachNiche(Long id) {
        if (id == null) return null;
        if (!nicheRepository.existsById(id)) {
            throw new EntityNotFoundException("MarketNiche not found: " + id);
        }
        return em.getReference(MarketNiche.class, id);
    }

    private Angle attachAngle(Long id) {
        if (id == null) return null;
        if (!angleRepository.existsById(id)) {
            throw new EntityNotFoundException("Angle not found: " + id);
        }
        return em.getReference(Angle.class, id);
    }

    private void validate(CreateHypothesisRequest req) {
        if (req.getTitle() == null || req.getTitle().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "title required");
        }
        // only title is required
    }

    private BigDecimal resolveTotalCostDelta(CreateHypothesisRequest req) {
        if (req.getCost() != null) {
            return req.getCost();
        }
        return req.getCostUsd();
    }

    private Set<PromptAttributeDescription> attachPromptAttributeDescriptions(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return new HashSet<>();
        Set<PromptAttributeDescription> set = new HashSet<>();
        for (Long id : ids) {
            if (!descriptionRepository.existsById(id)) {
                throw new EntityNotFoundException("PromptAttributeDescription not found: " + id);
            }
            set.add(em.getReference(PromptAttributeDescription.class, id));
        }
        return set;
    }

    @Transactional
    public Hypothesis create(CreateHypothesisRequest req) {
        validate(req);
        Hypothesis h = Hypothesis.builder()
                .marketNiche(attachNiche(req.getMarketNicheId()))
                .title(req.getTitle())
                .premiseAngle(attachAngle(req.getPremiseAngleId()))
                .promise(req.getPromise())
                .problem(req.getProblem())
                .persona(req.getPersona())
                .mechanism(req.getMechanism())
                .uniqueMechanism(req.getUniqueMechanism())
                .entrega(req.getEntrega())
                .successRule(req.getSuccessRule())
                .prompt(req.getPrompt())
                .model(req.getModel())
                .costUsd(req.getCostUsd())
                .cost(req.getCost())
                .expense(req.getExpense())
                .promptAttributeDescriptions(attachPromptAttributeDescriptions(req.getPromptAttributeDescriptionIds()))
                .generatedAt(Instant.now())
                .offerType(req.getOfferType() == null ? null : OfferType.valueOf(req.getOfferType()))
                .price(req.getPrice())
                .kpiTargetCpl(req.getKpiTargetCpl())
                .build();
        Hypothesis saved = repository.save(h);
        BigDecimal delta = resolveTotalCostDelta(req);
        if (delta != null && saved.getMarketNiche() != null && saved.getMarketNiche().getId() != null) {
            nicheRepository.incrementTotalCost(saved.getMarketNiche().getId(), delta);
        }
        return saved;
    }

    public Iterable<Hypothesis> listByMarketNiche(Long marketNicheId, HypothesisStatus status) {
        if (status == null) {
            return repository.findByMarketNicheId(marketNicheId);
        }
        return repository.findByMarketNicheIdAndStatus(marketNicheId, status);
    }

    public Iterable<Hypothesis> list(HypothesisStatus status) {
        if (status == null) {
            return repository.findAll();
        }
        return repository.findByStatus(status);
    }

    @Transactional
    public Hypothesis updateStatus(UUID id, HypothesisStatus status) {
        Hypothesis h = repository.findById(id).orElseThrow();
        h.setStatus(status);
        return h;
    }

    private void validate(UpdateHypothesisRequest req) {
        if (req.getTitle() == null || req.getTitle().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "title required");
        }
    }

    @Transactional
    public Hypothesis update(UUID id, UpdateHypothesisRequest req) {
        validate(req);
        Hypothesis h = repository.findById(id).orElseThrow();
        if (h.getStatus() != HypothesisStatus.BACKLOG) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "only BACKLOG hypotheses can be edited");
        }
        h.setTitle(req.getTitle());
        h.setPremiseAngle(attachAngle(req.getPremiseAngleId()));
        h.setPromise(req.getPromise());
        h.setProblem(req.getProblem());
        h.setPersona(req.getPersona());
        h.setMechanism(req.getMechanism());
        h.setUniqueMechanism(req.getUniqueMechanism());
        h.setEntrega(req.getEntrega());
        h.setSuccessRule(req.getSuccessRule());
        h.setCost(req.getCost());
        h.setExpense(req.getExpense());
        h.setOfferType(req.getOfferType() == null ? null : OfferType.valueOf(req.getOfferType()));
        h.setPrice(req.getPrice());
        h.setKpiTargetCpl(req.getKpiTargetCpl());
        return h;
    }
}

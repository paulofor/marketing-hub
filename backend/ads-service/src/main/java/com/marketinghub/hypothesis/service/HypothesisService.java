package com.marketinghub.hypothesis.service;

import com.marketinghub.creative.label.Angle;
import com.marketinghub.creative.label.repository.AngleRepository;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.repository.MarketNicheRepository;
import com.marketinghub.hypothesis.*;
import com.marketinghub.hypothesis.dto.CreateHypothesisRequest;
import com.marketinghub.hypothesis.repository.HypothesisRepository;
import java.util.UUID;
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
    private final EntityManager em;

    public HypothesisService(HypothesisRepository repository,
                             MarketNicheRepository nicheRepository,
                             AngleRepository angleRepository,
                             EntityManager em) {
        this.repository = repository;
        this.nicheRepository = nicheRepository;
        this.angleRepository = angleRepository;
        this.em = em;
    }

    private MarketNiche attachNiche(Long id) {
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
        if (req.getMarketNicheId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "marketNicheId required");
        }
        if (req.getPremiseAngleId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "angle required");
        }
        if (req.getKpiTargetCpl() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "kpiTargetCpl required");
        }
        if ("TRIPWIRE".equals(req.getOfferType()) && req.getPrice() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "price required for TRIPWIRE");
        }
    }

    @Transactional
    public Hypothesis create(CreateHypothesisRequest req) {
        validate(req);
        Hypothesis h = Hypothesis.builder()
                .marketNiche(attachNiche(req.getMarketNicheId()))
                .title(req.getTitle())
                .premiseAngle(attachAngle(req.getPremiseAngleId()))
                .offerType(req.getOfferType() == null ? null : OfferType.valueOf(req.getOfferType()))
                .price(req.getPrice())
                .kpiTargetCpl(req.getKpiTargetCpl())
                .build();
        return repository.save(h);
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
}

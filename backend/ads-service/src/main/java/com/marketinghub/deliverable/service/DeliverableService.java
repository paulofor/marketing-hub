package com.marketinghub.deliverable.service;

import com.marketinghub.deliverable.Deliverable;
import com.marketinghub.deliverable.dto.CreateDeliverableRequest;
import com.marketinghub.deliverable.dto.UpdateDeliverableRequest;
import com.marketinghub.deliverable.repository.DeliverableRepository;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.repository.MarketNicheRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Business logic for managing {@link Deliverable} records.
 */
@Service
public class DeliverableService {
    private final DeliverableRepository repository;
    private final MarketNicheRepository nicheRepository;

    public DeliverableService(DeliverableRepository repository, MarketNicheRepository nicheRepository) {
        this.repository = repository;
        this.nicheRepository = nicheRepository;
    }

    public List<Deliverable> listAll() {
        return repository.findAllLatestFirst();
    }

    public Deliverable get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Deliverable not found: " + id));
    }

    public List<Deliverable> listByNiche(Long nicheId) {
        if (!nicheRepository.existsById(nicheId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Market niche not found: " + nicheId);
        }
        return repository.findByNicheIdOrderByCreatedAtDesc(nicheId);
    }

    @Transactional
    public Deliverable create(CreateDeliverableRequest request) {
        if (request.getMarketNicheId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "marketNicheId is required");
        }
        if (!StringUtils.hasText(request.getTitle())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "title is required");
        }
        if (!StringUtils.hasText(request.getPrompt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "prompt is required");
        }
        MarketNiche niche = nicheRepository.findById(request.getMarketNicheId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Market niche not found: " + request.getMarketNicheId()));
        Deliverable deliverable = Deliverable.builder()
                .niche(niche)
                .title(request.getTitle().trim())
                .description(StringUtils.hasText(request.getDescription()) ? request.getDescription() : null)
                .content(StringUtils.hasText(request.getContent()) ? request.getContent() : null)
                .model(StringUtils.hasText(request.getModel()) ? request.getModel() : null)
                .prompt(request.getPrompt())
                .build();
        return repository.save(deliverable);
    }

    @Transactional
    public Deliverable update(Long id, UpdateDeliverableRequest request) {
        Deliverable deliverable = get(id);
        if (StringUtils.hasText(request.getTitle())) {
            deliverable.setTitle(request.getTitle().trim());
        }
        if (!StringUtils.hasText(request.getPrompt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "prompt is required");
        }
        deliverable.setPrompt(request.getPrompt());
        deliverable.setDescription(StringUtils.hasText(request.getDescription()) ? request.getDescription() : null);
        deliverable.setContent(StringUtils.hasText(request.getContent()) ? request.getContent() : null);
        deliverable.setModel(StringUtils.hasText(request.getModel()) ? request.getModel() : null);
        return repository.save(deliverable);
    }
}

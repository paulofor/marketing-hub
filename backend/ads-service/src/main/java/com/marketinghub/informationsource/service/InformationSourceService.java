package com.marketinghub.informationsource.service;

import com.marketinghub.informationsource.InformationSource;
import com.marketinghub.informationsource.dto.CreateInformationSourceRequest;
import com.marketinghub.informationsource.repository.InformationSourceRepository;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.repository.MarketNicheRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Business logic for managing information sources.
 */
@Service
public class InformationSourceService {
    private final InformationSourceRepository repository;
    private final MarketNicheRepository nicheRepository;

    public InformationSourceService(InformationSourceRepository repository, MarketNicheRepository nicheRepository) {
        this.repository = repository;
        this.nicheRepository = nicheRepository;
    }

    public List<InformationSource> listByNiche(Long nicheId) {
        if (!nicheRepository.existsById(nicheId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Market niche not found: " + nicheId);
        }
        return repository.findByNicheIdOrderByCreatedAtDesc(nicheId);
    }

    @Transactional
    public InformationSource create(CreateInformationSourceRequest request) {
        if (request.getMarketNicheId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "marketNicheId is required");
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
        }
        if (!StringUtils.hasText(request.getUrl())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "url is required");
        }
        MarketNiche niche = nicheRepository.findById(request.getMarketNicheId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Market niche not found: " + request.getMarketNicheId()));
        InformationSource source = InformationSource.builder()
                .niche(niche)
                .name(request.getName().trim())
                .url(request.getUrl().trim())
                .build();
        return repository.save(source);
    }
}

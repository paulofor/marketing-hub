package com.marketinghub.targeting.service;

import com.marketinghub.targeting.TargetingElement;
import com.marketinghub.targeting.dto.TargetingMetaAdsPendingElementDto;
import com.marketinghub.targeting.dto.UpdateTargetingMetaAdsDataRequest;
import com.marketinghub.repository.jpa.targeting.TargetingElementRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class TargetingMetaAdsSyncService {
    private final TargetingElementRepository repository;

    public TargetingMetaAdsSyncService(TargetingElementRepository repository) {
        this.repository = repository;
    }

    public List<TargetingMetaAdsPendingElementDto> listPending(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        return repository.findMetaAdsPending(PageRequest.of(0, safeLimit)).stream()
                .map(this::toPendingDto)
                .toList();
    }

    @Transactional
    public void updateMetaAdsData(Long id, UpdateTargetingMetaAdsDataRequest request) {
        if (id == null || request == null) {
            return;
        }
        TargetingElement element = repository.findById(id).orElseThrow();
        if (StringUtils.hasText(request.metaId())) {
            element.setMetaId(request.metaId().trim());
        }
        if (StringUtils.hasText(request.metaKey())) {
            element.setMetaKey(request.metaKey().trim());
        }
        if (request.metaAudienceSizeLowerBound() != null) {
            element.setMetaAudienceSizeLowerBound(request.metaAudienceSizeLowerBound());
        }
        if (request.metaAudienceSizeUpperBound() != null) {
            element.setMetaAudienceSizeUpperBound(request.metaAudienceSizeUpperBound());
        }
    }

    private TargetingMetaAdsPendingElementDto toPendingDto(TargetingElement element) {
        return new TargetingMetaAdsPendingElementDto(
                element.getId(),
                element.getNiche() != null ? element.getNiche().getId() : null,
                element.getType(),
                element.getTerm()
        );
    }
}

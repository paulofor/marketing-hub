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

/**
 * Serviço responsável por expor e atualizar dados oficiais da Meta Ads em elementos de segmentação.
 */
@Service
public class TargetingMetaAdsSyncService {
    private final TargetingElementRepository repository;

    /**
     * Inicializa o serviço com o repositório de elementos de segmentação.
     */
    public TargetingMetaAdsSyncService(TargetingElementRepository repository) {
        this.repository = repository;
    }

    /**
     * Lista elementos aprovados que ainda precisam de ID oficial e alcance da Meta Ads.
     */
    public List<TargetingMetaAdsPendingElementDto> listPending(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        return repository.findMetaAdsPending(PageRequest.of(0, safeLimit)).stream()
                .map(this::toPendingDto)
                .toList();
    }

    /**
     * Atualiza o elemento com ID oficial, chave exibível e faixa de audiência retornados pela Meta.
     */
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

    /**
     * Converte uma entidade pendente no DTO consumido pelo facebook-ads-worker.
     */
    private TargetingMetaAdsPendingElementDto toPendingDto(TargetingElement element) {
        return new TargetingMetaAdsPendingElementDto(
                element.getId(),
                element.getNiche() != null ? element.getNiche().getId() : null,
                element.getType(),
                element.getTerm()
        );
    }
}

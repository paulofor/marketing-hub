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
     * Atualiza o elemento com sucesso da Meta ou marca ausência definitiva de ID oficial para sair da fila.
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
        if (Boolean.TRUE.equals(request.metaIdUnavailable())) {
            element.setMetaId(null);
            element.setMetaKey(null);
            element.setMetaAudienceSizeLowerBound(null);
            element.setMetaAudienceSizeUpperBound(null);
            element.setMetaIdUnavailable(true);
            element.setMetaIdUnavailableReason(normalizeReason(request.metaIdUnavailableReason()));
        } else if (Boolean.FALSE.equals(request.metaIdUnavailable()) || StringUtils.hasText(request.metaId())) {
            element.setMetaIdUnavailable(false);
            element.setMetaIdUnavailableReason(null);
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

    /**
     * Normaliza a justificativa operacional para impedir payloads excessivos em campos de auditoria.
     */
    private String normalizeReason(String reason) {
        if (!StringUtils.hasText(reason)) {
            return null;
        }
        String normalized = reason.trim();
        return normalized.length() <= 1000 ? normalized : normalized.substring(0, 1000);
    }
}

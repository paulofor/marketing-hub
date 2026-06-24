package com.marketinghub.metaaudience.service;

import com.marketinghub.metaaudience.MetaAudience;
import com.marketinghub.metaaudience.MetaAudienceSegment;
import com.marketinghub.metaaudience.service.internalComplete.MetaAudienceSyncCompleteRequest;
import com.marketinghub.metaaudience.service.internalPending.MetaAudiencePendingResponse;
import com.marketinghub.metaaudience.service.requestAudience.MetaAudienceRequest;
import com.marketinghub.metaaudience.service.requestAudience.MetaAudienceResponse;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.repository.jpa.metaaudience.MetaAudienceRepository;
import com.marketinghub.repository.jpa.metaaudience.MetaAudienceSegmentRepository;
import com.marketinghub.repository.jpa.niche.MarketNicheRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Serviço de leitura e escrita das audiências Meta Ads decididas pelos módulos executores. */
@Service
@RequiredArgsConstructor
public class BackendMetaAudienceService {
    private final MetaAudienceRepository audienceRepository;
    private final MetaAudienceSegmentRepository segmentRepository;
    private final MarketNicheRepository nicheRepository;
    private final JdbcTemplate jdbcTemplate;

    /** Persiste uma audiência recebida do OPRM sem calcular elegibilidade, nome, recorte ou volume de negócio. */
    @Transactional
    public MetaAudienceResponse requestAudience(MetaAudienceRequest request) {
        validateRequiredPersistenceFields(request);
        MarketNiche niche = nicheRepository.findById(request.marketNicheId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nicho não encontrado."));
        Instant now = Instant.now();
        MetaAudience audience = new MetaAudience();
        audience.setMarketNiche(niche);
        audience.setSourceCnaeCode(normalizeCnae(request.cnaeCode()));
        audience.setAudienceName(limit(request.audienceName().trim(), 255));
        audience.setFacebookAdAccountId(request.facebookAdAccountId().trim());
        audience.setAudienceType(limit(request.audienceType().trim(), 32));
        audience.setSourceType(limit(request.sourceType().trim(), 32));
        audience.setFilterStrategy(request.filterStrategy());
        audience.setEligibilityStatus(limit(request.eligibilityStatus().trim(), 32));
        audience.setTotalContacts(nonNegative(request.totalContacts()));
        audience.setUniqueEmails(nonNegative(request.uniqueEmails()));
        audience.setSyncedContacts(0);
        audience.setCreatedAt(now);
        audience.setUpdatedAt(now);
        audience = audienceRepository.save(audience);

        MetaAudienceSegment segment = new MetaAudienceSegment();
        segment.setMetaAudience(audience);
        segment.setMarketNiche(niche);
        segment.setSegmentName(request.segmentName().trim());
        segment.setSegmentDescription(request.segmentDescription());
        segment.setPainFocus(request.painFocus());
        segment.setDesiredOutcomeFocus(request.desiredOutcomeFocus());
        segment.setOfferAngle(request.offerAngle());
        segment.setSelectionRule(request.filterStrategy());
        segment.setEstimatedContacts(nonNegative(request.estimatedContacts()));
        segment.setCreatedAt(now);
        segment.setUpdatedAt(now);
        segmentRepository.save(segment);
        return toResponse(audience);
    }

    /** Lista registros em READY e apenas anexa os emails brutos persistidos para execução técnica do worker. */
    @Transactional(readOnly = true)
    public List<MetaAudiencePendingResponse> listPending(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 20));
        return audienceRepository.findByEligibilityStatusOrderByUpdatedAtAsc("READY", PageRequest.of(0, safeLimit)).stream()
                .map(a -> new MetaAudiencePendingResponse(a.getId(), a.getMarketNiche().getId(), a.getSourceCnaeCode(),
                        a.getAudienceName(), a.getFacebookAdAccountId(), loadEmails(a.getSourceCnaeCode())))
                .toList();
    }

    /** Atualiza o estado persistido da audiência conforme o resultado técnico reportado pelo worker. */
    @Transactional
    public MetaAudienceResponse completeSync(Long id, MetaAudienceSyncCompleteRequest request) {
        MetaAudience audience = audienceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Audiência não encontrada."));
        Instant now = Instant.now();
        if (request != null && "FAILED".equalsIgnoreCase(request.status())) {
            audience.setEligibilityStatus("FAILED");
            audience.setErrorMessage(limit(request.errorMessage(), 1000));
        } else {
            audience.setEligibilityStatus("SYNCED");
            audience.setFacebookAudienceId(request != null ? request.facebookAudienceId() : null);
            audience.setSyncedContacts(request != null ? request.syncedContacts() : 0);
            audience.setLastSyncAt(now);
            audience.setErrorMessage(null);
        }
        audience.setUpdatedAt(now);
        return toResponse(audienceRepository.save(audience));
    }

    /** Valida somente campos necessários para persistência e relacionamento, sem regra de negócio de público. */
    private void validateRequiredPersistenceFields(MetaAudienceRequest request) {
        if (request == null || request.marketNicheId() == null || !StringUtils.hasText(request.cnaeCode())
                || !StringUtils.hasText(request.audienceName()) || !StringUtils.hasText(request.segmentName())
                || !StringUtils.hasText(request.facebookAdAccountId()) || !StringUtils.hasText(request.audienceType())
                || !StringUtils.hasText(request.sourceType()) || !StringUtils.hasText(request.eligibilityStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Campos persistentes obrigatórios ausentes para registrar a audiência Meta.");
        }
    }

    /** Lê os emails persistidos pelo backend para entrega técnica ao worker, sem calcular seleção de público. */
    private List<String> loadEmails(String cnaeCode) {
        return jdbcTemplate.queryForList(
                "SELECT email FROM oprm_estabelecimento_cnae_raiz WHERE cnae_code = ? AND email IS NOT NULL AND email <> ''",
                String.class,
                cnaeCode);
    }

    /** Normaliza o CNAE apenas para alinhar a chave física de leitura e escrita. */
    private String normalizeCnae(String cnaeCode) {
        return cnaeCode.replaceAll("\\D", "");
    }

    /** Converte valores nulos em zero para persistência numérica simples. */
    private long nonNegative(Long value) {
        return value == null || value < 0 ? 0 : value;
    }

    /** Converte a entidade persistida para resposta pública enxuta. */
    private MetaAudienceResponse toResponse(MetaAudience audience) {
        return new MetaAudienceResponse(audience.getId(), audience.getMarketNiche().getId(), audience.getAudienceName(),
                audience.getEligibilityStatus(), audience.getTotalContacts(), audience.getUniqueEmails());
    }

    /** Limita textos ao tamanho máximo da coluna de destino. */
    private String limit(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}

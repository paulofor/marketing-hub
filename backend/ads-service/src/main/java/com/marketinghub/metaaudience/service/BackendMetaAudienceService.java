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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Serviço responsável por controlar audiências Meta Ads vinculadas a nichos e expor pendências ao worker. */
@Service
@RequiredArgsConstructor
@Slf4j
public class BackendMetaAudienceService {
    private final MetaAudienceRepository audienceRepository;
    private final MetaAudienceSegmentRepository segmentRepository;
    private final MarketNicheRepository nicheRepository;
    private final JdbcTemplate jdbcTemplate;

    /** Cria uma audiência pronta para sincronização quando existe volume mínimo de emails no CNAE informado. */
    @Transactional
    public MetaAudienceResponse requestAudience(MetaAudienceRequest request) {
        if (request == null || request.marketNicheId() == null || !StringUtils.hasText(request.cnaeCode())
                || !StringUtils.hasText(request.segmentName()) || !StringUtils.hasText(request.facebookAdAccountId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nicho, CNAE, segmento e conta Meta são obrigatórios.");
        }
        MarketNiche niche = nicheRepository.findById(request.marketNicheId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nicho não encontrado."));
        String cnaeCode = request.cnaeCode().replaceAll("\\D", "");
        List<String> emails = loadUniqueEmails(cnaeCode);
        if (emails.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CNAE sem emails elegíveis para audiência Meta.");
        }
        Instant now = Instant.now();
        MetaAudience audience = new MetaAudience();
        audience.setMarketNiche(niche);
        audience.setSourceCnaeCode(cnaeCode);
        audience.setAudienceName(buildAudienceName(niche.getId(), request.segmentName(), cnaeCode));
        audience.setFacebookAdAccountId(request.facebookAdAccountId().trim());
        audience.setAudienceType("CUSTOMER_LIST");
        audience.setSourceType("OPRM_CNAE_EMAILS");
        audience.setFilterStrategy(request.filterStrategy());
        audience.setEligibilityStatus("READY");
        audience.setTotalContacts(emails.size());
        audience.setUniqueEmails(emails.size());
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
        segment.setEstimatedContacts(emails.size());
        segment.setCreatedAt(now);
        segment.setUpdatedAt(now);
        segmentRepository.save(segment);
        return toResponse(audience);
    }

    /** Lista audiências prontas com emails normalizados para o worker criar na Meta. */
    @Transactional(readOnly = true)
    public List<MetaAudiencePendingResponse> listPending(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 20));
        return audienceRepository.findByEligibilityStatusOrderByUpdatedAtAsc("READY", PageRequest.of(0, safeLimit)).stream()
                .map(a -> new MetaAudiencePendingResponse(a.getId(), a.getMarketNiche().getId(), a.getSourceCnaeCode(),
                        a.getAudienceName(), a.getFacebookAdAccountId(), loadUniqueEmails(a.getSourceCnaeCode())))
                .toList();
    }

    /** Registra sucesso ou falha retornada pelo worker após chamada à Meta. */
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

    /** Carrega emails únicos do CNAE em ordem estável para garantir idempotência do envio ao worker. */
    private List<String> loadUniqueEmails(String cnaeCode) {
        List<String> rows = jdbcTemplate.queryForList(
                "SELECT email FROM oprm_estabelecimento_cnae_raiz WHERE cnae_code = ? AND email IS NOT NULL AND email <> ''",
                String.class,
                cnaeCode);
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String row : rows) {
            if (StringUtils.hasText(row)) {
                unique.add(row.trim().toLowerCase(Locale.ROOT));
            }
        }
        return List.copyOf(unique);
    }

    /** Monta o nome funcional obrigatório da audiência na Meta. */
    private String buildAudienceName(Long marketNicheId, String segmentName, String cnaeCode) {
        return limit("MH - Nicho " + marketNicheId + " - " + segmentName.trim() + " - CNAE " + cnaeCode, 255);
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

package com.marketinghub.oprm.generalaudience.service;

import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceSeed;
import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceSeedStatus;
import com.marketinghub.oprm.generalaudience.service.createSeed.CreateGeneralAudienceSeedRequest;
import com.marketinghub.oprm.generalaudience.service.getSeed.GeneralAudienceSeedResponse;
import com.marketinghub.oprm.generalaudience.service.listSeeds.GeneralAudienceSeedSummaryResponse;
import com.marketinghub.oprm.generalaudience.service.updateSeed.UpdateGeneralAudienceSeedRequest;
import com.marketinghub.repository.jpa.oprm.generalaudience.OprmGeneralAudienceSeedRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Responsável por orquestrar o cadastro e a revisão manual de sementes de público geral no OPRM. */
@Service
public class OprmGeneralAudienceService {

    private static final String DEFAULT_COUNTRY = "BR";
    private static final String DEFAULT_LANGUAGE = "pt-BR";
    private final OprmGeneralAudienceSeedRepository seedRepository;

    /** Inicializa o serviço com a persistência centralizada de sementes de público geral. */
    public OprmGeneralAudienceService(OprmGeneralAudienceSeedRepository seedRepository) {
        this.seedRepository = seedRepository;
    }

    /** Lista sementes cadastradas para seleção e revisão manual pelo usuário. */
    @Transactional(readOnly = true)
    public List<GeneralAudienceSeedSummaryResponse> listSeeds() {
        return seedRepository.findAllByOrderByUpdatedAtDesc().stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    /** Cadastra uma nova semente ampla sem tratá-la como nicho ou campanha pronta. */
    @Transactional
    public GeneralAudienceSeedResponse createSeed(CreateGeneralAudienceSeedRequest request) {
        OprmGeneralAudienceSeed seed = new OprmGeneralAudienceSeed();
        seed.setName(requiredText(request.name(), "name"));
        seed.setDescription(normalizeOptionalText(request.description()));
        seed.setMarketContext(normalizeOptionalText(request.marketContext()));
        seed.setCountry(resolveDefaultText(request.country(), DEFAULT_COUNTRY));
        seed.setLanguage(resolveDefaultText(request.language(), DEFAULT_LANGUAGE));
        seed.setSeedType(request.seedType());
        seed.setStatus(request.status() == null ? OprmGeneralAudienceSeedStatus.DRAFT : request.status());
        seed.setBusinessGoal(normalizeOptionalText(request.businessGoal()));
        seed.setRiskNotes(normalizeOptionalText(request.riskNotes()));
        return toResponse(seedRepository.save(seed));
    }

    /** Detalha uma semente para deixar claro que ela ainda precisa ser quebrada em subnichos e dores. */
    @Transactional(readOnly = true)
    public GeneralAudienceSeedResponse getSeed(Long seedId) {
        return toResponse(findSeed(seedId));
    }

    /** Atualiza campos manuais da semente sem acionar descoberta automática ou publicação de campanha. */
    @Transactional
    public GeneralAudienceSeedResponse updateSeed(Long seedId, UpdateGeneralAudienceSeedRequest request) {
        OprmGeneralAudienceSeed seed = findSeed(seedId);
        if (request.name() != null) {
            seed.setName(requiredText(request.name(), "name"));
        }
        if (request.description() != null) {
            seed.setDescription(normalizeOptionalText(request.description()));
        }
        if (request.marketContext() != null) {
            seed.setMarketContext(normalizeOptionalText(request.marketContext()));
        }
        if (request.country() != null) {
            seed.setCountry(requiredText(request.country(), "country"));
        }
        if (request.language() != null) {
            seed.setLanguage(requiredText(request.language(), "language"));
        }
        if (request.seedType() != null) {
            seed.setSeedType(request.seedType());
        }
        if (request.status() != null) {
            seed.setStatus(request.status());
        }
        if (request.businessGoal() != null) {
            seed.setBusinessGoal(normalizeOptionalText(request.businessGoal()));
        }
        if (request.riskNotes() != null) {
            seed.setRiskNotes(normalizeOptionalText(request.riskNotes()));
        }
        return toResponse(seedRepository.save(seed));
    }

    /** Arquiva a semente para removê-la do fluxo ativo sem apagar histórico de decisão. */
    @Transactional
    public GeneralAudienceSeedResponse archiveSeed(Long seedId) {
        OprmGeneralAudienceSeed seed = findSeed(seedId);
        seed.setStatus(OprmGeneralAudienceSeedStatus.ARCHIVED);
        return toResponse(seedRepository.save(seed));
    }

    /** Busca a semente ou devolve erro HTTP de recurso inexistente. */
    private OprmGeneralAudienceSeed findSeed(Long seedId) {
        return seedRepository.findById(seedId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Semente de público geral não encontrada: " + seedId));
    }

    /** Converte a entidade de semente para o contrato detalhado. */
    private GeneralAudienceSeedResponse toResponse(OprmGeneralAudienceSeed seed) {
        return new GeneralAudienceSeedResponse(
                seed.getId(),
                seed.getName(),
                seed.getDescription(),
                seed.getMarketContext(),
                seed.getCountry(),
                seed.getLanguage(),
                seed.getSeedType(),
                seed.getStatus(),
                seed.getBusinessGoal(),
                seed.getRiskNotes(),
                seed.getCreatedAt(),
                seed.getUpdatedAt());
    }

    /** Converte a entidade de semente para o contrato resumido de listagem. */
    private GeneralAudienceSeedSummaryResponse toSummaryResponse(OprmGeneralAudienceSeed seed) {
        return new GeneralAudienceSeedSummaryResponse(
                seed.getId(),
                seed.getName(),
                seed.getMarketContext(),
                seed.getCountry(),
                seed.getLanguage(),
                seed.getSeedType(),
                seed.getStatus(),
                seed.getUpdatedAt());
    }

    /** Normaliza texto obrigatório e rejeita valor vazio para evitar semente sem decisão comercial. */
    private String requiredText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " não pode ficar vazio");
        }
        return value.trim();
    }

    /** Aplica valor padrão quando o campo opcional não foi informado. */
    private String resolveDefaultText(String value, String defaultValue) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        return value.trim();
    }

    /** Normaliza textos opcionais para persistir nulo em vez de conteúdo vazio. */
    private String normalizeOptionalText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}

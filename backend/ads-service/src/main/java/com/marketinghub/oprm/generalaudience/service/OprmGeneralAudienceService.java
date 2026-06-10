package com.marketinghub.oprm.generalaudience.service;

import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceSeed;
import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceSeedStatus;
import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceSubniche;
import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceSubnicheStatus;
import com.marketinghub.oprm.generalaudience.service.convertToMarketNiche.ConvertGeneralAudienceSubnicheToMarketNicheRequest;
import com.marketinghub.oprm.generalaudience.service.convertToMarketNiche.GeneralAudienceMarketNicheConversionResponse;
import com.marketinghub.oprm.generalaudience.service.createSeed.CreateGeneralAudienceSeedRequest;
import com.marketinghub.oprm.generalaudience.service.createSubniche.CreateGeneralAudienceSubnicheRequest;
import com.marketinghub.oprm.generalaudience.service.getSeed.GeneralAudienceSeedResponse;
import com.marketinghub.oprm.generalaudience.service.getSubniche.GeneralAudienceSubnicheResponse;
import com.marketinghub.oprm.generalaudience.service.listSeeds.GeneralAudienceSeedSummaryResponse;
import com.marketinghub.oprm.generalaudience.service.listSubniches.GeneralAudienceSubnicheSummaryResponse;
import com.marketinghub.oprm.generalaudience.service.updateSeed.UpdateGeneralAudienceSeedRequest;
import com.marketinghub.oprm.generalaudience.service.updateSubniche.UpdateGeneralAudienceSubnicheRequest;
import com.marketinghub.repository.jpa.oprm.generalaudience.OprmGeneralAudienceMarketNicheMaterializationRepository;
import com.marketinghub.repository.jpa.oprm.generalaudience.OprmGeneralAudienceSeedRepository;
import com.marketinghub.repository.jpa.oprm.generalaudience.OprmGeneralAudienceSubnicheRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Responsável por orquestrar o cadastro e a revisão manual de sementes e subnichos gerais no OPRM. */
@Service
public class OprmGeneralAudienceService {

    private static final String DEFAULT_COUNTRY = "BR";
    private static final String DEFAULT_LANGUAGE = "pt-BR";
    private final OprmGeneralAudienceSeedRepository seedRepository;
    private final OprmGeneralAudienceSubnicheRepository subnicheRepository;
    private final OprmGeneralAudienceMarketNicheMaterializationRepository marketNicheMaterializationRepository;

    /** Inicializa o serviço com a persistência centralizada de públicos gerais. */
    public OprmGeneralAudienceService(
            OprmGeneralAudienceSeedRepository seedRepository,
            OprmGeneralAudienceSubnicheRepository subnicheRepository,
            OprmGeneralAudienceMarketNicheMaterializationRepository marketNicheMaterializationRepository) {
        this.seedRepository = seedRepository;
        this.subnicheRepository = subnicheRepository;
        this.marketNicheMaterializationRepository = marketNicheMaterializationRepository;
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

    /** Lista subnichos descobertos para uma semente sem consultar ou alterar o fluxo NichoCNAE. */
    @Transactional(readOnly = true)
    public List<GeneralAudienceSubnicheSummaryResponse> listSubniches(Long seedId) {
        findSeed(seedId);
        return subnicheRepository.findAllBySeedIdOrderByUpdatedAtDesc(seedId).stream()
                .map(this::toSubnicheSummaryResponse)
                .toList();
    }

    /** Cadastra um subnicho derivado de uma semente com dados suficientes para revisão comercial. */
    @Transactional
    public GeneralAudienceSubnicheResponse createSubniche(
            Long seedId,
            CreateGeneralAudienceSubnicheRequest request) {
        OprmGeneralAudienceSeed seed = findSeed(seedId);
        OprmGeneralAudienceSubniche subniche = new OprmGeneralAudienceSubniche();
        subniche.setSeed(seed);
        subniche.setName(requiredText(request.name(), "name"));
        subniche.setPersonaSummary(normalizeOptionalText(request.personaSummary()));
        subniche.setPainSummary(normalizeOptionalText(request.painSummary()));
        subniche.setDesiredOutcomeSummary(normalizeOptionalText(request.desiredOutcomeSummary()));
        subniche.setLanguagePatterns(normalizeOptionalText(request.languagePatterns()));
        subniche.setChannelsSummary(normalizeOptionalText(request.channelsSummary()));
        subniche.setQualificationQuestion(normalizeOptionalText(request.qualificationQuestion()));
        subniche.setStatus(request.status() == null
                ? OprmGeneralAudienceSubnicheStatus.DISCOVERED
                : request.status());
        subniche.setOpportunityScore(request.opportunityScore());
        subniche.setRiskScore(request.riskScore());
        subniche.setMarketNicheId(request.marketNicheId());
        return toSubnicheResponse(subnicheRepository.save(subniche));
    }

    /** Detalha um subnicho para revisão de persona, dores, canais e pergunta qualificadora. */
    @Transactional(readOnly = true)
    public GeneralAudienceSubnicheResponse getSubniche(Long subnicheId) {
        return toSubnicheResponse(findSubniche(subnicheId));
    }

    /** Atualiza campos manuais do subnicho sem convertê-lo automaticamente em nicho ou campanha. */
    @Transactional
    public GeneralAudienceSubnicheResponse updateSubniche(
            Long subnicheId,
            UpdateGeneralAudienceSubnicheRequest request) {
        OprmGeneralAudienceSubniche subniche = findSubniche(subnicheId);
        if (request.name() != null) {
            subniche.setName(requiredText(request.name(), "name"));
        }
        if (request.personaSummary() != null) {
            subniche.setPersonaSummary(normalizeOptionalText(request.personaSummary()));
        }
        if (request.painSummary() != null) {
            subniche.setPainSummary(normalizeOptionalText(request.painSummary()));
        }
        if (request.desiredOutcomeSummary() != null) {
            subniche.setDesiredOutcomeSummary(normalizeOptionalText(request.desiredOutcomeSummary()));
        }
        if (request.languagePatterns() != null) {
            subniche.setLanguagePatterns(normalizeOptionalText(request.languagePatterns()));
        }
        if (request.channelsSummary() != null) {
            subniche.setChannelsSummary(normalizeOptionalText(request.channelsSummary()));
        }
        if (request.qualificationQuestion() != null) {
            subniche.setQualificationQuestion(normalizeOptionalText(request.qualificationQuestion()));
        }
        if (request.status() != null) {
            subniche.setStatus(request.status());
        }
        if (request.opportunityScore() != null) {
            subniche.setOpportunityScore(request.opportunityScore());
        }
        if (request.riskScore() != null) {
            subniche.setRiskScore(request.riskScore());
        }
        if (request.marketNicheId() != null) {
            subniche.setMarketNicheId(request.marketNicheId());
        }
        return toSubnicheResponse(subnicheRepository.save(subniche));
    }

    /** Aprova um subnicho para experimento quando ele já tem definição comercial suficiente. */
    @Transactional
    public GeneralAudienceSubnicheResponse approveSubniche(Long subnicheId) {
        OprmGeneralAudienceSubniche subniche = findSubniche(subnicheId);
        subniche.setStatus(OprmGeneralAudienceSubnicheStatus.APPROVED_FOR_EXPERIMENT);
        return toSubnicheResponse(subnicheRepository.save(subniche));
    }

    /** Rejeita um subnicho para evitar avanço de público amplo, genérico ou sem confirmação de lead. */
    @Transactional
    public GeneralAudienceSubnicheResponse rejectSubniche(Long subnicheId) {
        OprmGeneralAudienceSubniche subniche = findSubniche(subnicheId);
        subniche.setStatus(OprmGeneralAudienceSubnicheStatus.REJECTED);
        return toSubnicheResponse(subnicheRepository.save(subniche));
    }

    /** Converte um subnicho aprovado em MarketNiche sem consultar ou alterar tabelas CNAE. */
    @Transactional
    public GeneralAudienceMarketNicheConversionResponse convertSubnicheToMarketNiche(
            Long subnicheId,
            ConvertGeneralAudienceSubnicheToMarketNicheRequest request) {
        ConvertGeneralAudienceSubnicheToMarketNicheRequest safeRequest = request == null
                ? new ConvertGeneralAudienceSubnicheToMarketNicheRequest(null, null, null, null, null)
                : request;
        OprmGeneralAudienceSubniche subniche = findSubniche(subnicheId);
        validateSubnicheCanBecomeMarketNiche(subniche);
        var marketNiche = marketNicheMaterializationRepository.saveMarketNiche(
                subniche.getMarketNicheId(),
                resolveDefaultText(safeRequest.name(), subniche.getName()),
                materializedDescription(subniche, safeRequest.description()),
                materializedBaseSegmentation(subniche, safeRequest.baseSegmentation()),
                materializedInterests(subniche, safeRequest.interests()),
                materializedDemographicFilters(subniche, safeRequest.demographicFilters()),
                materializedExtraTips(subniche));
        subniche.setMarketNicheId(marketNiche.id());
        subniche.setStatus(OprmGeneralAudienceSubnicheStatus.CONVERTED_TO_NICHE);
        OprmGeneralAudienceSubniche savedSubniche = subnicheRepository.save(subniche);
        return new GeneralAudienceMarketNicheConversionResponse(
                savedSubniche.getId(),
                savedSubniche.getSeed().getId(),
                marketNiche.id(),
                marketNiche.name(),
                savedSubniche.getStatus(),
                marketNiche.reusedExisting(),
                savedSubniche.getUpdatedAt());
    }

    /** Busca a semente ou devolve erro HTTP de recurso inexistente. */
    private OprmGeneralAudienceSeed findSeed(Long seedId) {
        return seedRepository.findById(seedId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Semente de público geral não encontrada: " + seedId));
    }

    /** Busca o subnicho ou devolve erro HTTP de recurso inexistente. */
    private OprmGeneralAudienceSubniche findSubniche(Long subnicheId) {
        return subnicheRepository.findById(subnicheId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Subnicho de público geral não encontrado: " + subnicheId));
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

    /** Converte a entidade de subnicho para o contrato detalhado. */
    private GeneralAudienceSubnicheResponse toSubnicheResponse(OprmGeneralAudienceSubniche subniche) {
        return new GeneralAudienceSubnicheResponse(
                subniche.getId(),
                subniche.getSeed().getId(),
                subniche.getName(),
                subniche.getPersonaSummary(),
                subniche.getPainSummary(),
                subniche.getDesiredOutcomeSummary(),
                subniche.getLanguagePatterns(),
                subniche.getChannelsSummary(),
                subniche.getQualificationQuestion(),
                subniche.getStatus(),
                subniche.getOpportunityScore(),
                subniche.getRiskScore(),
                subniche.getMarketNicheId(),
                subniche.getCreatedAt(),
                subniche.getUpdatedAt());
    }

    /** Converte a entidade de subnicho para o contrato resumido de listagem. */
    private GeneralAudienceSubnicheSummaryResponse toSubnicheSummaryResponse(OprmGeneralAudienceSubniche subniche) {
        return new GeneralAudienceSubnicheSummaryResponse(
                subniche.getId(),
                subniche.getSeed().getId(),
                subniche.getName(),
                subniche.getPersonaSummary(),
                subniche.getPainSummary(),
                subniche.getChannelsSummary(),
                subniche.getQualificationQuestion(),
                subniche.getStatus(),
                subniche.getOpportunityScore(),
                subniche.getRiskScore(),
                subniche.getMarketNicheId(),
                subniche.getUpdatedAt());
    }


    /** Valida se o subnicho tem aprovação humana e campos mínimos antes de virar nicho. */
    private void validateSubnicheCanBecomeMarketNiche(OprmGeneralAudienceSubniche subniche) {
        if (subniche.getStatus() != OprmGeneralAudienceSubnicheStatus.APPROVED_FOR_EXPERIMENT
                && subniche.getStatus() != OprmGeneralAudienceSubnicheStatus.CONVERTED_TO_NICHE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Subnicho precisa estar aprovado para experimento antes de virar MarketNiche");
        }
        requiredText(subniche.getName(), "name");
        requiredText(subniche.getPersonaSummary(), "personaSummary");
        requiredText(subniche.getPainSummary(), "painSummary");
        requiredText(subniche.getQualificationQuestion(), "qualificationQuestion");
    }

    /** Monta descrição do nicho com origem rastreável de público geral e sem referência a CNAE. */
    private String materializedDescription(
            OprmGeneralAudienceSubniche subniche,
            String overrideDescription) {
        if (StringUtils.hasText(overrideDescription)) {
            return overrideDescription.trim();
        }
        return String.join("\n\n",
                "Nicho convertido a partir de Público Geral OPRM.",
                "Origem: Público Geral > " + subniche.getSeed().getName() + " > " + subniche.getName(),
                "Persona/contexto:\n" + requiredText(subniche.getPersonaSummary(), "personaSummary"),
                "Dor principal:\n" + requiredText(subniche.getPainSummary(), "painSummary"),
                "Resultado desejado:\n" + optionalText(subniche.getDesiredOutcomeSummary()),
                "Pergunta qualificadora:\n" + requiredText(subniche.getQualificationQuestion(), "qualificationQuestion"));
    }

    /** Monta segmentação base do nicho a partir da semente e do subnicho aprovados. */
    private String materializedBaseSegmentation(
            OprmGeneralAudienceSubniche subniche,
            String overrideBaseSegmentation) {
        if (StringUtils.hasText(overrideBaseSegmentation)) {
            return overrideBaseSegmentation.trim();
        }
        return "Público Geral OPRM: " + subniche.getSeed().getName()
                + " > " + subniche.getName()
                + ". País: " + subniche.getSeed().getCountry()
                + ". Idioma: " + subniche.getSeed().getLanguage()
                + ". Triagem obrigatória: " + requiredText(subniche.getQualificationQuestion(), "qualificationQuestion");
    }

    /** Monta interesses sugeridos sem transformar público geral em CNAE. */
    private String materializedInterests(
            OprmGeneralAudienceSubniche subniche,
            String overrideInterests) {
        if (StringUtils.hasText(overrideInterests)) {
            return overrideInterests.trim();
        }
        return String.join("\n\n",
                "Canais e contexto de aquisição:\n" + optionalText(subniche.getChannelsSummary()),
                "Padrões de linguagem observados:\n" + optionalText(subniche.getLanguagePatterns()));
    }

    /** Monta filtros demográficos e de triagem para evitar público amplo sem confirmação. */
    private String materializedDemographicFilters(
            OprmGeneralAudienceSubniche subniche,
            String overrideDemographicFilters) {
        if (StringUtils.hasText(overrideDemographicFilters)) {
            return overrideDemographicFilters.trim();
        }
        return "Usar demografia apenas quando fizer sentido para " + subniche.getName()
                + ". Confirmar pertencimento pela pergunta: "
                + requiredText(subniche.getQualificationQuestion(), "qualificationQuestion");
    }

    /** Monta dicas operacionais de origem para preservar rastreabilidade sem acionar hipótese. */
    private String materializedExtraTips(OprmGeneralAudienceSubniche subniche) {
        return String.join("\n",
                "Origem operacional: OPRM_PUBLICO_GERAL",
                "seedId=" + subniche.getSeed().getId(),
                "subnicheId=" + subniche.getId(),
                "Não foi criado a partir de CNAE.",
                "Próximo passo recomendado: criar hipótese específica para uma dor principal antes de qualquer campanha.");
    }

    /** Retorna texto opcional ou aviso claro de ausência para materialização auditável. */
    private String optionalText(String value) {
        if (!StringUtils.hasText(value)) {
            return "Não informado na revisão do subnicho.";
        }
        return value.trim();
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

package com.marketinghub.facebookads.service.recommendation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.facebookads.FacebookAdStatus;
import com.marketinghub.facebookads.FacebookAdsCampaign;
import com.marketinghub.facebookads.FacebookAdsRecommendation;
import com.marketinghub.repository.jpa.facebookads.FacebookAdsCampaignRepository;
import com.marketinghub.repository.jpa.facebookads.FacebookAdsRecommendationRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Orquestra a seleção e persistência das sugestões oficiais da Meta para campanhas ativas.
 */
@Service
public class FacebookCampaignRecommendationService {
    private final FacebookAdsCampaignRepository campaignRepository;
    private final FacebookAdsRecommendationRepository recommendationRepository;
    private final ObjectMapper objectMapper;

    /**
     * Cria o serviço com os repositórios necessários para sincronizar sugestões da Meta.
     */
    public FacebookCampaignRecommendationService(FacebookAdsCampaignRepository campaignRepository,
                                                 FacebookAdsRecommendationRepository recommendationRepository,
                                                 ObjectMapper objectMapper) {
        this.campaignRepository = campaignRepository;
        this.recommendationRepository = recommendationRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Lista campanhas ativas e em execução que devem ter sugestões coletadas pelo worker.
     */
    @Transactional(readOnly = true)
    public List<FacebookCampaignRecommendationSyncTarget> listSyncTargets() {
        return campaignRepository
                .findAllByExperimentStatusAndStatus(ExperimentStatus.RUNNING, FacebookAdStatus.ACTIVE)
                .stream()
                .map(campaign -> new FacebookCampaignRecommendationSyncTarget(
                        campaign.getId(),
                        StringUtils.hasText(campaign.getExternalId()) ? campaign.getExternalId() : campaign.getId(),
                        campaign.getExperiment().getId(),
                        campaign.getAdAccountId(),
                        campaign.getRecommendationsLastSyncedAt()))
                .toList();
    }

    /**
     * Substitui o retrato de sugestões de uma campanha pelo payload mais recente coletado na Meta.
     */
    @Transactional
    public void ingest(String campaignId, FacebookCampaignRecommendationIngestionRequest request) {
        FacebookAdsCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new NoSuchElementException("Facebook campaign not found: " + campaignId));
        Instant collectedAt = request != null && request.collectedAt() != null ? request.collectedAt() : Instant.now();
        recommendationRepository.deleteByCampaignId(campaignId);
        for (JsonNode recommendationNode : normalizeRecommendations(request != null ? request.recommendations() : null)) {
            recommendationRepository.save(toEntity(campaign, recommendationNode, collectedAt));
        }
        campaign.setRecommendationsLastSyncedAt(collectedAt);
        campaign.setRecommendationsLastError(null);
    }

    /**
     * Registra falha de coleta para a campanha sem apagar o último retrato válido.
     */
    @Transactional
    public void registerError(String campaignId, String message) {
        FacebookAdsCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new NoSuchElementException("Facebook campaign not found: " + campaignId));
        campaign.setRecommendationsLastError(sanitizeMessage(message));
    }

    /**
     * Lista as sugestões atualmente salvas para uma campanha.
     */
    @Transactional(readOnly = true)
    public List<FacebookCampaignRecommendationDto> listByCampaign(String campaignId) {
        return recommendationRepository.findByCampaignIdOrderByCollectedAtDescIdAsc(campaignId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Normaliza o payload recebido da Meta para uma lista de itens de recomendação.
     */
    private List<JsonNode> normalizeRecommendations(JsonNode recommendations) {
        if (recommendations == null || recommendations.isNull() || recommendations.isMissingNode()) {
            return List.of();
        }
        JsonNode candidates = recommendations.has("data") ? recommendations.path("data") : recommendations;
        if (!candidates.isArray()) {
            return List.of(candidates);
        }
        List<JsonNode> result = new ArrayList<>();
        candidates.forEach(result::add);
        return result;
    }

    /**
     * Converte uma recomendação bruta da Meta na entidade persistida pelo backend.
     */
    private FacebookAdsRecommendation toEntity(FacebookAdsCampaign campaign, JsonNode node, Instant collectedAt) {
        FacebookAdsRecommendation recommendation = new FacebookAdsRecommendation();
        recommendation.setCampaign(campaign);
        recommendation.setRecommendationCode(text(node, "code"));
        recommendation.setTitle(text(node, "title"));
        recommendation.setMessage(text(node, "message"));
        recommendation.setImportance(text(node, "importance"));
        recommendation.setConfidence(text(node, "confidence"));
        recommendation.setBlameField(text(node, "blame_field"));
        recommendation.setRecommendationDataJson(writeJson(node.path("recommendation_data")));
        recommendation.setRawJson(writeJson(node));
        recommendation.setCollectedAt(collectedAt);
        return recommendation;
    }

    /**
     * Converte a entidade persistida no contrato de leitura da API.
     */
    private FacebookCampaignRecommendationDto toDto(FacebookAdsRecommendation recommendation) {
        return new FacebookCampaignRecommendationDto(
                recommendation.getId(),
                recommendation.getCampaign().getId(),
                recommendation.getRecommendationCode(),
                recommendation.getTitle(),
                recommendation.getMessage(),
                recommendation.getImportance(),
                recommendation.getConfidence(),
                recommendation.getBlameField(),
                recommendation.getRecommendationDataJson(),
                recommendation.getRawJson(),
                recommendation.getCollectedAt());
    }

    /**
     * Lê um campo textual simples preservando nulo quando a Meta não envia valor útil.
     */
    private String text(JsonNode node, String fieldName) {
        if (node == null || !node.hasNonNull(fieldName)) {
            return null;
        }
        String value = node.path(fieldName).asText(null);
        return StringUtils.hasText(value) ? value : null;
    }

    /**
     * Serializa um nó JSON para armazenamento bruto e auditável.
     */
    private String writeJson(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Could not serialize Facebook recommendation payload", ex);
        }
    }

    /**
     * Limita a mensagem de erro para armazenamento operacional no cadastro da campanha.
     */
    private String sanitizeMessage(String message) {
        if (!StringUtils.hasText(message)) {
            return "Unknown recommendation sync error";
        }
        String trimmed = message.trim();
        return trimmed.length() > 500 ? trimmed.substring(0, 500) : trimmed;
    }
}

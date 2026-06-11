package com.marketinghub.niche.service;

import com.marketinghub.chat.ChatDialog;
import com.marketinghub.repository.jpa.chat.ChatDialogRepository;
import com.marketinghub.differentiatedtechnology.DifferentiatedTechnology;
import com.marketinghub.repository.jpa.differentiatedtechnology.DifferentiatedTechnologyRepository;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.MarketNicheEnrichmentProfile;
import com.marketinghub.niche.dto.CreateMarketNicheRequest;
import com.marketinghub.niche.dto.MarketNicheListItemProjection;
import com.marketinghub.niche.description.NicheDetailedDescription;
import com.marketinghub.repository.jpa.niche.description.NicheDetailedDescriptionRepository;
import com.marketinghub.repository.jpa.niche.MarketNicheRepository;
import com.marketinghub.repository.jpa.niche.MarketNicheEnrichmentProfileRepository;
import com.marketinghub.targeting.service.TargetingElementSyncService;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.experiment.ExperimentStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Responsabilidade: coordenar regras administrativas e operacionais de nichos de mercado.
 */
@Service
public class MarketNicheService {
    private final MarketNicheRepository repository;
    private final MarketNicheEnrichmentProfileRepository enrichmentProfileRepository;
    private final ChatDialogRepository chatDialogRepository;
    private final DifferentiatedTechnologyRepository differentiatedTechnologyRepository;
    private final NicheDetailedDescriptionRepository detailedDescriptionRepository;
    private final TargetingElementSyncService targetingElementSyncService;

    /** Inicializa o serviço com os repositórios e sincronizadores necessários para administrar nichos. */
    public MarketNicheService(MarketNicheRepository repository,
                              MarketNicheEnrichmentProfileRepository enrichmentProfileRepository,
                              ChatDialogRepository chatDialogRepository,
                              DifferentiatedTechnologyRepository differentiatedTechnologyRepository,
                              NicheDetailedDescriptionRepository detailedDescriptionRepository,
                              TargetingElementSyncService targetingElementSyncService) {
        this.repository = repository;
        this.enrichmentProfileRepository = enrichmentProfileRepository;
        this.chatDialogRepository = chatDialogRepository;
        this.differentiatedTechnologyRepository = differentiatedTechnologyRepository;
        this.detailedDescriptionRepository = detailedDescriptionRepository;
        this.targetingElementSyncService = targetingElementSyncService;
    }

    /**
     * Cria e persiste um nicho de mercado.
     */
    @Transactional
    public MarketNiche create(CreateMarketNicheRequest request) {
        ChatDialog chat = null;
        if (request.getChatDialogId() != null) {
            chat = chatDialogRepository.findById(request.getChatDialogId()).orElseThrow();
        }
        DifferentiatedTechnology differentiatedTechnology =
                resolveDifferentiatedTechnology(request.getDifferentiatedTechnologyId());
        MarketNiche niche = MarketNiche.builder()
                .name(request.getName())
                .description(request.getDescription())
                .interestCategory(request.getInterestCategory())
                .roleCategory(request.getRoleCategory())
                .demandVolume(request.getDemandVolume())
                .promises(request.getPromises())
                .offers(request.getOffers())
                .cost(request.getCost())
                .expense(request.getExpense())
                .totalCost(request.getTotalCost())
                .totalRevenue(request.getTotalRevenue())
                .baseSegmentation(request.getBaseSegmentation())
                .interests(request.getInterests())
                .demographicFilters(request.getDemographicFilters())
                .interestList(normalizeStringList(request.getInterestList()))
                .roleList(normalizeStringList(request.getRoleList()))
                .behaviorList(normalizeStringList(request.getBehaviorList()))
                .extraTips(request.getExtraTips())
                .hypothesesToGenerate(request.getHypothesesToGenerate())
                .interestsToGenerate(request.getInterestsToGenerate())
                .jobTitlesToGenerate(request.getJobTitlesToGenerate())
                .behaviorsToGenerate(request.getBehaviorsToGenerate())
                .detailedDescriptionsToGenerate(request.getDetailedDescriptionsToGenerate())
                .hypothesisModel(normalizeModel(request.getHypothesisModel()))
                .interestModel(normalizeModel(request.getInterestModel()))
                .jobTitleModel(normalizeModel(request.getJobTitleModel()))
                .behaviorModel(normalizeModel(request.getBehaviorModel()))
                .detailedDescriptionModel(normalizeModel(request.getDetailedDescriptionModel()))
                .differentiatedTechnology(differentiatedTechnology)
                .chatDialog(chat)
                .build();
        MarketNiche saved = repository.save(niche);
        targetingElementSyncService.syncManualLists(saved);
        return saved;
    }

    /** Busca um nicho pelo identificador informado. */
    public MarketNiche get(Long id) {
        return repository.findById(id).orElseThrow();
    }

    /** Atualiza os dados administrativos de um nicho existente. */
    @Transactional
    public MarketNiche update(Long id, CreateMarketNicheRequest request) {
        MarketNiche niche = repository.findById(id).orElseThrow();
        niche.setName(request.getName());
        niche.setDescription(request.getDescription());
        niche.setInterestCategory(request.getInterestCategory());
        niche.setRoleCategory(request.getRoleCategory());
        niche.setDemandVolume(request.getDemandVolume());
        niche.setPromises(request.getPromises());
        niche.setOffers(request.getOffers());
        niche.setCost(request.getCost());
        niche.setExpense(request.getExpense());
        niche.setTotalCost(request.getTotalCost());
        niche.setTotalRevenue(request.getTotalRevenue());
        niche.setBaseSegmentation(request.getBaseSegmentation());
        niche.setInterests(request.getInterests());
        niche.setDemographicFilters(request.getDemographicFilters());
        niche.setInterestList(normalizeStringList(request.getInterestList()));
        niche.setRoleList(normalizeStringList(request.getRoleList()));
        niche.setBehaviorList(normalizeStringList(request.getBehaviorList()));
        niche.setExtraTips(request.getExtraTips());
        niche.setHypothesesToGenerate(request.getHypothesesToGenerate());
        niche.setInterestsToGenerate(request.getInterestsToGenerate());
        niche.setJobTitlesToGenerate(request.getJobTitlesToGenerate());
        niche.setBehaviorsToGenerate(request.getBehaviorsToGenerate());
        niche.setDetailedDescriptionsToGenerate(request.getDetailedDescriptionsToGenerate());
        niche.setHypothesisModel(normalizeModel(request.getHypothesisModel()));
        niche.setInterestModel(normalizeModel(request.getInterestModel()));
        niche.setJobTitleModel(normalizeModel(request.getJobTitleModel()));
        niche.setBehaviorModel(normalizeModel(request.getBehaviorModel()));
        niche.setDetailedDescriptionModel(normalizeModel(request.getDetailedDescriptionModel()));
        niche.setDifferentiatedTechnology(
                resolveDifferentiatedTechnology(request.getDifferentiatedTechnologyId()));
        ChatDialog chat = null;
        if (request.getChatDialogId() != null) {
            chat = chatDialogRepository.findById(request.getChatDialogId()).orElseThrow();
        }
        niche.setChatDialog(chat);
        MarketNiche saved = repository.save(niche);
        targetingElementSyncService.syncManualLists(saved);
        return saved;
    }


    /** Normaliza listas textuais removendo valores vazios e espaços externos. */
    private java.util.List<String> normalizeStringList(java.util.List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(item -> item != null && !item.isBlank())
                .map(String::trim)
                .toList();
    }

    /** Normaliza o nome do modelo para armazenar nulo quando não houver valor útil. */
    private String normalizeModel(String model) {
        if (model == null || model.isBlank()) {
            return null;
        }
        return model;
    }

    /** Solicita a geração assíncrona de interesses para um nicho. */
    @Transactional
    public MarketNiche requestInterests(Long id, int quantity, String model) {
        MarketNiche niche = repository.findById(id).orElseThrow();
        niche.setInterestsToGenerate(Math.max(0, quantity));
        if (model != null) {
            niche.setInterestModel(normalizeModel(model));
        }
        return niche;
    }

    /** Solicita a geração assíncrona de cargos para um nicho. */
    @Transactional
    public MarketNiche requestJobTitles(Long id, int quantity, String model) {
        MarketNiche niche = repository.findById(id).orElseThrow();
        niche.setJobTitlesToGenerate(Math.max(0, quantity));
        if (model != null) {
            niche.setJobTitleModel(normalizeModel(model));
        }
        return niche;
    }

    /** Solicita a geração assíncrona de comportamentos para um nicho. */
    @Transactional
    public MarketNiche requestBehaviors(Long id, int quantity, String model) {
        MarketNiche niche = repository.findById(id).orElseThrow();
        niche.setBehaviorsToGenerate(Math.max(0, quantity));
        if (model != null) {
            niche.setBehaviorModel(normalizeModel(model));
        }
        return niche;
    }

    /**
     * Solicita a geração de descrições detalhadas definindo a quantidade pendente.
     */
    @Transactional
    public MarketNiche requestDetailedDescriptions(Long id, int quantity, String model) {
        MarketNiche niche = repository.findById(id).orElseThrow();
        niche.setDetailedDescriptionsToGenerate(Math.max(0, quantity));
        if (model != null) {
            niche.setDetailedDescriptionModel(normalizeModel(model));
        }
        return niche;
    }

    /** Resolve a tecnologia diferenciada opcional ou falha quando o identificador não existe. */
    private DifferentiatedTechnology resolveDifferentiatedTechnology(Long id) {
        if (id == null) {
            return null;
        }
        return differentiatedTechnologyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Differentiated technology not found: " + id));
    }

    /** Resolve a descrição detalhada ativa usada como contexto para hipóteses. */
    private NicheDetailedDescription resolveDetailedDescription(Long nicheId, Long descriptionId) {
        if (descriptionId == null) {
            return null;
        }
        return detailedDescriptionRepository.findByIdAndMarketNicheIdAndActiveTrue(descriptionId, nicheId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Active detailed description not found for niche: " + descriptionId));
    }

    /**
     * Solicita a geração de novas hipóteses definindo a quantidade pendente.
     */
    @Transactional
    public MarketNiche requestHypotheses(Long id,
                                         int quantity,
                                         String model,
                                         Long differentiatedTechnologyId,
                                         Long detailedDescriptionId) {
        MarketNiche niche = repository.findById(id).orElseThrow();
        niche.setHypothesesToGenerate(quantity);
        if (model != null) {
            niche.setHypothesisModel(normalizeModel(model));
        }
        if (differentiatedTechnologyId != null) {
            niche.setDifferentiatedTechnology(resolveDifferentiatedTechnology(differentiatedTechnologyId));
        }
        if (detailedDescriptionId != null) {
            niche.setHypothesisDetailedDescription(resolveDetailedDescription(id, detailedDescriptionId));
        }
        return niche;
    }


    /** Lista uma página de nichos com agregados comerciais para a tela administrativa. */
    public Page<MarketNicheListItemProjection> listItems(int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 100));
        Pageable pageable = PageRequest.of(safePage, safeSize);
        return repository.findListItems(pageable);
    }

    /** Lista todos os nichos cadastrados para a tela administrativa. */
    public Iterable<MarketNiche> list() {
        return repository.findAll();
    }


    /** Lista o perfil enriquecido mais recente por identificador de nicho para listagens paginadas. */
    public Map<Long, MarketNicheEnrichmentProfile> latestEnrichmentProfilesByNicheIds(List<Long> nicheIds) {
        if (nicheIds == null) {
            return Map.of();
        }
        List<Long> safeIds = nicheIds.stream()
                .filter(id -> id != null)
                .toList();
        if (safeIds.isEmpty()) {
            return Map.of();
        }
        return enrichmentProfileRepository.findLatestByMarketNicheIds(safeIds).stream()
                .collect(Collectors.toMap(profile -> profile.getMarketNiche().getId(), Function.identity()));
    }

    /** Lista o perfil enriquecido mais recente por nicho para habilitar navegação contextual no frontend. */
    public Map<Long, MarketNicheEnrichmentProfile> latestEnrichmentProfilesByNiche(List<MarketNiche> niches) {
        List<Long> nicheIds = niches.stream()
                .map(MarketNiche::getId)
                .filter(id -> id != null)
                .toList();
        if (nicheIds.isEmpty()) {
            return Map.of();
        }
        return enrichmentProfileRepository.findLatestByMarketNicheIds(nicheIds).stream()
                .collect(Collectors.toMap(profile -> profile.getMarketNiche().getId(), Function.identity()));
    }

    /** Lista nichos prontos para criação de pixel do Facebook. */
    public List<MarketNiche> listReadyForPixel() {
        List<ExperimentStatus> statuses = List.of(
                ExperimentStatus.PLANNED,
                ExperimentStatus.RUNNING,
                ExperimentStatus.PAUSED
        );
        return repository.findReadyForPixel(statuses, ExperimentPlatform.FACEBOOK);
    }

    /** Vincula dados do pixel do Facebook ao nicho informado. */
    @Transactional
    public MarketNiche attachFacebookPixel(Long nicheId, String pixelId, String pixelCode, Instant createdAt) {
        if (pixelId == null || pixelId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "pixelId is required");
        }
        MarketNiche niche = repository.findById(nicheId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Market niche not found: " + nicheId));
        niche.setFacebookPixelId(pixelId.trim());
        niche.setFacebookPixelCode(pixelCode);
        niche.setFacebookPixelCreatedAt(createdAt != null ? createdAt : Instant.now());
        return niche;
    }
}

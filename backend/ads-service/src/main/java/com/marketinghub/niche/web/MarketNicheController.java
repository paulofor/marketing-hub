package com.marketinghub.niche.web;

import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.MarketNicheEnrichmentProfile;
import com.marketinghub.niche.dto.CreateMarketNicheRequest;
import com.marketinghub.niche.dto.MarketNicheDto;
import com.marketinghub.niche.dto.MarketNicheListItemDto;
import com.marketinghub.niche.dto.MarketNicheListItemProjection;
import com.marketinghub.niche.dto.MarketNicheListPageDto;
import com.marketinghub.niche.mapper.MarketNicheMapper;
import com.marketinghub.niche.service.MarketNicheService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

/**
 * Responsabilidade: expor endpoints administrativos de nichos de mercado.
 */
@RestController
@RequestMapping("/api/niches")
public class MarketNicheController {
    private final MarketNicheService service;
    private final MarketNicheMapper mapper;

    /** Inicializa o controller de nichos com o serviço de domínio e mapper de DTOs. */
    public MarketNicheController(MarketNicheService service, MarketNicheMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    /** Cria um novo nicho de mercado a partir do payload administrativo. */
    @PostMapping
    public MarketNicheDto create(@RequestBody CreateMarketNicheRequest request) {
        return mapper.toDto(service.create(request));
    }

    /** Consulta um nicho de mercado pelo identificador. */
    @GetMapping("/{id}")
    public MarketNicheDto get(@PathVariable Long id) {
        return mapper.toDto(service.get(id));
    }

    /** Atualiza os dados principais de um nicho de mercado. */
    @PutMapping("/{id}")
    public MarketNicheDto update(@PathVariable Long id, @RequestBody CreateMarketNicheRequest request) {
        return mapper.toDto(service.update(id, request));
    }

    /** Agenda a geração de interesses para segmentação do nicho. */
    @PatchMapping("/{id}/interests-to-generate")
    public MarketNicheDto requestInterests(@PathVariable Long id,
                                            @RequestParam("quantity") int quantity,
                                            @RequestParam(value = "model", required = false) String model) {
        return mapper.toDto(service.requestInterests(id, quantity, model));
    }

    /** Agenda a geração de cargos para segmentação do nicho. */
    @PatchMapping("/{id}/job-titles-to-generate")
    public MarketNicheDto requestJobTitles(@PathVariable Long id,
                                            @RequestParam("quantity") int quantity,
                                            @RequestParam(value = "model", required = false) String model) {
        return mapper.toDto(service.requestJobTitles(id, quantity, model));
    }

    /** Agenda a geração de comportamentos para segmentação do nicho. */
    @PatchMapping("/{id}/behaviors-to-generate")
    public MarketNicheDto requestBehaviors(@PathVariable Long id,
                                            @RequestParam("quantity") int quantity,
                                            @RequestParam(value = "model", required = false) String model) {
        return mapper.toDto(service.requestBehaviors(id, quantity, model));
    }

    /** Agenda a geração de hipóteses e opcionalmente define contexto de tecnologia ou descrição detalhada. */
    @PatchMapping("/{id}/hypotheses-to-generate")
    public MarketNicheDto requestHypotheses(@PathVariable Long id,
                                            @RequestParam("quantity") int quantity,
                                            @RequestParam(value = "model", required = false) String model,
                                            @RequestParam(value = "differentiatedTechnologyId", required = false)
                                            Long differentiatedTechnologyId,
                                            @RequestParam(value = "detailedDescriptionId", required = false)
                                            Long detailedDescriptionId) {
        return mapper.toDto(service.requestHypotheses(
                id,
                quantity,
                model,
                differentiatedTechnologyId,
                detailedDescriptionId));
    }

    /** Agenda a geração de descrições detalhadas para aprofundar o nicho. */
    @PatchMapping("/{id}/detailed-descriptions-to-generate")
    public MarketNicheDto requestDetailedDescriptions(@PathVariable Long id,
                                                      @RequestParam("quantity") int quantity,
                                                      @RequestParam(value = "model", required = false) String model) {
        return mapper.toDto(service.requestDetailedDescriptions(id, quantity, model));
    }

    /** Lista nichos paginados para a tela administrativa com custo total e contagens operacionais. */
    @GetMapping("/summary")
    public MarketNicheListPageDto listSummary(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "30") int size) {
        Page<MarketNicheListItemProjection> nichePage = service.listItems(page, size);
        List<Long> nicheIds = nichePage.getContent().stream()
                .map(MarketNicheListItemProjection::getId)
                .toList();
        Map<Long, MarketNicheEnrichmentProfile> profilesByNiche = service.latestEnrichmentProfilesByNicheIds(nicheIds);
        List<MarketNicheListItemDto> items = nichePage.getContent().stream()
                .map(item -> new MarketNicheListItemDto(
                        item.getId(),
                        item.getName(),
                        profilesByNiche.containsKey(item.getId()) ? profilesByNiche.get(item.getId()).getId() : null,
                        item.getCreatedAt(),
                        item.getTotalCost(),
                        item.getPipelineHypothesesCount(),
                        item.getExperimentsCount()))
                .toList();
        return new MarketNicheListPageDto(
                items,
                nichePage.getTotalElements(),
                nichePage.getTotalPages(),
                nichePage.getNumber(),
                nichePage.getSize());
    }

    /** Lista os nichos e marca aqueles que já possuem perfil enriquecido materializado. */
    @GetMapping
    public List<MarketNicheDto> list() {
        List<MarketNiche> niches = StreamSupport.stream(service.list().spliterator(), false).toList();
        Map<Long, MarketNicheEnrichmentProfile> profilesByNiche = service.latestEnrichmentProfilesByNiche(niches);
        return niches.stream()
                .map(niche -> toDtoWithEnrichment(niche, profilesByNiche.get(niche.getId())))
                .toList();
    }

    /** Converte o nicho em DTO incluindo o vínculo opcional para a tela de nicho enriquecido. */
    private MarketNicheDto toDtoWithEnrichment(MarketNiche niche, MarketNicheEnrichmentProfile profile) {
        MarketNicheDto dto = mapper.toDto(niche);
        dto.setEnrichedNicheProfileId(profile == null ? null : profile.getId());
        return dto;
    }
}

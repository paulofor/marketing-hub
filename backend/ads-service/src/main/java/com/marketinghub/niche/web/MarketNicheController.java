package com.marketinghub.niche.web;

import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.MarketNicheEnrichmentProfile;
import com.marketinghub.niche.dto.CreateMarketNicheRequest;
import com.marketinghub.niche.dto.MarketNicheDto;
import com.marketinghub.niche.mapper.MarketNicheMapper;
import com.marketinghub.niche.service.MarketNicheService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

/**
 * REST controller for market niches.
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

package com.marketinghub.planning.web;

import com.marketinghub.planning.CommercialPlanStatus;
import com.marketinghub.planning.dto.CommercialPlanDto;
import com.marketinghub.planning.dto.CommercialPlanMilestoneDto;
import com.marketinghub.planning.dto.CommercialPlanSimulationDto;
import com.marketinghub.planning.dto.CreateCommercialPlanRequest;
import com.marketinghub.planning.dto.CreateCommercialPlanSimulationRequest;
import com.marketinghub.planning.dto.UpdateCommercialPlanMilestoneRequest;
import com.marketinghub.planning.dto.UpdateCommercialPlanRequest;
import com.marketinghub.planning.mapper.CommercialPlanMapper;
import com.marketinghub.planning.service.CommercialPlanService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Responsabilidade: expor endpoints do modulo de planejamento comercial. */
@RestController
@RequestMapping("/api/planning/commercial-plans")
public class CommercialPlanController {
    private final CommercialPlanService service;
    private final CommercialPlanMapper mapper;

    public CommercialPlanController(CommercialPlanService service, CommercialPlanMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    /** Cria um plano comercial de primeira venda. */
    @PostMapping
    public CommercialPlanDto create(@RequestBody CreateCommercialPlanRequest request) {
        var plan = service.create(request);
        return mapper.toDto(plan, service.listMilestones(plan.getId()), service.listSimulations(plan.getId()));
    }

    /** Lista planos comerciais com filtro opcional por status. */
    @GetMapping
    public List<CommercialPlanDto> list(@RequestParam(required = false) CommercialPlanStatus status) {
        return service.list(status).stream()
                .map(plan -> mapper.toDto(plan, service.listMilestones(plan.getId()), service.listSimulations(plan.getId())))
                .toList();
    }

    /** Busca o detalhe de um plano comercial. */
    @GetMapping("/{id}")
    public CommercialPlanDto get(@PathVariable Long id) {
        return mapper.toDto(service.getPlan(id), service.listMilestones(id), service.listSimulations(id));
    }

    /** Atualiza um plano comercial existente. */
    @PutMapping("/{id}")
    public CommercialPlanDto update(@PathVariable Long id, @RequestBody UpdateCommercialPlanRequest request) {
        var plan = service.update(id, request);
        return mapper.toDto(plan, service.listMilestones(id), service.listSimulations(id));
    }

    /** Atualiza um marco comercial do plano. */
    @PatchMapping("/{planId}/milestones/{milestoneId}")
    public CommercialPlanMilestoneDto updateMilestone(
            @PathVariable Long planId,
            @PathVariable Long milestoneId,
            @RequestBody UpdateCommercialPlanMilestoneRequest request) {
        return mapper.toMilestoneDto(service.updateMilestone(planId, milestoneId, request));
    }

    /** Gera uma simulacao manual assistida para o plano. */
    @PostMapping("/{planId}/simulations")
    public CommercialPlanSimulationDto simulate(
            @PathVariable Long planId,
            @RequestBody CreateCommercialPlanSimulationRequest request) {
        return mapper.toSimulationDto(service.simulate(planId, request));
    }
}

package com.marketinghub.pde.controller;

import com.marketinghub.pde.dto.AiGuidanceCreateRequest;
import com.marketinghub.pde.dto.AiGuidancePendingResponse;
import com.marketinghub.pde.dto.AiGuidanceResponse;
import com.marketinghub.pde.dto.AiGuidanceResultRequest;
import com.marketinghub.pde.dto.PublicPresenceDiagnosticRequest;
import com.marketinghub.pde.service.AiGuidanceService;
import com.marketinghub.pde.service.InternalApiAuthorizer;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Expõe contratos fechados de orientação por IA para frontend e worker PDE. */
@RestController
public class AiGuidanceController {

    private final AiGuidanceService aiGuidanceService;
    private final InternalApiAuthorizer internalApiAuthorizer;

    /** Recebe os serviços que controlam solicitações de IA e autorizam o executor. */
    public AiGuidanceController(
            AiGuidanceService aiGuidanceService,
            InternalApiAuthorizer internalApiAuthorizer) {
        this.aiGuidanceService = aiGuidanceService;
        this.internalApiAuthorizer = internalApiAuthorizer;
    }

    /** Cria uma orientação guiada por IA para a missão informada. */
    @PostMapping("/api/pde/access/{token}/missions/{missionId}/ai-guidance")
    @ResponseStatus(HttpStatus.CREATED)
    public AiGuidanceResponse createGuidance(
            @PathVariable("token") String token,
            @PathVariable("missionId") String missionId,
            @Valid @RequestBody AiGuidanceCreateRequest request) {
        return aiGuidanceService.createGuidanceRequest(token, missionId, request);
    }

    /** Cria um diagnóstico público de presença com plano de 7 dias gerado por IA. */
    @PostMapping("/api/pde/public/presence-diagnostic")
    @ResponseStatus(HttpStatus.CREATED)
    public AiGuidanceResponse createPublicPresenceDiagnostic(
            @Valid @RequestBody PublicPresenceDiagnosticRequest request) {
        return aiGuidanceService.createPublicPresenceDiagnostic(request);
    }

    /** Retorna o estado do diagnóstico público de presença. */
    @GetMapping("/api/pde/public/presence-diagnostic/{requestId}")
    public AiGuidanceResponse getPublicPresenceDiagnostic(
            @PathVariable("requestId") String requestId) {
        return aiGuidanceService.getPublicPresenceDiagnostic(requestId);
    }

    /** Retorna o estado atual de uma orientação guiada por IA. */
    @GetMapping("/api/pde/access/{token}/ai-guidance/{requestId}")
    public AiGuidanceResponse getGuidance(
            @PathVariable("token") String token,
            @PathVariable("requestId") String requestId) {
        return aiGuidanceService.getGuidance(token, requestId);
    }

    /** Entrega uma lista unitária de pendência ao worker executor do PDE. */
    @GetMapping("/api/internal/pde/ai-guidance/stage-executions/pending")
    public List<AiGuidancePendingResponse> getPendingGuidance(
            @RequestHeader(value = "X-PDE-Internal-Token", required = false) String internalToken) {
        internalApiAuthorizer.requireAuthorized(internalToken);
        return aiGuidanceService.getPendingGuidance().map(List::of).orElseGet(List::of);
    }

    /** Recebe o resultado do worker executor para uma orientação por IA. */
    @PostMapping("/api/internal/pde/ai-guidance/stage-executions/{requestId}/result")
    public AiGuidanceResponse receiveGuidanceResult(
            @PathVariable("requestId") String requestId,
            @RequestHeader(value = "X-PDE-Internal-Token", required = false) String internalToken,
            @Valid @RequestBody AiGuidanceResultRequest request) {
        internalApiAuthorizer.requireAuthorized(internalToken);
        return aiGuidanceService.receiveGuidanceResult(requestId, request);
    }
}

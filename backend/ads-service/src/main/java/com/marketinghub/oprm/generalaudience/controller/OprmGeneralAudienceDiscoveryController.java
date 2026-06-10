package com.marketinghub.oprm.generalaudience.controller;

import com.marketinghub.oprm.generalaudience.service.OprmGeneralAudienceDiscoveryService;
import com.marketinghub.oprm.generalaudience.service.createHypothesis.CreateGeneralAudienceHypothesisRequest;
import com.marketinghub.oprm.generalaudience.service.createHypothesis.GeneralAudienceHypothesisResponse;
import com.marketinghub.oprm.generalaudience.service.createPainAngle.CreateGeneralAudiencePainAngleRequest;
import com.marketinghub.oprm.generalaudience.service.createSourceEvidence.CreateGeneralAudienceSourceEvidenceRequest;
import com.marketinghub.oprm.generalaudience.service.listPainAngles.GeneralAudiencePainAngleResponse;
import com.marketinghub.oprm.generalaudience.service.listSourceEvidences.GeneralAudienceSourceEvidenceResponse;
import com.marketinghub.oprm.generalaudience.service.qualityGate.GeneralAudienceQualityGateResponse;
import com.marketinghub.oprm.generalaudience.service.updatePainAngle.UpdateGeneralAudiencePainAngleRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Responsável pelos endpoints OPRM do pipeline de descoberta de públicos gerais. */
@RestController
@RequestMapping("/api/oprm/general-audiences")
public class OprmGeneralAudienceDiscoveryController {

    private final OprmGeneralAudienceDiscoveryService service;

    /** Inicializa o controller com o serviço de descoberta de públicos gerais. */
    public OprmGeneralAudienceDiscoveryController(OprmGeneralAudienceDiscoveryService service) {
        this.service = service;
    }

    /** Lista dores e ângulos testáveis de um subnicho de público geral. */
    @GetMapping("/subniches/{subnicheId}/pain-angles")
    public ResponseEntity<List<GeneralAudiencePainAngleResponse>> listPainAngles(@PathVariable Long subnicheId) {
        return ResponseEntity.ok(service.listPainAngles(subnicheId));
    }

    /** Cadastra dor e ângulo seguro sem gerar oferta final ou campanha. */
    @PostMapping("/subniches/{subnicheId}/pain-angles")
    public ResponseEntity<GeneralAudiencePainAngleResponse> createPainAngle(
            @PathVariable Long subnicheId,
            @Valid @RequestBody CreateGeneralAudiencePainAngleRequest request) {
        GeneralAudiencePainAngleResponse response = service.createPainAngle(subnicheId, request);
        return ResponseEntity
                .created(URI.create("/api/oprm/general-audiences/pain-angles/" + response.id()))
                .body(response);
    }

    /** Atualiza um ângulo testável mantendo validações de qualidade e compliance. */
    @PatchMapping("/pain-angles/{angleId}")
    public ResponseEntity<GeneralAudiencePainAngleResponse> updatePainAngle(
            @PathVariable Long angleId,
            @Valid @RequestBody UpdateGeneralAudiencePainAngleRequest request) {
        return ResponseEntity.ok(service.updatePainAngle(angleId, request));
    }

    /** Aprova um ângulo somente quando não há promessa arriscada ou dor genérica. */
    @PostMapping("/pain-angles/{angleId}/approve")
    public ResponseEntity<GeneralAudiencePainAngleResponse> approvePainAngle(@PathVariable Long angleId) {
        return ResponseEntity.ok(service.approvePainAngle(angleId));
    }

    /** Cria uma hipótese específica a partir de um ângulo aprovado de público geral. */
    @PostMapping("/pain-angles/{angleId}/create-hypothesis")
    public ResponseEntity<GeneralAudienceHypothesisResponse> createHypothesis(
            @PathVariable Long angleId,
            @Valid @RequestBody(required = false) CreateGeneralAudienceHypothesisRequest request) {
        GeneralAudienceHypothesisResponse response = service.createHypothesis(angleId, request);
        return ResponseEntity
                .created(URI.create("/api/hypotheses/" + response.hypothesisId()))
                .body(response);
    }

    /** Lista evidências agregadas associadas à semente de público geral. */
    @GetMapping("/seeds/{seedId}/source-evidences")
    public ResponseEntity<List<GeneralAudienceSourceEvidenceResponse>> listSeedEvidences(@PathVariable Long seedId) {
        return ResponseEntity.ok(service.listSeedEvidences(seedId));
    }

    /** Registra evidência agregada e rastreável sem gravar dados pessoais ou comentários integrais. */
    @PostMapping("/seeds/{seedId}/source-evidences")
    public ResponseEntity<GeneralAudienceSourceEvidenceResponse> createSourceEvidence(
            @PathVariable Long seedId,
            @Valid @RequestBody CreateGeneralAudienceSourceEvidenceRequest request) {
        GeneralAudienceSourceEvidenceResponse response = service.createSourceEvidence(seedId, request);
        return ResponseEntity
                .created(URI.create("/api/oprm/general-audiences/source-evidences/" + response.id()))
                .body(response);
    }

    /** Avalia se o subnicho possui qualidade mínima para avançar sem virar saída genérica. */
    @GetMapping("/subniches/{subnicheId}/quality-gate")
    public ResponseEntity<GeneralAudienceQualityGateResponse> evaluateQualityGate(@PathVariable Long subnicheId) {
        return ResponseEntity.ok(service.evaluateQualityGate(subnicheId));
    }
}

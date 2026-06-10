package com.marketinghub.oprm.generalaudience.controller;

import com.marketinghub.oprm.generalaudience.service.OprmGeneralAudienceService;
import com.marketinghub.oprm.generalaudience.service.createSeed.CreateGeneralAudienceSeedRequest;
import com.marketinghub.oprm.generalaudience.service.getSeed.GeneralAudienceSeedResponse;
import com.marketinghub.oprm.generalaudience.service.listSeeds.GeneralAudienceSeedSummaryResponse;
import com.marketinghub.oprm.generalaudience.service.updateSeed.UpdateGeneralAudienceSeedRequest;
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

/** Responsável pelos endpoints OPRM de cadastro e revisão manual de sementes de público geral. */
@RestController
@RequestMapping("/api/oprm/general-audiences")
public class OprmGeneralAudienceController {

    private final OprmGeneralAudienceService service;

    /** Inicializa o controller com o serviço único do módulo de públicos gerais. */
    public OprmGeneralAudienceController(OprmGeneralAudienceService service) {
        this.service = service;
    }

    /** Lista sementes de público geral para seleção operacional. */
    @GetMapping("/seeds")
    public ResponseEntity<List<GeneralAudienceSeedSummaryResponse>> listSeeds() {
        return ResponseEntity.ok(service.listSeeds());
    }

    /** Cadastra uma semente de público geral sem iniciar campanha ou descoberta automática. */
    @PostMapping("/seeds")
    public ResponseEntity<GeneralAudienceSeedResponse> createSeed(
            @Valid @RequestBody CreateGeneralAudienceSeedRequest request) {
        GeneralAudienceSeedResponse response = service.createSeed(request);
        return ResponseEntity
                .created(URI.create("/api/oprm/general-audiences/seeds/" + response.id()))
                .body(response);
    }

    /** Detalha uma semente de público geral para revisão manual. */
    @GetMapping("/seeds/{seedId}")
    public ResponseEntity<GeneralAudienceSeedResponse> getSeed(@PathVariable Long seedId) {
        return ResponseEntity.ok(service.getSeed(seedId));
    }

    /** Atualiza os campos manuais da semente de público geral. */
    @PatchMapping("/seeds/{seedId}")
    public ResponseEntity<GeneralAudienceSeedResponse> updateSeed(
            @PathVariable Long seedId,
            @Valid @RequestBody UpdateGeneralAudienceSeedRequest request) {
        return ResponseEntity.ok(service.updateSeed(seedId, request));
    }

    /** Arquiva uma semente de público geral sem remover seu histórico. */
    @PostMapping("/seeds/{seedId}/archive")
    public ResponseEntity<GeneralAudienceSeedResponse> archiveSeed(@PathVariable Long seedId) {
        return ResponseEntity.ok(service.archiveSeed(seedId));
    }
}

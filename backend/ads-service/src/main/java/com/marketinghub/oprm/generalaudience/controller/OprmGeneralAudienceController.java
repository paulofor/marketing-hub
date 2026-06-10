package com.marketinghub.oprm.generalaudience.controller;

import com.marketinghub.oprm.generalaudience.service.OprmGeneralAudienceService;
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

/** Responsável pelos endpoints OPRM de cadastro e revisão manual de sementes e subnichos gerais. */
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

    /** Lista subnichos descobertos a partir de uma semente de público geral. */
    @GetMapping("/seeds/{seedId}/subniches")
    public ResponseEntity<List<GeneralAudienceSubnicheSummaryResponse>> listSubniches(@PathVariable Long seedId) {
        return ResponseEntity.ok(service.listSubniches(seedId));
    }

    /** Cadastra manualmente um subnicho derivado da semente sem tratá-lo como CNAE. */
    @PostMapping("/seeds/{seedId}/subniches")
    public ResponseEntity<GeneralAudienceSubnicheResponse> createSubniche(
            @PathVariable Long seedId,
            @Valid @RequestBody CreateGeneralAudienceSubnicheRequest request) {
        GeneralAudienceSubnicheResponse response = service.createSubniche(seedId, request);
        return ResponseEntity
                .created(URI.create("/api/oprm/general-audiences/subniches/" + response.id()))
                .body(response);
    }

    /** Detalha um subnicho de público geral para revisão de persona, dor, canais e triagem. */
    @GetMapping("/subniches/{subnicheId}")
    public ResponseEntity<GeneralAudienceSubnicheResponse> getSubniche(@PathVariable Long subnicheId) {
        return ResponseEntity.ok(service.getSubniche(subnicheId));
    }

    /** Atualiza os campos manuais de um subnicho de público geral. */
    @PatchMapping("/subniches/{subnicheId}")
    public ResponseEntity<GeneralAudienceSubnicheResponse> updateSubniche(
            @PathVariable Long subnicheId,
            @Valid @RequestBody UpdateGeneralAudienceSubnicheRequest request) {
        return ResponseEntity.ok(service.updateSubniche(subnicheId, request));
    }

    /** Aprova um subnicho específico para avançar para experimentos futuros. */
    @PostMapping("/subniches/{subnicheId}/approve")
    public ResponseEntity<GeneralAudienceSubnicheResponse> approveSubniche(@PathVariable Long subnicheId) {
        return ResponseEntity.ok(service.approveSubniche(subnicheId));
    }

    /** Rejeita um subnicho amplo ou inseguro sem apagar histórico de descoberta. */
    @PostMapping("/subniches/{subnicheId}/reject")
    public ResponseEntity<GeneralAudienceSubnicheResponse> rejectSubniche(@PathVariable Long subnicheId) {
        return ResponseEntity.ok(service.rejectSubniche(subnicheId));
    }

    /** Converte um subnicho aprovado em MarketNiche sem usar tabelas CNAE. */
    @PostMapping("/subniches/{subnicheId}/convert-to-market-niche")
    public ResponseEntity<GeneralAudienceMarketNicheConversionResponse> convertSubnicheToMarketNiche(
            @PathVariable Long subnicheId,
            @Valid @RequestBody(required = false) ConvertGeneralAudienceSubnicheToMarketNicheRequest request) {
        return ResponseEntity.ok(service.convertSubnicheToMarketNiche(subnicheId, request));
    }
}

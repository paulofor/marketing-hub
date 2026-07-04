package com.marketinghub.metaaudience.controller;

import com.marketinghub.metaaudience.service.BackendMetaAudienceService;
import com.marketinghub.metaaudience.service.internalComplete.MetaAudienceSyncCompleteRequest;
import com.marketinghub.metaaudience.service.internalPending.MetaAudiencePendingResponse;
import com.marketinghub.metaaudience.service.linkExperiment.ExperimentMetaAudienceResponse;
import com.marketinghub.metaaudience.service.linkExperiment.LinkMetaAudienceExperimentRequest;
import com.marketinghub.metaaudience.service.requestAudience.MetaAudienceRequest;
import com.marketinghub.metaaudience.service.requestAudience.MetaAudienceResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Controller único responsável por criar audiências Meta Ads e expor a fila interna ao worker. */
@RestController
@RequiredArgsConstructor
public class MetaAudienceController {
    private final BackendMetaAudienceService service;

    /** Solicita a criação operacional de uma audiência Meta vinculada a nicho e CNAE. */
    @PostMapping("/api/meta-audiences")
    public MetaAudienceResponse requestAudience(@RequestBody MetaAudienceRequest request) {
        return service.requestAudience(request);
    }

    /** Lista audiências prontas para criação na Meta pelo Facebook Ads Worker. */
    @GetMapping("/api/internal/meta-audiences/pending")
    public List<MetaAudiencePendingResponse> pending(@RequestParam(defaultValue = "10") int limit) {
        return service.listPending(limit);
    }

    /** Lista os planos de audiência CNAE de um nicho. */
    @GetMapping("/api/meta-audiences/niches/{nicheId}")
    public List<MetaAudienceResponse> listByNiche(@PathVariable Long nicheId) {
        return service.listByNiche(nicheId);
    }

    /** Vincula uma audiência CNAE ao experimento responsável pela validação. */
    @PostMapping("/api/meta-audiences/experiment-links")
    public ExperimentMetaAudienceResponse linkExperiment(@RequestBody LinkMetaAudienceExperimentRequest request) {
        return service.linkExperiment(request);
    }

    /** Lista audiências CNAE usadas por um experimento. */
    @GetMapping("/api/meta-audiences/experiments/{experimentId}")
    public List<ExperimentMetaAudienceResponse> listByExperiment(@PathVariable Long experimentId) {
        return service.listByExperiment(experimentId);
    }

    /** Lista vínculos de audiências CNAE dentro de um nicho. */
    @GetMapping("/api/meta-audiences/niches/{nicheId}/experiment-links")
    public List<ExperimentMetaAudienceResponse> listExperimentLinksByNiche(@PathVariable Long nicheId) {
        return service.listExperimentLinksByNiche(nicheId);
    }

    /** Recebe o resultado da criação/sincronização da audiência feita pelo worker. */
    @PatchMapping("/api/internal/meta-audiences/{id}/sync")
    public MetaAudienceResponse complete(@PathVariable Long id, @RequestBody MetaAudienceSyncCompleteRequest request) {
        return service.completeSync(id, request);
    }
}

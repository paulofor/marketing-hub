package com.marketinghub.metaaudience.controller;

import com.marketinghub.metaaudience.service.BackendMetaAudienceService;
import com.marketinghub.metaaudience.service.internalComplete.MetaAudienceSyncCompleteRequest;
import com.marketinghub.metaaudience.service.internalPending.MetaAudiencePendingResponse;
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

    /** Recebe o resultado da criação/sincronização da audiência feita pelo worker. */
    @PatchMapping("/api/internal/meta-audiences/{id}/sync")
    public MetaAudienceResponse complete(@PathVariable Long id, @RequestBody MetaAudienceSyncCompleteRequest request) {
        return service.completeSync(id, request);
    }
}

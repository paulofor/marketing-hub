package com.marketinghub.mois.web;

import com.marketinghub.mois.dto.MoisCollectionPersistenceDtos;
import com.marketinghub.mois.service.MoisCollectionPersistenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/mois/persistence")
@RequiredArgsConstructor
public class MoisCollectionPersistenceController {

    private final MoisCollectionPersistenceService service;

    @PutMapping("/collection-jobs/{jobId}")
    @ResponseStatus(HttpStatus.OK)
    public MoisCollectionPersistenceDtos.CollectionJobStateResponse upsertCollectionJobState(
            @PathVariable String jobId,
            @RequestBody MoisCollectionPersistenceDtos.CollectionJobStateResponse request
    ) {
        return service.upsertJobState(jobId, request);
    }

    @GetMapping("/collection-jobs/{jobId}")
    public MoisCollectionPersistenceDtos.CollectionJobStateResponse getCollectionJobState(@PathVariable String jobId) {
        return service.getJobState(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "collection job state not found"));
    }

    @GetMapping("/collection-jobs")
    public MoisCollectionPersistenceDtos.CollectionJobStateListResponse listCollectionJobStates(
            @RequestParam(required = false) String workspaceId,
            @RequestParam(required = false) String status
    ) {
        return service.listJobStates(workspaceId, status);
    }

    @GetMapping("/collection-highlights/by-source")
    public MoisCollectionPersistenceDtos.SourceHighlightListResponse listSourceHighlights(
            @RequestParam(required = false) String workspaceId,
            @RequestParam(required = false) String status
    ) {
        return service.summarizeBySource(workspaceId, status);
    }
}

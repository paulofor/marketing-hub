package com.marketinghub.mois.service;

import com.marketinghub.mois.dto.MoisCollectionPersistenceDtos;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class MoisCollectionPersistenceService {

    private final Map<String, MoisCollectionPersistenceDtos.CollectionJobStateResponse> statesByJobId = new ConcurrentHashMap<>();

    public MoisCollectionPersistenceDtos.CollectionJobStateResponse upsertJobState(
            String jobId,
            MoisCollectionPersistenceDtos.CollectionJobStateResponse state
    ) {
        statesByJobId.put(jobId, state);
        return state;
    }

    public Optional<MoisCollectionPersistenceDtos.CollectionJobStateResponse> getJobState(String jobId) {
        return Optional.ofNullable(statesByJobId.get(jobId));
    }

    public MoisCollectionPersistenceDtos.CollectionJobStateListResponse listJobStates(String workspaceId, String status) {
        List<MoisCollectionPersistenceDtos.CollectionJobStateResponse> items = statesByJobId.values().stream()
                .filter(item -> item.job() != null)
                .filter(item -> workspaceId == null || workspaceId.isBlank() || workspaceId.equals(item.job().workspaceId()))
                .filter(item -> status == null || status.isBlank() || status.equalsIgnoreCase(item.job().status()))
                .sorted(Comparator.comparing((MoisCollectionPersistenceDtos.CollectionJobStateResponse item) -> item.job().createdAt())
                        .reversed())
                .toList();
        return new MoisCollectionPersistenceDtos.CollectionJobStateListResponse(new ArrayList<>(items));
    }
}

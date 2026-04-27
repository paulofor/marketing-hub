package com.marketinghub.mds.repository;

import com.marketinghub.mds.MdsArtifactLineageEdge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MdsArtifactLineageEdgeRepository extends JpaRepository<MdsArtifactLineageEdge, Long> {
    List<MdsArtifactLineageEdge> findByParentArtifact_Request_IdOrChildArtifact_Request_IdOrderByIdAsc(Long parentRequestId,
                                                                                                        Long childRequestId);
}

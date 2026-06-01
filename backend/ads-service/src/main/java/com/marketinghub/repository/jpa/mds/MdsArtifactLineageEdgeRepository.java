package com.marketinghub.repository.jpa.mds;

import com.marketinghub.mds.MdsArtifactLineageEdge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositório JPA responsável pela persistência de MdsArtifactLineageEdge.
 */
public interface MdsArtifactLineageEdgeRepository extends JpaRepository<MdsArtifactLineageEdge, Long> {
    List<MdsArtifactLineageEdge> findByParentArtifact_Request_IdOrChildArtifact_Request_IdOrderByIdAsc(Long parentRequestId,
                                                                                                        Long childRequestId);
}

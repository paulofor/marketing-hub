package com.marketinghub.repository.jpa.mds;

import com.marketinghub.mds.MdsArtifactLineageEdge;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório JPA responsável pela persistência de MdsArtifactLineageEdge. */
public interface MdsArtifactLineageEdgeRepository
    extends JpaRepository<MdsArtifactLineageEdge, Long> {
  List<MdsArtifactLineageEdge>
      findByParentArtifact_Request_IdOrChildArtifact_Request_IdOrderByIdAsc(
          Long parentRequestId, Long childRequestId);
}

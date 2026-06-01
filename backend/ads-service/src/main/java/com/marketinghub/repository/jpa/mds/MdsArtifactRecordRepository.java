package com.marketinghub.repository.jpa.mds;

import com.marketinghub.mds.MdsArtifactRecord;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório JPA responsável pela persistência de MdsArtifactRecord.
 */
public interface MdsArtifactRecordRepository extends JpaRepository<MdsArtifactRecord, Long> {
    java.util.Optional<MdsArtifactRecord> findFirstByRequestIdAndArtifactTypeOrderByCreatedAtDescIdDesc(
            Long requestId,
            String artifactType
    );

    java.util.List<MdsArtifactRecord> findByRequestIdOrderByCreatedAtAscIdAsc(Long requestId);
}

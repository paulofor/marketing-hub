package com.marketinghub.mds.repository;

import com.marketinghub.mds.MdsArtifactRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MdsArtifactRecordRepository extends JpaRepository<MdsArtifactRecord, Long> {
    java.util.Optional<MdsArtifactRecord> findFirstByRequestIdAndArtifactTypeOrderByCreatedAtDescIdDesc(
            Long requestId,
            String artifactType
    );
}

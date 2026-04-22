package com.marketinghub.mois.repository;

import com.marketinghub.mois.MoisSourceSnapshot;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MoisSourceSnapshotRepository extends JpaRepository<MoisSourceSnapshot, Long> {
    List<MoisSourceSnapshot> findByRequest_RequestIdOrderByCreatedAtAsc(String requestId);

    Optional<MoisSourceSnapshot> findByArtifactId(String artifactId);

    long countByRequest_RequestId(String requestId);
}

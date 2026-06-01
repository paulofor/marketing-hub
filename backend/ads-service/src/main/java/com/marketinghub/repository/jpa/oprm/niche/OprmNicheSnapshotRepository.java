package com.marketinghub.repository.jpa.oprm.niche;

import com.marketinghub.oprm.niche.OprmNicheSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório JPA responsável pela persistência de OprmNicheSnapshot.
 */
public interface OprmNicheSnapshotRepository extends JpaRepository<OprmNicheSnapshot, Long> {
}

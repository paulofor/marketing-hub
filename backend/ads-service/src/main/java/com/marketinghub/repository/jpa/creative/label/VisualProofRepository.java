package com.marketinghub.repository.jpa.creative.label;

import com.marketinghub.creative.label.VisualProof;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório JPA responsável pela persistência de VisualProof.
 */
public interface VisualProofRepository extends JpaRepository<VisualProof, Long> {
}

package com.marketinghub.repository.jpa.oprm.cnae;

import com.marketinghub.oprm.cnae.OprmCnaeEnrichmentArtifact;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório responsável por persistir artefatos de enriquecimento CNAE recebidos do OPRM.
 */
public interface OprmCnaeEnrichmentArtifactRepository extends JpaRepository<OprmCnaeEnrichmentArtifact, Long> {}

package com.marketinghub.repository.jpa.kit;

import com.marketinghub.pde.kit.privateprototype.v1.KitPrivateArtifactChunk;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: persistir partes privadas ordenadas e isoladas pelo artefato de origem. */
public interface KitPrivateArtifactChunkRepository
    extends JpaRepository<KitPrivateArtifactChunk, Long> {
  /** Recupera somente as partes do artefato solicitado, na ordem original. */
  List<KitPrivateArtifactChunk> findByArtifactIdOrderByPartNumberAsc(String artifactId);
}

package com.marketinghub.repository.jpa.pde;

import com.marketinghub.pde.PdeProductionSlot;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório JPA responsável pelos slots produtivos versionados do PDE. */
public interface PdeProductionSlotRepository extends JpaRepository<PdeProductionSlot, Long> {

  /** Lista os slots de um produto PDE por código operacional. */
  List<PdeProductionSlot> findByProductSlugOrderBySlotCodeAsc(String productSlug);

  /** Busca um slot de produto pelo código operacional. */
  Optional<PdeProductionSlot> findByProductSlugAndSlotCode(String productSlug, String slotCode);

  /** Busca o contrato de slot mais recente pela versão comercial publicada. */
  Optional<PdeProductionSlot> findFirstByProductSlugAndExperienceVersionOrderByPublishedAtDesc(
      String productSlug, String experienceVersion);

  /** Busca um slot produtivo pelo domínio público normalizado. */
  Optional<PdeProductionSlot> findFirstByDomain(String domain);

  /** Busca o slot mais recente associado diretamente a um experimento de origem. */
  Optional<PdeProductionSlot> findFirstBySourceExperimentIdOrderByUpdatedAtDesc(
      Long sourceExperimentId);
}
